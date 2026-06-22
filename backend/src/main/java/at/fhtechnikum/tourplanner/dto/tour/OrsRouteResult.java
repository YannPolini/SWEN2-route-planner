package at.fhtechnikum.tourplanner.dto.tour;

import java.util.List;

/**
 * Result of one ORS /v2/directions call.
 *
 * distanceKm      — route length in km
 * durationSeconds — estimated travel time in seconds
 * coordinates     — [[lat,lng],...] pairs ready for a Leaflet polyline
 */
public record OrsRouteResult(
        double distanceKm,
        double durationSeconds,
        List<double[]> coordinates
) {}
