package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tour.OrsRouteResult;
import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TourService {

    private static final Logger log = LoggerFactory.getLogger(TourService.class);

    private final TourRepository repository;
    private final OrsService     orsService;
    private final ObjectMapper   objectMapper;

    public TourService(TourRepository repository,
                       OrsService orsService,
                       ObjectMapper objectMapper) {
        this.repository   = repository;
        this.orsService   = orsService;
        this.objectMapper = objectMapper;
    }

    public List<Tour> getAllTours() {
        List<Tour> tours = repository.findAll();
        log.info("getAllTours: {} found", tours.size());
        return tours;
    }

    public Optional<Tour> getTourById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public void createTour(Tour tour) {
        log.info("createTour: {}", tour.getName());
        enrichWithOrsData(tour);
        repository.save(tour);
    }

    @Transactional
    public boolean deleteTour(String id) {
        log.info("deleteTour: {}", id);
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<Tour> updateTour(String tourId, Tour tour) {
        log.info("updateTour: {}", tourId);
        if (!repository.existsById(tourId)) {
            throw new RuntimeException("Tour not found: " + tourId);
        }
        enrichWithOrsData(tour);
        return Optional.of(repository.save(tour));
    }

    // Fills distance, estimatedTime and routeGeometry before every DB save.
    // On failure: logs a warning, keeps existing/zero values, save still proceeds.
    private void enrichWithOrsData(Tour tour) {
        String from = tour.getStartLocation();
        String to   = tour.getEndLocation();

        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            log.warn("enrichWithOrsData: skipping, missing start/end location");
            return;
        }

        try {
            OrsRouteResult result = orsService.getRoute(from, to, tour.getTransportType());

            tour.setDistance(Math.round(result.distanceKm() * 100.0) / 100.0);
            tour.setEstimatedTime(result.durationSeconds());

            String geometryJson = objectMapper.writeValueAsString(result.coordinates());
            tour.setRouteGeometry(geometryJson);

            log.info("ORS enrichment ok: {} km, {} s",
                    Math.round(result.distanceKm() * 100.0) / 100.0,
                    (long) result.durationSeconds());

        } catch (JsonProcessingException e) {
            log.warn("ORS enrichment: JSON error: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("ORS enrichment failed for '{}' -> '{}': {}", from, to, e.getMessage());
        }
    }
}
