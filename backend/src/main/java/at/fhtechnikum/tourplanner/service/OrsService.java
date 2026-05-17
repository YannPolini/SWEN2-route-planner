package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tour.OrsRouteResult;
import at.fhtechnikum.tourplanner.dto.tour.TransportType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrsService {

    private static final Logger log = LoggerFactory.getLogger(OrsService.class);

    @Value("${ors.api.key}")
    private String apiKey;

    @Value("${ors.api.base-url:https://api.openrouteservice.org}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Converts a place name to [longitude, latitude] (ORS coordinate order).
    public double[] geocode(String placeName) throws Exception {
        String encoded = URLEncoder.encode(placeName, StandardCharsets.UTF_8);
        String url = baseUrl + "/geocode/search?api_key=" + apiKey
                + "&text=" + encoded + "&size=1";

        log.info("ORS geocode: {}", placeName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("ORS geocode HTTP {}: {}", response.statusCode(), response.body());
            throw new RuntimeException(
                    "ORS Geocoding failed for '" + placeName + "' (HTTP " + response.statusCode() + ")");
        }

        JsonNode features = objectMapper.readTree(response.body()).path("features");
        if (features.isEmpty()) {
            throw new RuntimeException("No geocoding result for: " + placeName);
        }

        JsonNode coords = features.get(0).path("geometry").path("coordinates");
        double lng = coords.get(0).asDouble();
        double lat = coords.get(1).asDouble();
        log.info("Geocoded '{}' to [{}, {}]", placeName, lng, lat);
        return new double[]{lng, lat}; // [lng, lat] — ORS order
    }

    // Calls ORS Directions, returns distance/duration/geometry.
    public OrsRouteResult getRoute(String from, String to, TransportType transportType) throws Exception {
        String profile      = toOrsProfile(transportType);
        double[] fromCoords = geocode(from); // [lng, lat]
        double[] toCoords   = geocode(to);   // [lng, lat]

        String url  = baseUrl + "/v2/directions/" + profile + "/geojson";
        String body = "{"
                + "\"coordinates\":["
                + "[" + fromCoords[0] + "," + fromCoords[1] + "],"
                + "[" + toCoords[0]   + "," + toCoords[1]   + "]"
                + "]}";

        log.info("ORS directions {} -> {} ({})", from, to, profile);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("ORS directions HTTP {}: {}", response.statusCode(), response.body());
            throw new RuntimeException(
                    "ORS Directions failed '" + from + "' -> '" + to
                    + "' (HTTP " + response.statusCode() + ")");
        }

        return parseRoute(response.body(), fromCoords, toCoords);
    }

    private OrsRouteResult parseRoute(String json, double[] fromCoords, double[] toCoords)
            throws Exception {

        JsonNode features = objectMapper.readTree(json).path("features");
        if (features.isEmpty()) {
            throw new RuntimeException("ORS returned no route features");
        }

        JsonNode summary = features.get(0).path("properties").path("summary");
        double distanceKm      = summary.path("distance").asDouble() / 1000.0; // m → km
        double durationSeconds = summary.path("duration").asDouble();           // already seconds

        // ORS geometry is [lng, lat]; Leaflet needs [lat, lng]
        List<double[]> coords = new ArrayList<>();
        for (JsonNode point : features.get(0).path("geometry").path("coordinates")) {
            coords.add(new double[]{point.get(1).asDouble(), point.get(0).asDouble()});
        }

        log.info("ORS route: {} km, {} s, {} points",
                Math.round(distanceKm * 100.0) / 100.0, (long) durationSeconds, coords.size());

        // Store fromCoords/toCoords as [lat, lng] in the result
        return new OrsRouteResult(
                distanceKm, durationSeconds, coords,
                fromCoords[1], fromCoords[0],
                toCoords[1],   toCoords[0]);
    }

    private String toOrsProfile(TransportType type) {
        return switch (type) {
            case BIKE     -> "cycling-regular";
            case HIKE     -> "foot-hiking";
            case RUNNING  -> "foot-walking";
            case VACATION -> "foot-walking";
        };
    }
}
