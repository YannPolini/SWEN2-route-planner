package at.fhtechnikum.tourplanner.dto.tour;

import java.util.List;

/**
 * Result of one ORS /v2/directions call.
 *
 * distanceKm      — route length in km
 * durationSeconds — estimated travel time in seconds
 * coordinates     — [[lat,lng],...] pairs ready for Leaflet polyline
 * fromLat/fromLng — geocoded start point
 * toLat/toLng     — geocoded end point
 */
public record OrsRouteResult(
        double distanceKm,
        double durationSeconds,
        List<double[]> coordinates,
        double fromLat,
        double fromLng,
        double toLat,
        double toLng
) {}
