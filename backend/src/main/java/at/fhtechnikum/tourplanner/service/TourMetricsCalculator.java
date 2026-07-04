package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TransportType;

public final class TourMetricsCalculator {

    private TourMetricsCalculator() {
    }

    public static void updateChildFriendliness(Tour tour) {
        if (tour == null) {
            return;
        }
        tour.setChildFriendliness(calculateChildFriendliness(tour));
    }

    public static int calculateChildFriendliness(Tour tour) {
        if (tour == null) {
            return 0;
        }

        int score = 5;
        double distanceKm = Math.max(0, tour.getDistance());
        double durationMinutes = Math.max(0, tour.getEstimatedTime()) / 60.0;

        if (distanceKm > 15) {
            score -= 3;
        } else if (distanceKm > 8) {
            score -= 2;
        } else if (distanceKm > 4) {
            score -= 1;
        }

        if (durationMinutes > 180) {
            score -= 2;
        } else if (durationMinutes > 60) {
            score -= 1;
        }

        TransportType type = tour.getTransportType();
        if (type == TransportType.RUNNING) {
            score -= 1;
        } else if (type == TransportType.BIKE && distanceKm > 8) {
            score -= 1;
        }

        return Math.max(0, Math.min(5, score));
    }
}
