package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.search.SearchResultDto;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Full-text search over tours and tour logs, including their computed
 * attributes (distance, time, child-friendliness). Read-only: it never
 * modifies data, it only filters what the repositories return.
 */
@Service
public class SearchService {

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;

    public SearchService(TourRepository tourRepository, TourLogRepository tourLogRepository) {
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
    }

    public SearchResultDto searchGlobal(String query) {
        return new SearchResultDto(searchTours(query), searchTourLogs(query));
    }

    public List<Tour> searchTours(String query) {
        String q = normalize(query);
        return tourRepository.findAll().stream()
                .filter(tour -> tourText(tour).contains(q))
                .toList();
    }

    public List<TourLog> searchTourLogs(String query) {
        String q = normalize(query);
        return tourLogRepository.findAll().stream()
                .filter(log -> logText(log).contains(q))
                .toList();
    }

    // All searchable + computed fields of a tour as one lowercase string.
    private String tourText(Tour tour) {
        return String.join(" ",
                nullSafe(tour.getName()),
                nullSafe(tour.getDescription()),
                nullSafe(tour.getStartLocation()),
                nullSafe(tour.getEndLocation()),
                tour.getTransportType() == null ? "" : tour.getTransportType().name(),
                String.valueOf(tour.getDistance()),
                String.valueOf(tour.getEstimatedTime()),
                String.valueOf(tour.getChildFriendliness())
        ).toLowerCase();
    }

    // All searchable + computed fields of a tour log as one lowercase string.
    private String logText(TourLog log) {
        return String.join(" ",
                nullSafe(log.getComment()),
                nullSafe(log.getCreatorName()),
                nullSafe(log.getDate()),
                nullSafe(log.getTime()),
                nullSafe(log.getTourID()),
                String.valueOf(log.getDifficulty()),
                String.valueOf(log.getRating()),
                String.valueOf(log.getTotalDistance()),
                String.valueOf(log.getTotalTime())
        ).toLowerCase();
    }

    private String normalize(String query) {
        return query == null ? "" : query.toLowerCase().strip();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
