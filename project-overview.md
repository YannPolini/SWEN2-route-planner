# TourPlanner — Projektüberblick

> SWEN2
> Stack: Spring Boot 3.3 / Java 22 · Angular 21 (SSR) · PostgreSQL · OpenRouteService API

---

## Architektur auf einen Blick

```
Browser (Angular 21 SSR)
    ├── auth (Login, Register, Guard)
    ├── tours (Hauptseite: Liste, Detail, Map, Form)
    │     ├── TourService (Model/Signal-State)
    │     ├── AddressInputComponent (ORS Autocomplete → liefert Koordinaten)
    │     └── TourMapComponent (Leaflet Karte)
    └── tourlogs (Logs zur ausgewählten Tour)

Spring Boot 8080
    ├── /api/tours        ← TourController → TourService → OrsService → ORS API
    ├── /api/logs         ← TourLogController → TourLogService
    ├── /api/import-export← ImportExportController → ImportExportService (CSV)
    ├── /api/search       ← SearchController (Stub, noch nicht implementiert)
    └── /api/auth         ← AuthController → AuthService

PostgreSQL (tourlogdb, Docker: tourlog-postgres)
    ├── tour
    └── tour_log
```

---

## Die ORS-Logik im Detail (Kernstück)

Das wichtigste Konzept der App: **Adressen werden nur EINMAL geocodiert — beim
Tippen im Autocomplete.** Die dort ermittelten Koordinaten werden bis zur
Routenberechnung durchgereicht. Es gibt keine zweite Geocodierung.

### Warum das so wichtig ist

ORS hat zwei getrennte Endpunkte:
- **Pelias Geocoding** (`/pelias/v1/autocomplete`, `/pelias/v1/search`): Text → Koordinaten
- **Directions** (`/openrouteservice/v2/directions`): Koordinaten → Route

Würde man die Adresse erst im Frontend (Autocomplete) und dann nochmal im Backend
(Search) geocodieren, könnten **zwei verschiedene Punkte** herauskommen — die
beiden Endpunkte ranken unterschiedlich. Der User sieht im Dropdown Punkt A,
geroutet wird aber Punkt B. Genau dieser Bug ("Adresse einige Straßen daneben")
ist durch das Durchreichen der Koordinaten gelöst.

### Primärweg (User wählt einen Vorschlag aus)

```
1. User tippt "Handelskai" in <app-address-input>
        │  (300ms debounce, ab 3 Zeichen)
        ▼
2. GET https://api.heigit.org/pelias/v1/autocomplete
        ?api_key=…&text=Handelskai&size=5
        &focus.point.lat=48.2082&focus.point.lon=16.3738
        │
        │  ORS-Antwort (gekürzt):
        │  {
        │    "features": [{
        │      "properties": { "label": "Handelskai, Wien, Österreich" },
        │      "geometry":   { "coordinates": [16.40495, 48.225874] }   // [lng, lat]
        │    }, …]
        │  }
        ▼
3. Dropdown zeigt die Labels. User klickt "Handelskai, Wien, Österreich"
        │
        ▼
4. AddressInput emittiert ZWEI Outputs:
        valueChange       → "Handelskai, Wien, Österreich"   (Text fürs Formular)
        coordinatesChange → { lat: 48.225874, lng: 16.40495 } (exakter Punkt)
        │
        ▼
5. ToursComponent speichert beides:
        tourForm.controls.from → "Handelskai, Wien, Österreich"
        fromCoords (Signal)    → { lat: 48.225874, lng: 16.40495 }
```

Beim Absenden des Formulars wird folgendes JSON ans Backend geschickt:

```json
POST /api/tours
{
  "id": "1a7814c2-…",
  "name": "Donaurunde",
  "from": "Handelskai, Wien, Österreich",
  "to":   "Donauinsel, Wien, Österreich",
  "fromLat": 48.225874, "fromLng": 16.40495,
  "toLat":   48.24210,  "toLng":   16.41835,
  "transportType": "bike",
  "distance": 0, "estimatedTime": 0, "routeGeometry": null
}
```

`distance`, `estimatedTime`, `routeGeometry` sind Platzhalter — die berechnet das
Backend gleich über ORS.

```
6. TourService.createTour() → enrichWithOrsData(tour)
        │  hasCoordinates(tour) == true  → Primärweg, KEIN Geocoding
        ▼
7. OrsService.getRoute([16.40495, 48.225874], [16.41835, 48.24210], BIKE)
        │
        ▼
8. POST https://api.heigit.org/openrouteservice/v2/directions/cycling-regular/geojson
        Header: Authorization: <api_key>
        Body:   { "coordinates": [[16.40495,48.225874],[16.41835,48.24210]] }
        │
        │  ORS-Antwort (gekürzt):
        │  {
        │    "features": [{
        │      "properties": { "summary": { "distance": 2910.5, "duration": 654.0 } },
        │      "geometry":   { "coordinates": [[16.40495,48.225874], …] }  // [lng, lat]
        │    }]
        │  }
        ▼
9. parseRoute():
        distanceKm      = 2910.5 / 1000 = 2.91 km
        durationSeconds = 654.0
        coordinates     = [[48.225874,16.40495], …]   // für Leaflet auf [lat,lng] gedreht
        ▼
10. Tour wird befüllt und gespeichert:
        distance=2.91, estimatedTime=654, routeGeometry="[[48.22,16.40],…]"
        ▼
11. Frontend: loadTours() → Signal aktualisiert → TourMapComponent zeichnet Polyline
```

### Fallback-Weg (User tippt frei, ohne Vorschlag zu wählen)

Tippt der User eine Adresse von Hand und wählt **keinen** Dropdown-Eintrag, sind
keine Koordinaten bekannt. `onInput()` setzt `coordinatesChange → null`, das
Formular sendet `fromLat/fromLng/toLat/toLng = null`.

```
Backend: hasCoordinates(tour) == false  → Fallback
    OrsService.getRoute("Stephansplatz 1", "Praterstern", VEHICLE)
        → geocode("Stephansplatz 1")  → GET /pelias/v1/search?text=…&size=1
                                          &focus.point.lat=48.2082&focus.point.lon=16.3738
        → geocode("Praterstern")      → GET /pelias/v1/search?…
        → dann dieselbe Directions-Anfrage wie oben
```

`focus.point` (Wien) ist dabei nur ein **Bias** für die Sortierung, keine
Einschränkung — internationale Adressen funktionieren weiterhin. Konfigurierbar
über `ors.geocode.focus-lat` / `ors.geocode.focus-lon`.

### Transport-Profil-Mapping (OrsService.toOrsProfile)

| TransportType | ORS-Profil |
|---|---|
| BIKE | cycling-regular |
| HIKE | foot-hiking |
| RUNNING | foot-walking |
| VEHICLE | driving-car |

### Koordinaten-Reihenfolge — Stolperfalle

| Kontext | Reihenfolge |
|---|---|
| ORS (Pelias + Directions) | `[lng, lat]` |
| Leaflet (Polyline, Marker) | `[lat, lng]` |

Das Backend dreht in `parseRoute()` einmalig auf `[lat, lng]`, bevor die Geometrie
als JSON gespeichert wird. Das Frontend kann sie direkt an Leaflet geben.

---

## Design Patterns

Mindestens ein Pattern ist gefordert. Implementiert (und im Protokoll belegbar):

### Facade — `OrsService` (Haupt-Pattern)
`OrsService` kapselt die komplette OpenRouteService-REST-API hinter einer
einfachen Schnittstelle. Der Business-Layer (`TourService`) ruft nur
`getRoute(...)` bzw. `autocomplete(...)` auf und weiß nichts von HTTP, URLs,
JSON-Parsing, Koordinaten-Reihenfolge oder API-Key. Alle technischen Details
und Fehler (`OrsServiceException`) bleiben hinter der Facade.

```
TourService ─► OrsService (Facade) ─► HTTP/JSON ─► ORS-API
   weiß nur          versteckt:
   "route mir A→B"   URLs, Key, Parsing, Fehler
```

### Observer — Angular Signals (Frontend)
Der Zustand liegt in `signal()`s; `computed()` und `effect()` sind die
Observer, die sich automatisch neu berechnen, wenn sich ein Signal ändert
(z.B. `filteredTours` reagiert auf `searchTerm` + `tours`). Die View wird
dadurch reaktiv aktualisiert, ohne manuelles Eventhandling.

### Repository — Spring Data JPA
`TourRepository`/`TourLogRepository` (`extends JpaRepository`) abstrahieren den
Datenzugriff. Der Service kennt keine SQL-Details, nur `findAll()`, `save()` usw.

---

## Backend

### Technologie
| | |
|---|---|
| Framework | Spring Boot 3.3 |
| Java | 22 |
| ORM | Hibernate / JPA, `ddl-auto=update` |
| DB | PostgreSQL, Port 5432, DB: `tourlogdb` |
| Start DB | `docker compose up -d` (Container `tourlog-postgres`) |
| API Port | 8080 |
| Validation | Jakarta Bean Validation (`@Valid`, `@Size`, `@Min`, `@Max`) |
| ORM Libs | Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) |
| ORS Base-URL | `https://api.heigit.org` (alte `api.openrouteservice.org` ist deprecated) |

### Paketstruktur

```
at.fhtechnikum.tourplanner
├── config/
│   └── SecurityConfig.java        — CORS, Security-Konfiguration
├── controller/
│   ├── TourController.java        — REST /api/tours (CRUD)
│   ├── TourLogController.java     — REST /api/logs (CRUD)
│   ├── ImportExportController.java— REST /api/import-export
│   ├── SearchController.java      — REST /api/search (Stub)
│   └── AuthController.java        — REST /api/auth
├── service/
│   ├── TourService.java           — Tour CRUD + ORS-Enrichment
│   ├── TourLogService.java        — TourLog CRUD
│   ├── OrsService.java            — ORS API (geocode, directions)
│   ├── ImportExportService.java   — CSV Import/Export (Apache Commons CSV)
│   └── AuthService.java           — Auth / Session
├── dto/
│   ├── tour/
│   │   ├── Tour.java              — @Entity + @JsonProperty("from"/"to"/"fromLat"/…)
│   │   ├── TransportType.java     — Enum: BIKE, HIKE, RUNNING, VEHICLE
│   │   └── OrsRouteResult.java    — Record: distanceKm, durationSeconds, coordinates
│   ├── tourlog/
│   │   └── TourLog.java           — @Entity TourLog
│   ├── auth/                      — LoginRequest, RegisterRequest, AuthResponse, ...
│   ├── importexport/              — ImportResultDto, TourExportDto, ErrorResponseDto
│   └── search/                    — TourSearchResultDto, TourLogSearchResultDto, TotalSearchResultDto
├── repository/
│   ├── TourRepository.java        — JpaRepository<Tour, String>
│   ├── TourLogRepository.java     — JpaRepository<TourLog, String>
│   └── (UserRepository, AppUserRepository, UserSessionRepository)
├── model/                         — Separate model-Klassen (teilw. parallel zu dto/)
│   ├── Tour.java, TourLog.java    — ⚠️ ACHTUNG: doppelte Klassen (model/ und dto/)
│   ├── AppUser.java, UserSession.java
│   └── Search.java
└── exception/
    ├── ResourceNotFoundException.java
    └── GlobalExceptionHandler.java
```

### Tour Entity (dto/tour/Tour.java)

| Feld | Typ | DB | Besonderheit |
|---|---|---|---|
| id | String | NOT NULL PK | UUID, vom Frontend generiert |
| name | String | NOT NULL | @Size(min=3) |
| description | String | NOT NULL | — |
| startLocation | String | NOT NULL | JSON: `"from"` via @JsonProperty |
| endLocation | String | NOT NULL | JSON: `"to"` via @JsonProperty |
| transportType | TransportType | NOT NULL | Enum: BIKE/HIKE/RUNNING/VEHICLE |
| startLat | Double | nullable | JSON: `"fromLat"` — Koordinate aus Autocomplete |
| startLng | Double | nullable | JSON: `"fromLng"` — Koordinate aus Autocomplete |
| endLat | Double | nullable | JSON: `"toLat"` — Koordinate aus Autocomplete |
| endLng | Double | nullable | JSON: `"toLng"` — Koordinate aus Autocomplete |
| distance | double | NOT NULL | Von ORS befüllt (km) |
| estimatedTime | double | NOT NULL | Von ORS befüllt (Sekunden) |
| childFriendliness | int | NOT NULL | 0–5 |
| routeImagePath | String | NOT NULL | Leer-String wenn nicht gesetzt |
| routeGeometry | String | TEXT nullable | JSON: `[[lat,lng],...]` von ORS |
| createdAt | LocalDateTime | NOT NULL | Vom Frontend mitgesendet |

> Die vier Koordinaten-Felder sind **nullable**: Bei frei getippten Adressen
> bleiben sie leer und das Backend geocodiert die Strings (Fallback).

> **Zur Checkliste „incl. Image / map image":** Das Karten-Bild der Tour ist die
> **live gerenderte Leaflet-Karte** (`TourMapComponent`), gezeichnet aus
> `routeGeometry`. Sie erscheint in den Tour-Details. Das Feld `routeImagePath`
> ist ein ungenutztes Alt-Feld (immer Leer-String) — die Karte ersetzt es.

### TourLog Entity (dto/tourlog/TourLog.java)

| Feld | Typ | Besonderheit |
|---|---|---|
| logID | String | JSON: `"logID"`, PK |
| date | String | Format: "YYYY-MM-DD" |
| time | String | Format: "HH:MM" |
| comment | String | — |
| difficulty | int | 0–5 |
| totalDistance | double | km, ≥0 |
| totalTime | double | Minuten, ≥0 |
| rating | int | 0–5 |
| tourID | String | FK zu Tour.id (kein @ManyToOne, nur String) |
| creatorName | String | Username des Erstellers |

### OrsService.java — Methodenübersicht

`OrsService` ist die **Facade** über die ORS-REST-API: er versteckt HTTP/JSON
und bietet dem Business-Layer nur Geocoding, Autocomplete und Routing. Alle
Fehler werden als eigene `OrsServiceException` geworfen (keine rohen HTTP-/
JSON-Exceptions nach außen).

```
autocomplete(text) → List<AddressSuggestion>       /pelias/v1/autocomplete (Backend-Proxy)
geocode(name) → [lng,lat]                          Fallback: /pelias/v1/search
getRoute(double[] from, double[] to, type)         Primär: routet aus Koordinaten
getRoute(String from, String to, type)             Fallback: geocodet, dann routet
requestDirections(from, to, profile)               POST /openrouteservice/v2/directions
parseRoute(json) → OrsRouteResult                  dreht Geometrie auf [lat,lng]
send(request) → HttpResponse                        HTTP-Helper, wrappt IO-Fehler
toOrsProfile(type) → "cycling-regular" | …         Enum → ORS-Profilname
```

Der Autocomplete läuft bewusst **über das Backend** (`GeocodeController` →
`/api/geocode/autocomplete`): so liegt der ORS-Key nur in `application.properties`
und gelangt nie in den Browser.

### TourService.enrichWithOrsData — Entscheidungslogik

```java
if (hasCoordinates(tour))          // alle 4 Koordinaten gesetzt?
    getRoute(fromCoords, toCoords, type)   // Primärweg
else
    getRoute(fromString, toString, type)   // Fallback (geocode)
```
Bei ORS-Fehler: Warning loggen, Tour trotzdem mit distance/time = 0 speichern
(die Tour geht nicht verloren).

### Import/Export (ImportExportService.java)
- CSV-Format mit Apache Commons CSV
- Export: alle Tours + zugehörige TourLogs in einer flachen CSV
- Import: parst CSV, speichert Tour + Log (wenn vorhanden), fehlerhafte Zeilen werden übersprungen
- `routeGeometry` wird beim Import **nicht** von ORS neu berechnet (Direktübernahme aus CSV)

### Auth
- `AuthController`, `AuthService`, `AppUser`, `UserSession` vorhanden
- Route Guard im Frontend (`authGuard`) schützt `/tours`, `/tourlogs`, `/profile`
- Session-basiert (kein JWT sichtbar)

### Bekannte Backend-Probleme / Tech-Debt
- ✅ **Eigene Layer-Exceptions** (`ResourceNotFoundException`, `OrsServiceException`) — gelöst
- ✅ **SearchController** ist jetzt implementiert (`SearchService`, Volltextsuche) — gelöst
- ⚠️ **Leere Gerüstdateien** in `model/` (`Tour`, `TransportType`, `TourLog`, `Search`) — 0 Bytes, kein echtes Duplikat, sollten gelöscht werden (main hat einen Teil schon entfernt)
- ⚠️ `TourLogController.update()` ruft `service.updateTourLog()` zweimal auf (wird vom Kollegen auf main bearbeitet)
- ⚠️ `ImportExportService.validateTour()` und `validateTourBusinessRules()` sind identisch — Duplikat
- ⚠️ `Tour.id` wird vom Frontend als UUID generiert — unüblich, normalerweise Backend-Aufgabe
- ⚠️ TourLog → Tour: kein echter FK-Constraint in JPA (`tourID` als String, nicht `@ManyToOne`)
- ⚠️ `System.out.println()` statt `log.info()` in mehreren Klassen (TourLogService, TourController)

---

## Frontend

### Technologie
| | |
|---|---|
| Framework | Angular 21, standalone components |
| Rendering | SSR (Server-Side Rendering) mit `isPlatformBrowser()` Guards |
| Styling | Bootstrap 5 |
| Karte | Leaflet (nur im Browser, SSR-Guard) |
| State | Angular Signals (`signal()`, `computed()`, `effect()`) |
| HTTP | `HttpClient` mit `withFetch()` (SSR-kompatibel) |
| Forms | Reactive Forms (`FormBuilder`, `Validators`) |

### Routing

| Route | Component | Guard |
|---|---|---|
| `/` | → Redirect zu `/tours` | — |
| `/tours` | ToursComponent | authGuard |
| `/tourlogs` | TourlogsComponent | authGuard |
| `/profile` | ProfileComponent | authGuard |
| `/login` | LoginComponent | — |
| `/register` | RegisterComponent | — |

### Komponenten-Struktur

```
app/
├── tours/
│   ├── tours.ts          — ViewModel: Tour-CRUD, Filter, Stats, Form-State, fromCoords/toCoords
│   ├── tours.html        — Template: Liste, Detail, Modal, Karte
│   └── tours.css
├── tourlogs/
│   ├── tourlogs.ts       — ViewModel: TourLog-CRUD, Popup-Form, Auth
│   └── tourlogs.html
├── tourlogs-list/
│   └── tourlogs-list.ts  — Präsentation: zeigt Log-Liste an
├── shared/
│   ├── address-input/
│   │   └── address-input.ts  — ORS Autocomplete; emittiert Label + Koordinaten
│   ├── tour-map/
│   │   └── tour-map.ts       — Leaflet Karte mit routeGeometry Polyline
│   └── search-bar/
│       └── search-bar.ts     — Einfache Suchleiste (Input + Output)
├── services/
│   └── tour.service.ts   — Model: Signal-State für Tours, API-Calls
├── models/
│   ├── tour.model.ts     — Interface Tour (inkl. fromLat/Lng/toLat/Lng), TRANSPORT_TYPES[]
│   └── tour.model.api.ts — TourApiService (HttpClient CRUD)
├── tourlogs.model/
│   ├── tourlogs.model.ts — Model: Signal-State für TourLogs
│   └── TourLogApiService.ts
├── auth/
│   ├── auth.service.ts   — currentUser Signal, Login/Logout
│   └── auth.guard.ts     — Route Guard
├── login/login.ts
├── register/register.ts
├── profile/profile.ts
└── import-export/
    ├── import-export.ts
    └── importExportService.ts
```

### State-Management-Muster (Angular Signals)

```
TourService (Model)
  _tours = signal<Tour[]>([])
  _selectedTourId = signal<string | null>(null)
  selectedTour = computed(...)      — abgeleiteter Zustand
  stats = computed(...)             — Statistiken
  tourCount = computed(...)

ToursComponent (ViewModel)
  searchTerm = signal<string>('')
  filterType = signal<TransportType | null>(null)
  showForm = signal(false)
  editingTour = signal<Tour | null>(null)
  fromCoords = signal<Coordinates | null>(null)   — aus Autocomplete-Auswahl
  toCoords   = signal<Coordinates | null>(null)
  filteredTours = computed(...)     — kombiniert Model + UI-State
```

### AddressInputComponent — Details

- Ruft den **Backend-Proxy** `GET /api/geocode/autocomplete?text=` auf (kein ORS-Key im Frontend!)
- Bekommt fertige `{label, lat, lng}[]` zurück — kein GeoJSON-Parsing nötig
- Debounce: 300ms · Mindest-Eingabelänge: 3 Zeichen
- Dropdown: `mousedown` statt `click` (verhindert blur-vor-select)
- blur-Event: 150ms Delay vor Schließen des Dropdowns
- Zwei Outputs:
  - `valueChange: string` — Label-Text (bei Tippen & Auswahl)
  - `coordinatesChange: Coordinates | null` — Koordinaten bei Auswahl, `null` bei freiem Tippen
- `isPlatformBrowser()` Guard für SSR

### TourMapComponent — Details

- Input: `routeGeometry: string | null`
- Parsed JSON: `[[lat,lng],[lat,lng],...]`
- Leaflet Polyline auf der Route
- Grüner Marker: `coords[0]` (Start) · Roter Marker: `coords[last]` (Ende)
- Fallback bei null: Wien Zentrum `[48.2082, 16.3738]`
- SSR Guard: Leaflet nur im Browser initialisiert

### Bekannte Frontend-Probleme / Tech-Debt

- ✅ **ORS API Key** nicht mehr im Frontend — läuft über Backend-Proxy (gelöst)
- ⚠️ **Tour.id** wird im Frontend mit `crypto.randomUUID()` generiert — unüblich
- ⚠️ **TourLog.logID** wird mit `Date.now()` generiert (tourlogs.ts) — Kollisions-anfällig
- ⚠️ Import/Export: `importExportService.ts` existiert, aber Verbindung zum Backend unklar
- ⚠️ Suchleiste filtert lokal (Frontend); der Backend-Such-Endpunkt (`/api/search`) ist da, aber noch nicht vom Frontend verdrahtet

---

## Datenbank

```sql
-- Wichtige Tabellen (von Hibernate aus Entities generiert)
tour (
  id VARCHAR PK,
  name VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  start_location VARCHAR NOT NULL,   -- JSON: "from"
  end_location VARCHAR NOT NULL,     -- JSON: "to"
  transport_type VARCHAR NOT NULL,   -- Enum-String (BIKE/HIKE/RUNNING/VEHICLE)
  start_lat DOUBLE,                  -- nullable, JSON: "fromLat"
  start_lng DOUBLE,                  -- nullable, JSON: "fromLng"
  end_lat DOUBLE,                    -- nullable, JSON: "toLat"
  end_lng DOUBLE,                    -- nullable, JSON: "toLng"
  distance DOUBLE NOT NULL,
  estimated_time DOUBLE NOT NULL,
  child_friendliness INT NOT NULL,
  route_image_path VARCHAR NOT NULL,
  route_geometry TEXT,               -- nullable, ORS Polyline JSON
  created_at TIMESTAMP NOT NULL
)

tour_log (
  log_id VARCHAR PK,
  date VARCHAR NOT NULL,
  time VARCHAR NOT NULL,
  comment VARCHAR NOT NULL,
  difficulty INT NOT NULL,
  total_distance DOUBLE NOT NULL,
  total_time DOUBLE NOT NULL,
  rating INT NOT NULL,
  tour_id VARCHAR NOT NULL,          -- kein echter FK-Constraint in JPA
  creator_name VARCHAR NOT NULL
)
```

Seed-Daten in `init.sql` (beim ersten Container-Start) und `data.sql` (bei jedem
Backend-Start, idempotent via `ON CONFLICT DO NOTHING`):
- 5 Tours (IDs '1'–'5'), Tour '5' = VEHICLE
- Tour Logs (IDs: 1, 2, 6, 7, 8, 9, 10)

> Hinweis: `data.sql` enthält noch `DROP CONSTRAINT IF EXISTS`-Zeilen aus früheren
> Schema-Änderungen. Bei einer **frischen** DB (Volume-Reset, `docker compose
> down -v`) sind sie überflüssig, da Hibernate alles korrekt neu anlegt.

---

## Konfiguration

| Datei | Beschreibung |
|---|---|
| `application.properties` | DB + ORS Key + Base-URL. Steht in `.gitignore`, ist aber **trotzdem getrackt** (vor dem Ignore committet) → Key liegt in der History, sollte rotiert + untracked werden |
| `application.example.properties` | Vorlage ohne echten Key |
| `compose.yaml` | PostgreSQL-Container (`tourlog-postgres`, Port 5432) |
| `init.sql` | Schema + Seed beim ersten Container-Start |
| `data.sql` | Seed bei jedem Backend-Start (idempotent) |

Relevante Properties:
```properties
ors.api.key=…                 # geheim, nicht committen
ors.api.base-url=https://api.heigit.org
ors.geocode.focus-lat=48.2082 # optional, Default Wien (Fallback-Geocode-Bias)
ors.geocode.focus-lon=16.3738
```

---

## API-Endpunkte Übersicht

| Methode | URL | Beschreibung |
|---|---|---|
| GET | /api/tours | Alle Tours |
| GET | /api/tours/{id} | Eine Tour |
| POST | /api/tours | Tour erstellen (triggert ORS) |
| PUT | /api/tours/{id} | Tour updaten (triggert ORS) |
| DELETE | /api/tours/{id} | Tour löschen |
| GET | /api/logs | Alle TourLogs |
| POST | /api/logs | TourLog erstellen |
| PUT | /api/logs/{id} | TourLog updaten |
| DELETE | /api/logs/{id} | TourLog löschen |
| GET | /api/geocode/autocomplete | Adress-Autocomplete (ORS-Proxy, Key bleibt im Backend) |
| POST | /api/import-export/import | CSV importieren |
| GET | /api/import-export/export | CSV exportieren |
| POST | /api/auth/login | Login |
| POST | /api/auth/register | Registrierung |
| GET | /api/search/search | Volltextsuche Tours + Logs (implementiert) |
| GET | /api/search/tours/search | Volltextsuche nur Tours |
| GET | /api/search/tourlogs/search | Volltextsuche nur Logs |

---

## Offene Punkte / Verbesserungspotenzial

### Erledigt ✅
- **ORS-Geocoding-Bug** (Adresse landete daneben): gelöst durch Koordinaten-
  Durchreichen vom Autocomplete (kein doppeltes Geocoding mehr)
- **ORS-Migration** auf `api.heigit.org` (alte Domain deprecated)
- **VACATION → VEHICLE** umbenannt, Mapping auf `driving-car`
- **Eigene Layer-Exceptions** (`ResourceNotFoundException`, `OrsServiceException`) statt roher `RuntimeException`/`Exception`
- **ORS-Key aus dem Frontend** entfernt — Backend-Proxy (`/api/geocode/autocomplete`)
- **Full-Text-Search** im Backend implementiert (war Stub)
- **Design Pattern** dokumentiert (Facade = `OrsService`)

### Wichtig (Tech-Debt)
1. **Leere Gerüstdateien** in `model/` löschen (kein echtes Duplikat — sind 0 Bytes)
2. `Tour.id` und `TourLog.logID` im Frontend generiert → sollte Backend-Aufgabe sein (`@GeneratedValue`)
3. `TourLogController.update()` ruft Service zweimal auf (Kollege bearbeitet das auf main)
4. `System.out.println()` durch `log.info()` ersetzen (TourLogService, TourController)
5. ⚠️ **Sicherheit:** `application.properties` (echter ORS-Key) ist getrackt + in Git-History → Key rotieren, Datei untracken

### Nice-to-have
6. Frontend-Suchleiste an Backend-`/api/search` verdrahten (statt nur lokalem Filter)
7. Responsiveness / CSS Design verbessern
8. TourLog → Tour: echten JPA FK-Constraint setzen (`@ManyToOne @JoinColumn`)
9. CSV-Import triggert keine ORS-Neuberechnung für routeGeometry
10. Auth: Session-Management prüfen (JWT vs Session)
