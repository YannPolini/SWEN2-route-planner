# Bug Context — ORS Geocoding Incorrect for Innerstädtische Locations

## Bug Description

**Symptom:** When a user types a Vienna-internal location like "Handelskai" (U6 station) or "Donauinsel" (U1 station), the geocoded coordinates land somewhere in the Innenstadt (city center), not at the actual location. The route on the map is wrong.

**Works correctly:** Wien → Graz (inter-city routes).

**Does NOT work correctly:** Any Vienna sub-location that is not unique globally — e.g., street names, U-Bahn stations, districts.

---

## Backend — Geocoding Flow

### Entry Point 
`TourService.java` calls `enrichWithOrsData(tour)` before every `repository.save()` (both create and update).

### OrsService.geocode() — the root cause

File: `backend/src/main/java/at/fhtechnikum/tourplanner/service/OrsService.java`, lines 36–68

```java
public double[] geocode(String placeName) throws Exception {
    String encoded = URLEncoder.encode(placeName, StandardCharsets.UTF_8);
    String url = baseUrl + "/geocode/search?api_key=" + apiKey
            + "&text=" + encoded + "&size=1";
    // ...
    double lng = coords.get(0).asDouble();
    double lat = coords.get(1).asDouble();
    return new double[]{lng, lat}; // [lng, lat] — ORS order
}
```

**Missing parameters:**
- NO `boundary.country=AUT` — query is global, not Austria-scoped
- NO `focus.point.lat` / `focus.point.lon` — no Vienna proximity bias
- NO `boundary.rect.*` — no bounding box for Austria/Vienna

This means "Handelskai" resolves to whatever result ORS considers most globally relevant for that string, which may be a different location entirely (e.g., a generic "Handelskai" result somewhere in the city center, or even a different city).

### What the autocomplete component sends to the backend

The user picks a suggestion from the dropdown like:
`"Handelskai, Wien, Österreich"`

That **full label string** is what gets stored in `tour.from` / `tour.to` and sent to the backend. So `geocode()` receives the full label.

However, ORS geocode/search without a focus point may still resolve ambiguously — the first result for "Handelskai, Wien, Österreich" should be correct in theory, but the absence of `boundary.country=AUT` or `focus.point` leaves it up to ORS's global ranking.

### OrsService.getRoute() — coordinate flow

```java
double[] fromCoords = geocode(from); // returns [lng, lat]
double[] toCoords   = geocode(to);   // returns [lng, lat]
// body sent to ORS Directions:
"coordinates":[[fromCoords[0], fromCoords[1]], [toCoords[0], toCoords[1]]]
// = [[lng, lat], [lng, lat]] — correct ORS order for Directions API
```

### OrsService.parseRoute() — coordinate conversion

```java
for (JsonNode point : features.get(0).path("geometry").path("coordinates")) {
    coords.add(new double[]{point.get(1).asDouble(), point.get(0).asDouble()});
    //                              ^^^lat                    ^^^lng   — swapped for Leaflet
}
// Result stored as [lat, lng] — correct for Leaflet
return new OrsRouteResult(
    distanceKm, durationSeconds, coords,
    fromCoords[1], fromCoords[0],   // lat, lng
    toCoords[1],   toCoords[0]);    // lat, lng
```

Coordinate conversion logic appears correct. The bug is upstream in `geocode()`.

### OrsRouteResult DTO

```java
public record OrsRouteResult(
    double distanceKm,
    double durationSeconds,
    List<double[]> coordinates,   // [[lat,lng], ...] — Leaflet order
    double fromLat, double fromLng,
    double toLat,   double toLng
) {}
```

### Transport profile mapping

```java
case BIKE     -> "cycling-regular";
case HIKE     -> "foot-hiking";
case RUNNING  -> "foot-walking";
case VACATION -> "foot-walking";  // was changed from driving-car at some point
```

Note: VACATION still maps to foot-walking — this may be intentional or a leftover.

---

## Frontend — How the Location Name Is Formed

### AddressInputComponent (`address-input.ts`)

Autocomplete call (lines 124–126):
```
https://api.openrouteservice.org/geocode/autocomplete
  ?api_key=<KEY>
  &text=<userInput>
  &size=5
```

**Missing parameters here too:**
- NO `boundary.country=AUT`
- NO `focus.point.*`

The result shown to the user is `f.properties?.label` — the full human-readable label string like:
`"Handelskai, Wien, Österreich"`

When the user selects a suggestion, `select(name)` emits the full label string via `valueChange`. This sets `tourForm.controls.from.value` to the full label string.

### tours.ts — Form submission

```typescript
const data = {
  from: v.from.trim(),   // full label string, e.g. "Handelskai, Wien, Österreich"
  to:   v.to.trim(),
  // ...
  distance:      0,        // placeholder — overwritten by backend ORS call
  estimatedTime: 0,        // placeholder
  routeGeometry: null,     // placeholder
};
```

The full label string is what the backend receives and passes to `geocode()`.

### tour-map.ts — Rendering

Reads `routeGeometry` (JSON string of `[[lat,lng],...]`), parses it, and draws a Leaflet polyline. Markers are placed at `coords[0]` (green, start) and `coords[coords.length - 1]` (red, end). If `routeGeometry` is null, falls back to Vienna center `[48.2082, 16.3738]`.

---

## Koordinaten-Konvertierung (Coordinate Conversion)

| Stage | Format | Notes |
|---|---|---|
| ORS geocode/search response | `[lng, lat]` | GeoJSON standard |
| `geocode()` return value | `[lng, lat]` | stored as-is from ORS |
| ORS directions body | `[[lng,lat],[lng,lat]]` | correct — ORS Directions expects `[lng,lat]` |
| ORS directions response geometry | `[lng, lat]` per point | GeoJSON standard |
| `parseRoute()` output | `[lat, lng]` per point | **swapped for Leaflet** |
| `OrsRouteResult.coordinates` | `[[lat,lng],...]` | correct for Leaflet |
| `routeGeometry` DB column | JSON string of `[[lat,lng],...]` | serialized from above |
| Frontend `tour-map.ts` | reads `[lat, lng]` | correct — matches what was stored |

The coordinate conversion itself appears consistent end-to-end. **The bug is in what coordinates `geocode()` returns for ambiguous/local place names.**

---

## Unterschiede Frontend vs. Backend (Differences Frontend vs. Backend)

| | Frontend (Autocomplete) | Backend (Geocode for route) |
|---|---|---|
| API endpoint | `/geocode/autocomplete` | `/geocode/search` |
| Parameters | `text`, `size=5` | `text`, `size=1` |
| Country filter | **none** | **none** |
| Focus point | **none** | **none** |
| Result used | `properties.label` (display text) | `geometry.coordinates` (actual coords) |

Both calls lack geographic context. The autocomplete may return the correct suggestion label by chance (because ORS autocomplete for "Handelskai Wien" likely ranks the Vienna one first), but the backend `geocode/search` call is making a separate independent request with `size=1` — it gets the top global result, which may differ.

---

## Warum funktioniert Wien↔Graz, nicht aber Handelskai?

ORS `/geocode/search` ohne Kontext-Parameter wählt das **globale Top-1-Ergebnis** nach ORS-internem Ranking.

| Eingabe | Warum es (nicht) funktioniert |
|---|---|
| "Wien" | Weltbekannte Hauptstadt → eindeutig, ORS findet Wien AT sicher |
| "Graz" | Zweitgrößte Stadt AT → eindeutig |
| "Handelskai" | Gibt es in mehreren Städten → ORS wählt globales Top-Ergebnis, evtl. nicht Wien |
| "Donauinsel" | Kleiner POI → ORS könnte auf `locality`-Ebene (Stadtmitte) zurückfallen |

**Konkrete Hypothese:** ORS liefert für `?text=Handelskai,Wien` ein Feature vom Layer `locality` (Wien als Ganzes) statt `street`/`venue` (der tatsächliche Handelskai). Das kann man prüfen indem man direkt im Browser aufruft:
```
https://api.openrouteservice.org/geocode/search?api_key=KEY&text=Handelskai%2C+Wien&size=3
```
Wenn `features[0].properties.layer` = `locality` → bestätigt.

## Hypotheses for Opus Analysis

1. **Missing `boundary.country=AUT` in `geocode()`**: ORS resolves "Handelskai" globally, top result may not be Vienna. Fix: add `&boundary.country=AUT` to the geocode URL.

2. **Missing `focus.point` in `geocode()`**: ORS has no proximity signal for Vienna. Fix: add `&focus.point.lat=48.2082&focus.point.lon=16.3738` (Vienna center) or use the user's selected autocomplete coordinates if available.

3. **Frontend passes label, not coordinates**: The autocomplete already resolved the correct feature and has its coordinates (`f.geometry.coordinates`), but the component discards them and only emits the label string. The backend then re-geocodes the label independently, which can produce a different result. Fix: emit both the label AND the coordinates from `AddressInputComponent`, store the pre-resolved coordinates, and skip the `geocode()` call in `OrsService` when coordinates are already known.

4. **Autocomplete label format**: The label "Handelskai, Wien, Österreich" works for Wien↔Graz because cities are globally unique. For sub-city locations the label contains enough context ("Wien, Österreich") — so hypothesis 1/2 (focus point or country filter) is the most likely minimal fix.

## Empfohlener Fix (minimal, sicher)

**Option A — Schnellster Fix:** `boundary.country=AUT` + `focus.point` in `geocode()` hinzufügen.

In `OrsService.java`, Zeile 38–39 ändern von:
```java
String url = baseUrl + "/geocode/search?api_key=" + apiKey
        + "&text=" + encoded + "&size=1";
```
zu:
```java
String url = baseUrl + "/geocode/search?api_key=" + apiKey
        + "&text=" + encoded + "&size=1"
        + "&boundary.country=AUT"
        + "&focus.point.lat=48.2082&focus.point.lon=16.3738";
```

**Option B — Sauberer Fix:** Koordinaten bereits im Frontend aus dem Autocomplete-Ergebnis nehmen und direkt an den Backend-Directions-Call übergeben (kein zweites `geocode()` nötig). Erfordert Änderungen in `address-input.ts`, `tours.ts`, `Tour.java` und `OrsService.getRoute()`.

→ **Empfehlung: Option A** für jetzt, da minimal und risikoarm. Option B als spätere Verbesserung.

---

## Key Files

| File | Role |
|---|---|
| `backend/.../service/OrsService.java` | geocode() and getRoute() — root cause location |
| `backend/.../service/TourService.java` | calls enrichWithOrsData() |
| `backend/.../dto/tour/OrsRouteResult.java` | DTO carrying result |
| `frontend/.../shared/address-input/address-input.ts` | autocomplete — also lacks country/focus params |
| `frontend/.../shared/tour-map/tour-map.ts` | map rendering — reads routeGeometry |
| `frontend/.../tours/tours.ts` | form submission — sends label strings as from/to |
| `backend/.../resources/application.properties` | ORS base URL: `https://api.openrouteservice.org` |
