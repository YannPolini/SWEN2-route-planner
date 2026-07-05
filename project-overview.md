# TourPlanner — Projektüberblick

> SWEN2 · Branch: `main` (Stand: aktueller Code, verifiziert am 2026-07-04)
> Stack: Spring Boot 3.3.5 / Java 21 · Angular 21.1 (SSR, standalone components) · PostgreSQL 16 · OpenRouteService API · OpenWeatherMap API

---

## Architektur auf einen Blick

```
Browser (Angular 21 SSR)
    ├── auth (Login, Register, Profile, authGuard)
    ├── tours (Hauptseite: Liste, Detail, Map, Form, Weather, Demo-Seed)
    │     ├── TourService (Model/Signal-State)
    │     ├── AddressInputComponent (ORS Autocomplete → liefert Koordinaten)
    │     └── TourMapComponent (Leaflet Karte)
    ├── tourlogs (Logs zur ausgewählten Tour)
    └── import-export (CSV Import/Export)

Spring Boot 8080
    ├── /api/auth        ← AuthController → AuthService → AppUserRepository/UserSessionRepository
    ├── /api/tours        ← TourController → TourService → OrsService → ORS API
    ├── /api/tours/{id}/weather ← TourController → WeatherService → OrsService (Geocode-Fallback) → OpenWeather API
    ├── /api/logs         ← TourLogController → TourLogService
    ├── /api/geocode      ← GeocodeController → OrsService (Autocomplete-Proxy, Key bleibt im Backend)
    ├── /api/import-export← ImportExportController → ImportExportService (CSV)
    └── /api/demo-data    ← DemoDataController → DemoDataService (befüllt leere Route Library)

PostgreSQL (tourlogdb, Docker: tourlog-postgres)
    ├── tour        (+ owner_user_id, creator_name)
    ├── tour_log    (+ owner_user_id, creator_name)
    ├── app_user
    └── user_session
```

> **Wichtig gegenüber älteren Notizen:** Ein `/api/search`-Feature (SearchController/SearchService)
> existierte auf einem Feature-Branch, ist aber **nicht** in `main` gelandet — es gibt aktuell
> keinen Such-Endpunkt im Backend. Die Suchleiste im Frontend filtert ausschließlich lokal
> (siehe `ToursComponent.filteredTours`). Wenn der Professor danach fragt: ehrlich sagen, dass
> serverseitige Suche nicht existiert.

---

## 1) Layer-Architektur (UI / BL / DAL) — konkret am Tour-Feature

| Layer | Klassen (Tour-Feature) | Aufgabe |
|---|---|---|
| **UI** | `ToursComponent` (`tours.ts`/`tours.html`), `AddressInputComponent`, `TourMapComponent`, `TourApiService` | Darstellung, Formular, HTTP-Aufruf ans Backend |
| **BL** (Business Logic) | `TourController` (Einstiegspunkt REST), `TourService`, `OrsService`, `WeatherService`, `TourMetricsCalculator` | Validierung/Orchestrierung, ORS-Anreicherung, Berechnungen |
| **DAL** (Data Access) | `TourRepository`, `TourLogRepository` (Spring Data JPA) | reiner Datenzugriff auf Postgres |

**Aufrufkette (Beweis: keine Skip-Layer-Calls) am Beispiel `POST /api/tours`:**

```
TourController.create()
    → authService.requireUser(header)        // BL → BL (AuthService)
    → service.createTour(dto, owner)          // BL (Controller → Service)
        → requireNewTourId() / assignOwner()  // BL-intern
        → enrichWithOrsData()                 // BL → BL (OrsService, eigener Service)
        → TourMetricsCalculator.updateChildFriendliness()  // BL-intern (statische Utility)
        → repository.save(tour)               // BL → DAL (TourRepository)
```

Der Controller ruft **nie** direkt ein Repository auf — jeder REST-Endpunkt geht über einen
`@Service`. `TourController` selbst hat außerdem keine SQL-/Persistenzabhängigkeit importiert.

> **Eine Unschärfe, die man ehrlich zugeben sollte:** `TourService` injiziert nicht nur sein
> "eigenes" `TourRepository`, sondern auch `TourLogRepository` direkt (für kaskadierendes Löschen
> in `deleteTour()` und für die Differenzberechnung in `updateTourMetrics()`), statt über
> `TourLogService` zu gehen. Das ist **kein UI→DAL-Skip** (bleibt innerhalb der BL→DAL-Regel),
> aber es ist eine Service-Klasse, die auf ein fremdes Aggregat (TourLog) zugreift, statt den
> zuständigen Service zu fragen. Wenn danach gefragt wird: zugeben, nicht schönreden.

### Layer-eigene Exceptions

| Exception | Wo geworfen | HTTP-Mapping (`GlobalExceptionHandler`) |
|---|---|---|
| `ResourceNotFoundException` | `TourService`, `TourLogService` — Tour/Log-ID existiert nicht | 404 Not Found |
| `OrsServiceException` | `OrsService` — jeder Fehler der ORS-API (HTTP-Fehler, leere Antwort, IO-Fehler) | 502 Bad Gateway |
| `WeatherServiceException` | `WeatherService` — jeder Fehler der OpenWeather-API | 502 Bad Gateway |
| `IllegalArgumentException` | `TourLogService`, `ImportExportService` — fachliche Eingabefehler (z.B. unbekannte `tourID`) | 400 Bad Request |

Rohe technische Exceptions (`SQLException`, `IOException`, `JsonProcessingException`) werden
**nirgends** an den Aufrufer durchgereicht — `OrsService.send()`/`WeatherService.send()` fangen
`IOException`/`InterruptedException` und wrappen sie in die eigene Exception.

> **Ehrliche Einschränkung:** Für Berechtigungsprüfungen (`requireOwner`, `requireNewTourId` in
> `TourService`, `AuthService.requireUser`) wird direkt Springs **`ResponseStatusException`**
> geworfen statt einer eigenen Domain-Exception. Das ist kein technischer Leak (keine
> SQL-/HTTP-Client-Exception), aber auch keine "echte" eigene Layer-Exception-Klasse — es ist
> eine Framework-Klasse, die man mit Status+Message parametriert. Sauberer wäre z.B. eine eigene
> `ForbiddenException`/`ConflictException`. Wenn gefragt wird "sind das eigene Exceptions?" —
> ehrliche Antwort: teilweise; die fachlichen Fehlerfälle (not found, ORS, Weather) haben eigene
> Klassen, die Auth/Ownership-Fälle nutzen Spring direkt.
>
> Zusätzlich: Bean-Validation-Fehler (`@Valid` schlägt fehl, z.B. `name` < 3 Zeichen) werfen
> `MethodArgumentNotValidException`. Dafür gibt es in `GlobalExceptionHandler` **keinen eigenen
> Handler** — diese Fälle landen im Default-Verhalten von Spring Boot (eigenes JSON-Format,
> nicht `ErrorResponseDto`). Das ist inkonsistent und vor der Prüfung einmal mit Postman
> gegenprüfen (z.B. `POST /api/tours` mit `name: "ab"`).

---

## 2) MVVM beim Tour-Feature

| MVVM-Rolle | Datei | Inhalt |
|---|---|---|
| **View** | `tours.html` | reines Template, keine Logik außer Angular-Control-Flow (`@if`/`@for`) |
| **ViewModel** | `tours.ts` → `ToursComponent` | UI-State (Signals), Formular (`ReactiveFormsModule`), Event-Handler, delegiert an Model |
| **Model** | `services/tour.service.ts` → `TourService` (State) + `models/tour.model.ts` (Interface `Tour`) + `models/tour.model.api.ts` (`TourApiService`, HTTP) | Zustand, API-Zugriff, keine UI-Logik |

### Konkrete Data-Binding-Beispiele (`tours.html` ↔ `tours.ts`)

```html
<!-- Interpolation: computed()-Signal direkt im Template ausgelesen -->
<strong>{{ stats().count }}</strong>
<strong>{{ stats().totalDistance | number: '1.1-1' }} km</strong>

<!-- Event-Binding: Klick ruft ViewModel-Methode -->
<button (click)="openCreateForm()">New Tour</button>

<!-- Property-Binding: CSS-Klasse abhängig von Signal-Wert -->
<button [class.active]="filterType() === type.value" (click)="setFilterType(type.value)">

<!-- Reactive Forms: formControlName statt [(ngModel)] -->
<form [formGroup]="tourForm" (ngSubmit)="submitForm()">
  <input formControlName="name" />

<!-- Custom-Component mit zwei Outputs (siehe address-input.ts) -->
<app-address-input
  [value]="tourForm.controls.from.value"
  [invalid]="(formSubmitted() || tourForm.controls.from.touched) && !!tourForm.controls.from.errors"
  (valueChange)="tourForm.controls.from.setValue($event)"
  (coordinatesChange)="fromCoords.set($event)"
/>

<!-- Karte bekommt Route als Input-Signal -->
<app-tour-map [fromLocation]="tour.from" [toLocation]="tour.to" [routeGeometry]="tour.routeGeometry" />
```

`ToursComponent` verwendet **kein** `[(ngModel)]` (Two-Way-Binding), sondern **Reactive Forms**
(`FormBuilder`, `formControlName`) — bewusste Wahl, weil Validierung (`Validators.required`,
`minLength(3)`) deklarativ am `FormGroup` hängt und der Formularzustand (`invalid`, `touched`)
direkt abfragbar ist, ohne manuelles Event-Handling pro Feld.

### Warum Signals statt klassischem MVVM mit RxJS/Observables überall?

`TourService` hält den State in `signal<Tour[]>([])`. `computed()` (`filteredTours`, `stats`,
`selectedTour`) leitet reaktiv ab, ohne dass die Komponente sich manuell subscriben/unsubscriben
muss (kein `async` Pipe, keine `OnDestroy`-Unsubscribe-Logik nötig). **RxJS/`Observable` wird
trotzdem verwendet — aber nur für die eigentlichen HTTP-Calls** (`HttpClient` gibt immer
`Observable` zurück), z.B. `this.tourService.deleteAllTours().subscribe(...)` in `tours.ts`.
Sobald die HTTP-Antwort da ist, wird sie in ein Signal geschrieben (`this._tours.set(tours)`),
und ab da läuft alles synchron über Signals. Kurz: **Observable für Async-I/O, Signal für
UI-State** — das ist der Grund, warum beides im Projekt vorkommt.

---

## 3) Design Pattern(s)

### Facade — `OrsService` (Haupt-Pattern) und analog `WeatherService`

`OrsService` kapselt die komplette OpenRouteService-REST-API (zwei verschiedene Endpunkte:
Pelias-Geocoding und Directions) hinter vier einfachen Methoden (`geocode`, `autocomplete`,
`getRoute(...)` ×2). Der Business-Layer (`TourService`) ruft nur `getRoute(...)` auf und weiß
nichts von HTTP, URLs, JSON-Parsing, Koordinaten-Reihenfolge oder API-Key.

```
TourService ─► OrsService (Facade) ─► HTTP/JSON ─► ORS-API
   weiß nur          versteckt:
   "route mir A→B"   URLs, Key, Parsing, Fehler

TourController ─► WeatherService (Facade) ─► HTTP/JSON ─► OpenWeather-API
                      + nutzt intern OrsService.geocode() als Fallback
```

`WeatherService` ist strukturell dasselbe Pattern, nochmal unabhängig implementiert — falls
gefragt wird "gibt's das Pattern öfter", ist das der zweite Beleg.

### Repository — Spring Data JPA
`TourRepository`/`TourLogRepository`/`AppUserRepository`/`UserSessionRepository`
(`extends JpaRepository`) abstrahieren den Datenzugriff. Der Service kennt keine SQL-Details,
nur `findAll()`, `save()`, oder abgeleitete Methoden wie `findByOwnerUserId(Long)`.

### Observer — Angular Signals (Frontend)
Der Zustand liegt in `signal()`s; `computed()` und `effect()` sind die Observer, die sich
automatisch neu berechnen, wenn sich ein Signal ändert (z.B. `filteredTours` reagiert auf
`searchTerm` + `filterType` + `tourService.tours()`). Ein `effect()` in `TourService` reagiert
sogar auf einen Signal aus einem **anderen** Service (`authService.currentUser()`) und lädt bei
Login/Logout automatisch die Tourenliste neu.

> **Was NICHT als Pattern gilt (Vorsicht in der Prüfung):** `TourMetricsCalculator` ist eine
> statische Utility-Klasse (privater Konstruktor, nur `static`-Methoden). Das ist **kein**
> Strategy-Pattern (es gibt nur einen Algorithmus, keine austauschbaren Implementierungen) und
> kein GoF-Pattern — einfach eine reine Berechnungsfunktion, ausgelagert aus `TourService` fürs
> Unit-Testen. Wenn der Professor "welches Pattern ist das?" fragt: ehrlich sagen "keins, ist
> pure Function extraction".

---

## 4) CRUD für Tours — kompletter Weg durch alle Layer

### Create (`POST /api/tours`)

```
UI: ToursComponent.submitForm()
    → baut Omit<Tour,'id'|'createdAt'> aus dem FormGroup + fromCoords()/toCoords()
    → TourService.addTour(data)                       [Model]
        → generiert id = crypto.randomUUID()            ⚠️ Frontend generiert die ID!
        → TourApiService.create(tour)                   [HTTP POST, Bearer-Token-Header]
BL:  TourController.create(header, @Valid Tour dto)
    → authService.requireUser(header)                  → AppUser owner
    → TourService.createTour(dto, owner)
        → requireNewTourId()   (409 falls ID schon existiert)
        → assignOwner()        (setzt ownerUserId/creatorName SERVERSEITIG, ignoriert Client-Werte)
        → enrichWithOrsData()  → OrsService.getRoute(...) → distance/estimatedTime/routeGeometry
        → TourMetricsCalculator.updateChildFriendliness()
DAL: repository.save(tour)                              → Hibernate INSERT
DB:  INSERT INTO tour (...)
```

### Modify (`PUT /api/tours/{id}`)
Gleicher Weg, aber `TourService.updateTour(id, tour, owner)` prüft zuerst `requireOwner()`
(403 falls die Tour einem anderen User gehört), übernimmt dann `tourId` in `tour.setId(...)`
und ruft erneut `enrichWithOrsData()` — **die Route wird bei jedem Update neu von ORS berechnet**,
auch wenn sich nur der Name geändert hat (kein Dirty-Checking auf from/to).

### Delete (`DELETE /api/tours/{id}`)
`TourService.deleteTour(id, owner)` prüft Ownership, löscht zuerst abhängige `TourLog`-Zeilen
(`tourLogRepository.deleteByTourID(id)`), dann die Tour selbst — verhindert verwaiste Logs, weil
es **keinen echten JPA-`@ManyToOne`-FK** zwischen `TourLog.tourID` und `Tour.id` gibt (nur ein
String-Feld, siehe Datenbank-Abschnitt).

### Pflichtattribute einer Tour (siehe `model/Tour.java`)

| Feld | Pflicht? | Validierung |
|---|---|---|
| `name` | ja | `@Size(min=3)` |
| `description` | ja | `@Column(nullable=false)`, keine Bean-Validation-Annotation |
| `from`/`to` (JSON) → `startLocation`/`endLocation` | ja | `@Column(nullable=false)` |
| `transportType` | ja | Enum BIKE/HIKE/RUNNING/VEHICLE |
| `fromLat/fromLng/toLat/toLng` | **optional** | nullable — nur gesetzt, wenn User einen Autocomplete-Vorschlag gewählt hat |

**"Inkl. Bild":** Es gibt **kein hochgeladenes Bild** und kein `MultipartFile` für Touren — das
Feld `routeImagePath` existiert in der Entity, ist aber **immer ein Leerstring** und wird nie
befüllt. Die Prüfungs-Vorgabe "Bild" wird stattdessen durch die **live gerenderte Leaflet-Karte**
(`TourMapComponent`) erfüllt, die aus `routeGeometry` gezeichnet wird und in der Detailansicht
sichtbar ist. Das muss man dem Professor aktiv so erklären, sonst wirkt es wie eine fehlende
Anforderung.

### Computed (berechnete) Attribute

| Attribut | Wann berechnet | Wie |
|---|---|---|
| `distance`, `estimatedTime`, `routeGeometry` | bei jedem Create/Update | `OrsService.getRoute(...)` → ORS-Response, **wird persistiert** |
| `childFriendliness` (0-5, persistiert) | bei jedem Create/Update **und zusätzlich erneut bei jedem Read** (`getAllTours`/`getTourById`/`getToursForUser`) | `TourMetricsCalculator.calculateChildFriendliness()` — Punkteabzug nach Distanz, Dauer, Transportmittel (siehe Tabelle unten) |
| `difficulty` (0-5, **nicht persistiert**, `@Transient`) | nur zur Laufzeit bei jedem Read | Durchschnitt aus allen `TourLog.difficulty`-Werten dieser Tour (`TourService.calculateAverageDifficulty`), `null` wenn keine Logs existieren |

`childFriendliness`-Logik (`TourMetricsCalculator`): Start bei 5 Punkten, Abzug für lange
Distanz (>15km: -3, >8km: -2, >4km: -1), Abzug für lange Dauer (>180min: -2, >60min: -1),
zusätzlicher Abzug für `RUNNING` (-1) und für `BIKE` bei >8km (-1), Ergebnis auf `[0,5]` geklemmt.

### Listenansicht
`ToursComponent.filteredTours` (computed) kombiniert `TourService.tours()` mit lokalem
Suchbegriff (`searchTerm`) und Transporttyp-Filter (`filterType`) — **rein clientseitig**, kein
Server-Roundtrip pro Tastendruck. Gerendert als Kartenliste (`@for (tour of filteredTours())`),
jede Karte zeigt Badge, Name, Route, Distanz, Dauer, Log-Anzahl, Difficulty.

### Detailansicht (inkl. Map)
Route `/tours/:id` → `ToursComponent` liest `route.paramMap`, setzt `detailTourId` und
`tourService.selectTour(id)`, lädt zusätzlich das Wetter (`loadWeather(id)`). Das Template
rendert bei gesetzter ID `selectedTour()` statt der Liste: Kopfbereich, `<app-tour-map>` mit der
gespeicherten `routeGeometry`, Fakten-Grid (Distanz, Dauer, Child-Friendly, Difficulty, Datum,
Log-Anzahl), Wetter-Karte, und darunter eingebettet `<app-tourlogs>` für die zugehörigen Logs.

---

## 5) Datenbank & OR-Mapping

- **OR-Mapping-Library:** Hibernate (via `spring-boot-starter-data-jpa`), `ddl-auto=update`
  (Schema wird aus den `@Entity`-Klassen generiert/migriert, kein manuelles DDL für neue Spalten
  nötig — `data.sql` enthält nur noch punktuelle, idempotente Migrationsbefehle wie
  `ADD COLUMN IF NOT EXISTS owner_user_id`).
- **SQL-Injection-Schutz:** Es gibt **keine einzige** manuell zusammengebaute SQL-Query im
  Projekt. Alle Datenbankzugriffe laufen über `JpaRepository`-Methoden — entweder
  Standardmethoden (`findAll`, `save`, `existsById`) oder **abgeleitete Query-Methoden**
  (`findByOwnerUserId`, `findByTourIDAndOwnerUserId`, `deleteByTourID`), die Spring Data aus dem
  Methodennamen generiert und intern als **parametrisierte** JPQL/SQL-Queries ausführt. Nirgends
  wird ein Parameter per String-Konkatenation in eine Query eingebaut.
- **DB-Connection-String liegt NICHT im Code:** `application.properties` enthält nur
  Platzhalter (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${ORS_API_KEY}`,
  `${OPENWEATHER_API_KEY}`). Die echten Werte stehen in `backend/.env` (durch
  `spring-dotenv`-Dependency automatisch geladen), `.env` ist in `.gitignore` eingetragen und
  **nicht** in Git getrackt. `application.properties` selbst ist zwar getrackt, enthält aber
  keine Geheimnisse mehr (nur die `${VAR}`-Referenzen) — der frühere Secret-Leak (ORS-Key direkt
  im Klartext committet) ist behoben.

---

## 6) ORS-Integration (OpenRouteService Directions API) — Kernstück

Das wichtigste Konzept: **Adressen werden nur EINMAL geocodiert — beim Tippen im Autocomplete.**
Die dort ermittelten Koordinaten werden bis zur Routenberechnung durchgereicht. Es gibt keine
zweite Geocodierung im Normalfall.

### Warum das wichtig ist
ORS hat zwei getrennte Endpunkte: **Pelias Geocoding** (`/pelias/v1/autocomplete`,
`/pelias/v1/search`: Text → Koordinaten) und **Directions**
(`/openrouteservice/v2/directions/{profile}/geojson`: Koordinaten → Route). Würde man die
Adresse erst im Frontend (Autocomplete) und dann nochmal im Backend geocodieren, könnten zwei
verschiedene Punkte herauskommen (die Endpunkte ranken unterschiedlich) — daher das
Koordinaten-Durchreichen.

### Primärweg (User wählt einen Autocomplete-Vorschlag)

```
1. User tippt "Handelskai" in <app-address-input> (300ms debounce, ab 3 Zeichen)
        ▼
2. GET http://localhost:8080/api/geocode/autocomplete?text=Handelskai   (Backend-Proxy!)
        │  GeocodeController → OrsService.autocomplete("Handelskai")
        │  → GET https://api.heigit.org/pelias/v1/autocomplete?api_key=…&text=…&size=5
        │      &focus.point.lat=48.2082&focus.point.lon=16.3738
        │  Backend parst GeoJSON → List<AddressSuggestion(label, lat, lng)>
        ▼
3. Dropdown zeigt Labels. User klickt "Handelskai, Wien, Österreich"
        ▼
4. AddressInputComponent emittiert:
        valueChange       → "Handelskai, Wien, Österreich"
        coordinatesChange → { lat: 48.225874, lng: 16.40495 }
        ▼
5. ToursComponent speichert beides: tourForm.controls.from + fromCoords (Signal)
```

Beim Absenden wird `distance/estimatedTime/routeGeometry` als 0/0/null mitgeschickt — Backend
berechnet sie gleich neu:

```
6. TourController.create() → TourService.createTour() → enrichWithOrsData(tour)
        hasCoordinates(tour) == true (alle 4 Koordinaten gesetzt) → Primärweg
        ▼
7. OrsService.getRoute([16.40495,48.225874], [16.41835,48.24210], BIKE)
        → toOrsProfile(BIKE) = "cycling-regular"
        → requestDirections(): POST https://api.heigit.org/openrouteservice/v2/directions/cycling-regular/geojson
              Header: Authorization: <api_key>
              Body:   {"coordinates":[[16.40495,48.225874],[16.41835,48.24210]]}
        ▼
8. ORS-Antwort (gekürzt): { "features": [{ "properties": { "summary": { "distance": 2910.5, "duration": 654.0 } },
                                            "geometry": { "coordinates": [[16.40495,48.225874], …] } }] }
        ▼
9. parseRoute(): distanceKm = 2.91, durationSeconds = 654.0,
                 coordinates auf [lat,lng] gedreht (ORS liefert [lng,lat])
        ▼
10. adjustDuration(): nur relevant, wenn transportType == RUNNING (siehe unten)
        ▼
11. Tour wird befüllt (distance, estimatedTime, routeGeometry als JSON-String) und gespeichert.
        ▼
12. Frontend: loadTours() → Signal aktualisiert → TourMapComponent zeichnet Polyline
```

### Fallback-Weg (User tippt frei, wählt keinen Vorschlag)
`onInput()` in `AddressInputComponent` setzt `coordinatesChange → null` bei jedem Tastendruck.
Ohne Koordinaten: `hasCoordinates(tour) == false` →
`OrsService.getRoute("Stephansplatz 1", "Praterstern", type)` → geocodiert beide Strings über
`/pelias/v1/search?text=…&size=1&focus.point.lat=…` (Wien-Bias, keine harte Einschränkung), dann
dieselbe Directions-Anfrage.

### Transport-Profil-Mapping + Lauf-Sonderfall

| TransportType | ORS-Profil | Besonderheit |
|---|---|---|
| BIKE | `cycling-regular` | — |
| HIKE | `foot-hiking` | — |
| RUNNING | `foot-walking` | ORS hat **kein eigenes Laufprofil** — deshalb wird das Walking-Profil für die Geometrie/Distanz verwendet, aber `OrsService.adjustDuration()` überschreibt danach die Dauer: `durationSeconds = distanceKm / 10.0 * 3600` (angenommene 10 km/h Lauftempo) statt der ORS-Gehzeit |
| VEHICLE | `driving-car` | — |

### Koordinaten-Reihenfolge — Stolperfalle
ORS (Pelias + Directions) liefert/erwartet `[lng, lat]`; Leaflet braucht `[lat, lng]`. Das
Backend dreht **einmalig** in `OrsService.parseRoute()` (Directions) bzw. beim Mapping in
`autocomplete()`/`geocode()`, bevor irgendetwas gespeichert oder zurückgegeben wird — das
Frontend muss nie selbst drehen.

### Fehlerverhalten
Jeder ORS-Fehler (HTTP-Status ≠ 200, leere Feature-Liste, ungültiges JSON, IO-Fehler,
Interrupt) wird in `OrsService` als `OrsServiceException` geworfen. `TourService.enrichWithOrsData()`
fängt genau diese Exception (plus `JsonProcessingException` beim Serialisieren der Geometrie) ab,
loggt eine Warnung und **speichert die Tour trotzdem** mit den bisherigen/Default-Werten
(`distance=0`, `estimatedTime=0`) — die Tour geht bei ORS-Ausfall nicht verloren, wird aber ohne
Route gespeichert, kein Retry. Ruft man dagegen `/api/geocode/autocomplete` direkt auf und ORS
antwortet mit Fehler, läuft die Exception bis zum `GlobalExceptionHandler` durch → 502 Bad
Gateway direkt an den Client.

Der ORS-API-Key verlässt **nie** den Server: `AddressInputComponent` ruft
`http://localhost:8080/api/geocode/autocomplete` auf (kein Key im Bundle/DevTools sichtbar).

---

## 7) Leaflet-Darstellung

`TourMapComponent` (`shared/tour-map/tour-map.ts`):
- **Input-Signal** `routeGeometry: string | null` — JSON-String `[[lat,lng],...]` aus der Tour.
- `ngAfterViewInit()`: dynamischer `import('leaflet')` (nur im Browser, `isPlatformBrowser()`-Guard
  wegen SSR — Leaflet greift auf `window`/`document` zu, die es beim Server-Rendering nicht gibt),
  erstellt die Karte mit OpenStreetMap-Tile-Layer, zentriert initial auf Wien.
- Ein `effect()` im Konstruktor beobachtet `routeGeometry`/`fromLocation`/`toLocation` und ruft
  `redrawRoute()`, sobald sich der Input ändert (z.B. nach Editieren der Tour) — reines
  Observer-Verhalten über Angular Signals, kein manuelles Re-Subscribe nötig.
- `redrawRoute()`: parst das JSON, zeichnet eine `L.polyline(coords, {...})`, setzt grünen Marker
  auf `coords[0]` (Start) und roten Marker auf `coords[coords.length-1]` (Ende), zoomt per
  `fitBounds()` auf die Route. Fällt `routeGeometry` leer/ungültig aus, bleibt die Wien-Übersicht.
- **Verbindung zur Tour-Detailansicht:** `tours.html` bindet `[routeGeometry]="tour.routeGeometry"`
  direkt an das vom Backend gelieferte, bereits auf `[lat,lng]` gedrehte Geometrie-JSON — die
  Karte braucht keine eigene Transformationslogik.

---

## 8) Input-Validierung

| Ebene | Wo | Beispiel |
|---|---|---|
| **Frontend (UX)** | `tourForm` (Reactive Forms) in `tours.ts` | `Validators.required`, `Validators.minLength(3)` auf `name` — verhindert Absenden, zeigt Inline-Fehlermeldung im Template (`@if (... tourForm.controls.name.errors)`) |
| **Backend (Bean Validation)** | `@Valid @RequestBody Tour dto` in `TourController`, Annotationen direkt auf der Entity (`@Size(min=3)` auf `name`, `@Min`/`@Max` auf `childFriendliness`) | Bei Verstoß: `MethodArgumentNotValidException` — **kein eigener Handler**, Spring-Boot-Default-Fehlerformat (siehe Abschnitt 1) |
| **Backend (fachlich, manuell)** | `TourService`/`TourLogService`/`ImportExportService` | z.B. `requireExistingTour()` prüft, ob `tourID` eines Logs überhaupt existiert → `IllegalArgumentException` → 400 mit `ErrorResponseDto` |
| **Backend (Ownership)** | `requireOwner()` in `TourService`/`TourLogService` | fremde Tour bearbeiten/löschen → `ResponseStatusException(403 FORBIDDEN)` |

Kein Crash bei Fehleingabe: Alle Fehlerpfade enden in einer Exception, die entweder vom
`GlobalExceptionHandler` (eigene Typen + `ResponseStatusException` + `RuntimeException`-Catch-all)
oder von Spring Boots Default-Mechanismus (Bean-Validation-Fehler) in eine HTTP-Fehlerantwort
umgewandelt wird — der Client bekommt immer eine strukturierte JSON-Antwort, nie einen 500 ohne
Body oder einen Stacktrace.

---

## 9) Unit Tests

### Backend (`backend/src/test/java/.../service/`)

| Testklasse | Testet | Mocking-Ansatz |
|---|---|---|
| `TourServiceTest` | `TourService` (Create/Update/Delete, Ownership-Checks, ORS-Anreicherung, Difficulty-Berechnung) | `@Mock TourRepository`, `@Mock TourLogRepository`, `@Mock OrsService`, `@Mock ObjectMapper` (Mockito, `@InjectMocks`) — **ORS wird komplett gemockt**, es wird nur geprüft, dass `TourService` die von `OrsService` gelieferten Werte korrekt übernimmt |
| `TourLogServiceTest` | `TourLogService` (CRUD, Ownership, Referenz-Validierung gegen `TourRepository`) | Repository-Mocks |
| `AuthServiceTest` | Register/Login/Me/Logout, Passwort-Hashing-Vergleich, Session-Expiry | Repository-Mocks, **echter** `BCryptPasswordEncoder` (nicht gemockt — testet echtes Hashing) |
| `DemoDataServiceTest` | Seed-Logik (12 Touren/41 Logs aus `demo-data.json`), Konfliktfall (User hat schon Touren) | Repository-Mocks, `ArgumentCaptor` zum Prüfen der kopierten Entities |
| `TourMetricsCalculatorTest` | reine Berechnungslogik (Punkteabzüge nach Distanz/Dauer/Transportmittel) | **kein Mocking** — reiner Unit-Test einer statischen Funktion |
| `WeatherServiceTest` | JSON-Mapping der OpenWeather-Antwort (Gruppierung nach Tag, Mittelwerte), Fehlerfall bei ungültigem JSON | `OrsService` gemockt (nur als Konstruktor-Dependency), **kein echter HTTP-Call** |

**Ehrliche Lücke:** Es gibt **keinen `OrsServiceTest`** — die HTTP-Kommunikation mit ORS selbst
(URL-Aufbau, JSON-Parsing der echten ORS-Antwortstruktur, Fehlerfälle bei HTTP-Fehlercodes) wird
nirgends direkt getestet, nur indirekt dadurch, dass `TourServiceTest` `OrsService` komplett
wegmockt. Ebenso gibt es **keine Controller-Tests** (kein `@WebMvcTest`/`MockMvc`) — die
REST-Schicht (Statuscodes, Exception-Mapping über `GlobalExceptionHandler`) ist nicht durch
automatisierte Tests abgedeckt. Wenn danach gefragt wird: zugeben, nicht behaupten, es sei
getestet.

### Frontend

**Es existieren keine automatisierten Frontend-Tests.** `package.json` → `"test": "echo \"No
automated tests configured\""` — es gibt kein Karma/Jasmine/Jest-Setup und keine `*.spec.ts`-Datei
im gesamten `frontend/src`. Falls der Professor das gezielt abfragt: ehrlich sagen, dass die
gesamte Testabdeckung auf dem Backend liegt.

---

## Backend

### Technologie
| | |
|---|---|
| Framework | Spring Boot 3.3.5 |
| Java | 21 |
| ORM | Hibernate / JPA, `ddl-auto=update` |
| DB | PostgreSQL 16, Port 5432, DB: `tourlogdb` |
| Start DB | `docker compose up -d` (in `backend/`, Container `tourlog-postgres`) |
| API Port | 8080 |
| Validation | Jakarta Bean Validation (`@Valid`, `@Size`, `@Min`, `@Max`) |
| Security | `spring-boot-starter-security` — aktuell nur für `BCryptPasswordEncoder` + CORS/CSRF-Konfiguration genutzt, **keine** Spring-Security-Filterchain-Autorisierung (`permitAll()` auf allen `/api/**`); Auth/Session wird manuell in `AuthService`/Controllern per Bearer-Token geprüft |
| Sonstige Libs | Lombok, Apache Commons CSV (Import/Export), Apache POI (im pom, aktuell ungenutzt für Excel), `spring-dotenv` (lädt `.env`) |
| ORS Base-URL | `https://api.heigit.org` |
| OpenWeather Base-URL | `https://api.openweathermap.org` |

### Paketstruktur (aktueller Stand, nicht mehr `dto/tour/Tour.java`)

```
at.fhtechnikum.tourplanner
├── config/
│   └── SecurityConfig.java        — CORS-Konfiguration, CSRF disabled, permitAll()
├── controller/
│   ├── TourController.java        — REST /api/tours (CRUD + /weather)
│   ├── TourLogController.java     — REST /api/logs (CRUD)
│   ├── ImportExportController.java— REST /api/import, /api/export (CSV)
│   ├── GeocodeController.java     — REST /api/geocode/autocomplete (ORS-Proxy)
│   ├── DemoDataController.java    — REST /api/demo-data/seed
│   └── AuthController.java        — REST /api/auth (register/login/me/logout/editUser)
├── service/
│   ├── TourService.java           — Tour CRUD + ORS-Enrichment + Difficulty-Aggregation
│   ├── TourLogService.java        — TourLog CRUD + Tour-Referenz-Validierung
│   ├── OrsService.java            — Facade: ORS Geocoding/Autocomplete/Directions
│   ├── WeatherService.java        — Facade: OpenWeather Forecast (nutzt OrsService.geocode als Fallback)
│   ├── TourMetricsCalculator.java — statische Utility: childFriendliness-Berechnung
│   ├── ImportExportService.java   — CSV Import/Export (Apache Commons CSV)
│   ├── DemoDataService.java       — kopiert gebündelte Demo-Touren/Logs für einen User
│   └── AuthService.java           — Register/Login/Session/Profil (BCrypt + Bearer-Token)
├── model/                          — @Entity-Klassen (JPA), gleichzeitig JSON-DTO (kein Split!)
│   ├── Tour.java                  — @Entity + @JsonProperty("from"/"to"/"fromLat"/…)
│   ├── TourLog.java                — @Entity
│   ├── TransportType.java          — Enum: BIKE, HIKE, RUNNING, VEHICLE (+ @JsonValue/@JsonCreator)
│   ├── AppUser.java                 — @Entity (id, name, email, passwordHash, createdAt)
│   └── UserSession.java             — @Entity (token als PK, @ManyToOne → AppUser, expiresAt)
├── dto/
│   ├── tour/       — AddressSuggestion (record), OrsRouteResult (record)
│   ├── auth/       — LoginRequest, RegisterRequest, EditUserRequest, AuthResponse, UserResponse
│   ├── importexport/ — ImportResultDto, ErrorResponseDto
│   ├── weather/    — WeatherForecastDto, WeatherForecastDayDto
│   └── demo/       — DemoSeedResponse
├── repository/
│   ├── TourRepository.java        — JpaRepository<Tour, String> + findByOwnerUserId, findByIdAndOwnerUserId
│   ├── TourLogRepository.java      — JpaRepository<TourLog, String> + findByTourID, deleteByTourID
│   ├── AppUserRepository.java
│   └── UserSessionRepository.java
└── exception/
    ├── ResourceNotFoundException.java
    ├── OrsServiceException.java
    ├── WeatherServiceException.java
    └── GlobalExceptionHandler.java  — @RestControllerAdvice, mappt eigene + Spring-Exceptions auf ErrorResponseDto
```

> **Wichtiger Punkt für die Prüfung (Kriterium "DAL/BL/UI-Trennung"):** `Tour`/`TourLog` sind
> **gleichzeitig** JPA-`@Entity` **und** das über REST ausgetauschte JSON-Objekt (nur per
> `@JsonProperty` umbenannte Felder, kein separates Request-/Response-DTO). Ein Split
> (`TourRequest`/`TourResponse`-Records + Mapper) war geplant, wurde aber **nicht umgesetzt** —
> das ist eine reale Schwäche in der strikten Schichtentrennung (die Datenbank-Struktur "leakt"
> 1:1 über die REST-API), auch wenn der Layer-Aufruf selbst (Controller→Service→Repository)
> korrekt ist. Ehrlich ansprechen, wenn danach gefragt wird — nicht als DTO-Pattern verkaufen.

### Tour Entity (`model/Tour.java`) — Felder

| Feld | Typ | DB | Besonderheit |
|---|---|---|---|
| id | String | NOT NULL PK | UUID, vom **Frontend** generiert (`crypto.randomUUID()`) |
| name | String | NOT NULL | `@Size(min=3)` |
| description | String | NOT NULL | — |
| startLocation/endLocation | String | NOT NULL | JSON: `"from"`/`"to"` via `@JsonProperty` |
| transportType | TransportType | NOT NULL | Enum |
| startLat/startLng/endLat/endLng | Double | nullable | JSON: `"fromLat"`/… — nur bei Autocomplete-Auswahl gesetzt |
| distance | double | NOT NULL | von ORS befüllt (km) |
| estimatedTime | double | NOT NULL | von ORS befüllt (Sekunden) |
| childFriendliness | int | NOT NULL | 0–5, `@JsonProperty(READ_ONLY)` — Client kann's nicht setzen |
| difficulty | Double | **`@Transient`, nicht in DB** | `@JsonProperty(READ_ONLY)`, nur zur Laufzeit berechnet |
| ownerUserId | Long | Spalte `owner_user_id` | `@JsonProperty(READ_ONLY)` — serverseitig gesetzt |
| creatorName | String | | `@JsonProperty(READ_ONLY)` |
| routeImagePath | String | NOT NULL | immer Leerstring, ungenutzt (siehe Abschnitt 4) |
| routeGeometry | String | TEXT, nullable | JSON `[[lat,lng],...]` von ORS |
| createdAt | LocalDateTime | NOT NULL | vom Frontend mitgesendet |

### Auth (`AuthService`, `AppUser`, `UserSession`)
- Registrierung/Login: BCrypt-Hash (`BCryptPasswordEncoder`), Session-Token = `UUID.randomUUID()`,
  gültig 7 Tage (`SESSION_DAYS`), gespeichert in `user_session`-Tabelle (kein JWT — Server hält den
  State).
- `AuthService.requireUser(header)` wird **manuell in jedem Controller** aufgerufen (nicht über
  einen Spring-Security-Filter), extrahiert den Bearer-Token, prüft Ablaufzeit, liefert `AppUser`.
- Demo-Account wird beim Start automatisch angelegt (`@PostConstruct seedDemoUser()`):
  `demo@tourplanner.local` / `demo1234`.
- Alle Touren/Logs sind pro `ownerUserId` isoliert (`findByOwnerUserId`) — Mehrbenutzerfähigkeit.

### DemoDataService
Kopiert eine gebündelte Datei `resources/demo/demo-data.json` (12 Touren, 41 Logs) für einen
neuen User, sobald dessen Route Library leer ist (sonst 409 Conflict). IDs werden pro User
eindeutig gemacht (`sourceId + "-user-" + ownerId`).

### Import/Export (`ImportExportService`)
CSV-Format über Apache Commons CSV. Import validiert jede Zeile einzeln (`validateTour`,
`validateTourLogBusinessRules`) und überspringt fehlerhafte Zeilen statt abzubrechen
(`ImportResultDto` mit `importedRows`/`failedRows`/`errors`). `routeGeometry` wird beim Import
**nicht** neu von ORS berechnet — direkte Übernahme aus der CSV.

### Bekannte Backend-Probleme / Tech-Debt (Stand jetzt)
- ⚠️ Kein DTO/Entity-Split (siehe oben) — Entity dient direkt als REST-Body
- ⚠️ Ownership-/Auth-Fehler nutzen Springs `ResponseStatusException` statt eigener Exception-Klassen
- ⚠️ `MethodArgumentNotValidException` hat keinen eigenen Handler → inkonsistentes Fehlerformat bei Bean-Validation-Fehlern
- ⚠️ `TourService` greift direkt auf `TourLogRepository` zu statt über `TourLogService`
- ⚠️ Kein `OrsServiceTest`, keine Controller-Tests
- ⚠️ `Tour.id`/`TourLog.logID` werden vom **Frontend** generiert, nicht per `@GeneratedValue`
- ⚠️ `TourLog.tourID` ist ein reines String-Feld, kein `@ManyToOne`/FK-Constraint in JPA
- ⚠️ Spring Security ist eingebunden, aber `SecurityFilterChain` erlaubt alles (`permitAll()`) — Autorisierung läuft komplett manuell in den Services

---

## Frontend

### Technologie
| | |
|---|---|
| Framework | Angular 21.1, standalone components |
| Rendering | SSR mit `isPlatformBrowser()`-Guards (für `localStorage`, Leaflet) |
| Styling | Bootstrap 5 |
| Karte | Leaflet (dynamischer Import, nur im Browser) |
| State | Angular Signals (`signal()`, `computed()`, `effect()`) |
| HTTP | `HttpClient` (aus `app.config.ts`) |
| Forms | Reactive Forms |
| Tests | **keine** (siehe Abschnitt 9) |

### Routing
| Route | Component | Guard |
|---|---|---|
| `/` | Redirect zu `/tours` | — |
| `/tours`, `/tours/:id` | `ToursComponent` | `authGuard` |
| `/tourlogs` | `TourlogsComponent` (eigenständige Seite, zusätzlich zur Einbettung in Tour-Detail) | `authGuard` |
| `/profile` | `ProfileComponent` | `authGuard` |
| `/login`, `/register` | `LoginComponent`/`RegisterComponent` | — |

### Komponenten-Struktur (relevant fürs Tour-Feature)
```
app/
├── tours/            — tours.ts (ViewModel), tours.html (View), tours.css
├── tourlogs/, tourlogs-list/ — eingebettete Logs zur ausgewählten Tour
├── shared/
│   ├── address-input/  — ORS Autocomplete (Backend-Proxy), zwei Outputs
│   ├── tour-map/        — Leaflet Karte
│   ├── transport-icon/  — Icon je TransportType
│   └── search-bar/       — rein lokale Suchleiste (kein Backend-Call)
├── services/tour.service.ts — Model: Signal-State, reagiert per effect() auf Login/Logout
├── models/
│   ├── tour.model.ts     — Interface Tour, WeatherForecast(-Day), TRANSPORT_TYPES[]
│   └── tour.model.api.ts — TourApiService (HttpClient CRUD + /weather + /demo-data/seed)
├── auth/ — auth.service.ts (Signal currentUser, Bearer-Token in localStorage), auth.guard.ts
└── import-export/ — import-export.ts, importExportService.ts (CSV Upload/Download)
```

### AddressInputComponent — Details
Backend-Proxy `GET /api/geocode/autocomplete?text=` (kein ORS-Key im Frontend). Debounce 300ms,
ab 3 Zeichen. Zwei Outputs: `valueChange` (Text) und `coordinatesChange` (Koordinaten oder
`null` bei freiem Tippen). `mousedown` statt `click` fürs Dropdown (verhindert, dass `blur` vor
der Auswahl feuert), 150ms Delay vor dem Schließen.

### TourMapComponent — Details
Siehe Abschnitt 7.

### Bekannte Frontend-Probleme / Tech-Debt
- ⚠️ `Tour.id` wird mit `crypto.randomUUID()` im Frontend generiert (unüblich, sollte Backend-Aufgabe sein)
- ⚠️ Keine automatisierten Tests
- ⚠️ Suchleiste filtert nur lokal — kein Server-Suchendpunkt vorhanden (siehe oben)

---

## Datenbank

```sql
tour (
  id VARCHAR PK,
  name VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  start_location VARCHAR NOT NULL,   -- JSON: "from"
  end_location VARCHAR NOT NULL,     -- JSON: "to"
  transport_type VARCHAR NOT NULL,
  start_lat DOUBLE, start_lng DOUBLE, end_lat DOUBLE, end_lng DOUBLE,  -- alle nullable
  distance DOUBLE NOT NULL,
  estimated_time DOUBLE NOT NULL,
  child_friendliness INT NOT NULL,
  owner_user_id BIGINT,               -- FK-artig, kein @ManyToOne
  creator_name VARCHAR,
  route_image_path VARCHAR NOT NULL,  -- immer Leerstring
  route_geometry TEXT,
  created_at TIMESTAMP NOT NULL
)

tour_log (
  log_id VARCHAR PK,
  date VARCHAR NOT NULL, time VARCHAR NOT NULL, comment VARCHAR NOT NULL,
  difficulty INT NOT NULL, total_distance DOUBLE NOT NULL, total_time DOUBLE NOT NULL,
  rating INT NOT NULL,
  tour_id VARCHAR NOT NULL,           -- kein echter FK-Constraint in JPA
  owner_user_id BIGINT, creator_name VARCHAR NOT NULL
)

app_user (
  id BIGINT PK (IDENTITY),
  name VARCHAR NOT NULL, email VARCHAR NOT NULL UNIQUE,
  password_hash VARCHAR NOT NULL, created_at TIMESTAMP NOT NULL
)

user_session (
  token VARCHAR PK,
  user_id BIGINT NOT NULL REFERENCES app_user(id),  -- echter @ManyToOne-FK
  created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL
)
```

`data.sql` enthält nur noch idempotente Migrationsbefehle (`ADD COLUMN IF NOT EXISTS`,
Umbenennung alter `VACATION`-Werte auf `VEHICLE`), **keine** Seed-Touren mehr — Demo-Daten kommen
über `DemoDataService`/`/api/demo-data/seed`, nicht mehr über SQL-Insert beim Start.

---

## Konfiguration

| Datei | Beschreibung |
|---|---|
| `backend/src/main/resources/application.properties` | nur `${VAR}`-Platzhalter, **keine** Secrets, getrackt in Git |
| `backend/src/main/resources/application.example.properties` | identische Vorlage für neue Entwickler |
| `backend/.env` | echte Werte (DB-Credentials, ORS-Key, OpenWeather-Key) — **in `.gitignore`, nicht getrackt** |
| `backend/.env.example` | Vorlage ohne echte Werte |
| `backend/compose.yaml` | PostgreSQL-Container (`tourlog-postgres`, Port 5432, DB `tourlogdb`) |
| `backend/src/main/resources/data.sql` | idempotente Schema-Migrationen (kein Seed mehr) |

Relevante Properties (Werte kommen aus `.env` via `spring-dotenv`):
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
ors.api.key=${ORS_API_KEY}
ors.api.base-url=${ORS_API_BASE_URL:https://api.heigit.org}
ors.geocode.focus-lat=${ORS_GEOCODE_FOCUS_LAT:48.2082}
ors.geocode.focus-lon=${ORS_GEOCODE_FOCUS_LON:16.3738}
openweather.api.key=${OPENWEATHER_API_KEY:}
```

---

## API-Endpunkte Übersicht

| Methode | URL | Beschreibung |
|---|---|---|
| POST | /api/auth/register | Registrierung |
| POST | /api/auth/login | Login |
| GET | /api/auth/me | aktueller User (Session-Check) |
| POST | /api/auth/logout | Session löschen |
| PUT | /api/auth/editUser | Profil ändern |
| GET | /api/tours | Alle Touren des eingeloggten Users |
| GET | /api/tours/{id} | Eine Tour |
| GET | /api/tours/{id}/weather | Wettervorhersage fürs Tour-Ende |
| POST | /api/tours | Tour erstellen (triggert ORS) |
| PUT | /api/tours/{id} | Tour updaten (triggert ORS neu) |
| DELETE | /api/tours | Alle Touren des Users löschen |
| DELETE | /api/tours/{id} | Tour löschen |
| GET | /api/logs | Alle Logs des Users |
| POST | /api/logs | Log erstellen |
| PUT | /api/logs/{id} | Log updaten |
| DELETE | /api/logs/{id} | Log löschen |
| GET | /api/geocode/autocomplete | Adress-Autocomplete (ORS-Proxy) |
| POST | /api/import | CSV importieren |
| GET | /api/export?format=csv | CSV exportieren |
| POST | /api/demo-data/seed | Demo-Bibliothek für leeren Account befüllen |

---

## Offene Punkte / Verbesserungspotenzial (aktueller Stand)

### Erledigt ✅
- ORS-Key aus dem Frontend entfernt (Backend-Proxy `/api/geocode/autocomplete`)
- Kein Secret mehr im getrackten `application.properties` (nur `${VAR}`-Referenzen, echte Werte in `.env`)
- Eigene Layer-Exceptions für die fachlichen Hauptfehlerfälle (`ResourceNotFoundException`, `OrsServiceException`, `WeatherServiceException`)
- Mehrbenutzerfähigkeit mit Ownership-Prüfung auf Tour/TourLog
- Demo-Daten-Feature ersetzt die alten SQL-Seed-Inserts (sauberer, über die App-Logik statt Rohdaten)

### Wichtig für die Prüfung (ehrlich ansprechen, nicht kaschieren)
1. Kein DTO/Entity-Split — Tour/TourLog-Entity ist gleichzeitig der REST-Body
2. Ownership-/Auth-Fehler nutzen `ResponseStatusException` statt eigener Exception-Klassen
3. Kein Handler für `MethodArgumentNotValidException` → inkonsistentes Fehlerformat bei Bean-Validation-Fehlern
4. Kein `/api/search`-Endpunkt (obwohl frühere Notizen/Branches das planten) — Suche ist rein Frontend-lokal
5. Keine Frontend-Tests, kein `OrsServiceTest`, keine Controller-Tests im Backend
6. `Tour.id`/`TourLog.logID` werden im Frontend generiert statt vom Backend
7. Kein echter JPA-FK zwischen `TourLog.tourID` und `Tour.id`
8. Spring Security ist eingebunden, aber autorisiert nichts selbst (`permitAll()`); alles läuft über manuelles Bearer-Token-Handling in `AuthService`

### Nice-to-have
- Responsiveness/CSS weiter verbessern
- CSV-Import könnte `routeGeometry` optional neu von ORS berechnen
- `TourService`/`TourLogRepository`-Kopplung entkoppeln (über `TourLogService` gehen)
