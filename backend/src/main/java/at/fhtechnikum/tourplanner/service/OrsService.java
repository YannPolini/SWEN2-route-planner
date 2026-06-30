package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tour.AddressSuggestion;
import at.fhtechnikum.tourplanner.dto.tour.OrsRouteResult;
import at.fhtechnikum.tourplanner.model.TransportType;
import at.fhtechnikum.tourplanner.exception.OrsServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade over the OpenRouteService REST API. Hides the HTTP/JSON details and
 * exposes two operations to the business layer: geocoding and routing.
 * All failures are surfaced as {@link OrsServiceException}.
 */
@Service
public class OrsService {

    private static final Logger log = LoggerFactory.getLogger(OrsService.class);

    @Value("${ors.api.key}")
    private String apiKey;

    @Value("${ors.api.base-url:https://api.heigit.org}")
    private String baseUrl;

    // Geocoding bias toward Vienna (does not restrict results to a region).
    @Value("${ors.geocode.focus-lat:48.2082}")
    private double focusLat;

    @Value("${ors.geocode.focus-lon:16.3738}")
    private double focusLon;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Geocodes a place name to [lng, lat]. Fallback only — the primary flow
    // routes from coordinates the client already picked in autocomplete.
    public double[] geocode(String placeName) {
        String url = baseUrl + "/pelias/v1/search?api_key=" + apiKey
                + "&text=" + URLEncoder.encode(placeName, StandardCharsets.UTF_8)
                + "&size=1"
                + "&focus.point.lat=" + focusLat + "&focus.point.lon=" + focusLon;

        log.info("ORS geocode (fallback) for: '{}'", placeName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new OrsServiceException(
                    "ORS geocoding failed for '" + placeName + "' (HTTP " + response.statusCode() + ")");
        }

        try {
            JsonNode features = objectMapper.readTree(response.body()).path("features");
            if (features.isEmpty()) {
                throw new OrsServiceException("No geocoding result for: " + placeName);
            }
            JsonNode coords = features.get(0).path("geometry").path("coordinates");
            double lng = coords.get(0).asDouble();
            double lat = coords.get(1).asDouble();
            log.info("Geocoded '{}' to [{}, {}]", placeName, lng, lat);
            return new double[]{lng, lat}; // [lng, lat] — ORS order
        } catch (JsonProcessingException e) {
            throw new OrsServiceException("ORS geocoding: invalid response for '" + placeName + "'", e);
        }
    }

    // Address autocomplete. Proxied through the backend so the API key never
    // reaches the browser. Returns label + exact coordinates per suggestion.
    public List<AddressSuggestion> autocomplete(String text) {
        String url = baseUrl + "/pelias/v1/autocomplete?api_key=" + apiKey
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&size=5"
                + "&focus.point.lat=" + focusLat + "&focus.point.lon=" + focusLon;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new OrsServiceException("ORS autocomplete failed (HTTP " + response.statusCode() + ")");
        }

        try {
            JsonNode features = objectMapper.readTree(response.body()).path("features");
            List<AddressSuggestion> suggestions = new ArrayList<>();
            for (JsonNode feature : features) {
                String label = feature.path("properties").path("label").asText(null);
                JsonNode coords = feature.path("geometry").path("coordinates");
                if (label != null && coords.isArray() && coords.size() == 2) {
                    suggestions.add(new AddressSuggestion(
                            label, coords.get(1).asDouble(), coords.get(0).asDouble())); // [lng,lat] → lat,lng
                }
            }
            return suggestions;
        } catch (JsonProcessingException e) {
            throw new OrsServiceException("ORS autocomplete: invalid response", e);
        }
    }

    // Primary flow: route directly from coordinates picked in autocomplete. [lng, lat].
    public OrsRouteResult getRoute(double[] fromCoords, double[] toCoords, TransportType transportType) {
        return requestDirections(fromCoords, toCoords, toOrsProfile(transportType));
    }

    // Fallback flow: geocode free-text addresses, then route.
    public OrsRouteResult getRoute(String from, String to, TransportType transportType) {
        return requestDirections(geocode(from), geocode(to), toOrsProfile(transportType));
    }

    private OrsRouteResult requestDirections(double[] fromCoords, double[] toCoords, String profile) {
        String url  = baseUrl + "/openrouteservice/v2/directions/" + profile + "/geojson";
        String body = "{\"coordinates\":[["
                + fromCoords[0] + "," + fromCoords[1] + "],["
                + toCoords[0]   + "," + toCoords[1]   + "]]}";

        log.info("ORS directions [{},{}] -> [{},{}] ({})",
                fromCoords[0], fromCoords[1], toCoords[0], toCoords[1], profile);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            log.error("ORS directions HTTP {}: {}", response.statusCode(), response.body());
            throw new OrsServiceException("ORS directions failed (HTTP " + response.statusCode() + ")");
        }
        return parseRoute(response.body());
    }

    private OrsRouteResult parseRoute(String json) {
        try {
            JsonNode features = objectMapper.readTree(json).path("features");
            if (features.isEmpty()) {
                throw new OrsServiceException("ORS returned no route features");
            }

            JsonNode summary = features.get(0).path("properties").path("summary");
            double distanceKm      = summary.path("distance").asDouble() / 1000.0; // m → km
            double durationSeconds = summary.path("duration").asDouble();

            // ORS geometry is [lng, lat]; Leaflet needs [lat, lng]
            List<double[]> coords = new ArrayList<>();
            for (JsonNode point : features.get(0).path("geometry").path("coordinates")) {
                coords.add(new double[]{point.get(1).asDouble(), point.get(0).asDouble()});
            }

            log.info("ORS route: {} km, {} s, {} points",
                    Math.round(distanceKm * 100.0) / 100.0, (long) durationSeconds, coords.size());

            return new OrsRouteResult(distanceKm, durationSeconds, coords);
        } catch (JsonProcessingException e) {
            throw new OrsServiceException("ORS directions: invalid response", e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new OrsServiceException("ORS request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrsServiceException("ORS request interrupted", e);
        }
    }

    private String toOrsProfile(TransportType type) {
        return switch (type) {
            case BIKE     -> "cycling-regular";
            case HIKE     -> "foot-hiking";
            case RUNNING  -> "foot-walking";
            case VEHICLE  -> "driving-car";
        };
    }
}
