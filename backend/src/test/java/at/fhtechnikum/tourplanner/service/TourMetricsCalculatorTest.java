package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TransportType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourMetricsCalculatorTest {

    @Test
    void shortHike_isVeryChildFriendly() {
        Tour tour = tour(TransportType.HIKE, 1.5, 25);

        int result = TourMetricsCalculator.calculateChildFriendliness(tour);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void runningRoute_getsIntensityPenalty() {
        Tour tour = tour(TransportType.RUNNING, 4.5, 45);

        int result = TourMetricsCalculator.calculateChildFriendliness(tour);

        assertThat(result).isEqualTo(3);
    }

    @Test
    void longBikeRoute_isNotChildFriendly() {
        Tour tour = tour(TransportType.BIKE, 22.0, 240);

        int result = TourMetricsCalculator.calculateChildFriendliness(tour);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void updateChildFriendliness_overwritesExistingValue() {
        Tour tour = tour(TransportType.HIKE, 10.0, 90);
        tour.setChildFriendliness(5);

        TourMetricsCalculator.updateChildFriendliness(tour);

        assertThat(tour.getChildFriendliness()).isEqualTo(2);
    }

    private Tour tour(TransportType type, double distanceKm, double durationMinutes) {
        Tour tour = new Tour();
        tour.setTransportType(type);
        tour.setDistance(distanceKm);
        tour.setEstimatedTime(durationMinutes * 60);
        return tour;
    }
}
