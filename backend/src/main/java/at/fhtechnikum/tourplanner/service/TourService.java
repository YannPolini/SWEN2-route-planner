package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tour.OrsRouteResult;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.exception.OrsServiceException;
import at.fhtechnikum.tourplanner.exception.ResourceNotFoundException;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TourService {

    private static final Logger log = LoggerFactory.getLogger(TourService.class);

    private final TourRepository repository;
    private final TourLogRepository tourLogRepository;
    private final OrsService     orsService;
    private final ObjectMapper   objectMapper;

    public TourService(TourRepository repository,
                       TourLogRepository tourLogRepository,
                       OrsService orsService,
                       ObjectMapper objectMapper) {
        this.repository   = repository;
        this.tourLogRepository = tourLogRepository;
        this.orsService   = orsService;
        this.objectMapper = objectMapper;
    }

    public List<Tour> getAllTours() {
        List<Tour> tours = repository.findAll();
        tours.forEach(this::updateTourMetrics);
        log.info("getAllTours: {} found", tours.size());
        return tours;
    }

    public Optional<Tour> getTourById(String id) {
        return repository.findById(id)
                .map(tour -> {
                    updateTourMetrics(tour);
                    return tour;
                });
    }

    public List<Tour> getToursForUser(AppUser owner) {
        List<Tour> tours = repository.findByOwnerUserId(owner.getId());
        tours.forEach(tour -> updateTourMetrics(tour, owner));
        log.info("getToursForUser: {} found for user {}", tours.size(), owner.getId());
        return tours;
    }

    public Optional<Tour> getTourById(String id, AppUser owner) {
        return repository.findByIdAndOwnerUserId(id, owner.getId())
                .map(tour -> {
                    updateTourMetrics(tour, owner);
                    return tour;
                });
    }

    @Transactional
    public void createTour(Tour tour) {
        log.info("createTour: {}", tour.getName());
        enrichWithOrsData(tour);
        TourMetricsCalculator.updateChildFriendliness(tour);
        repository.save(tour);
    }

    @Transactional
    public void createTour(Tour tour, AppUser owner) {
        log.info("createTour: {} for user {}", tour.getName(), owner.getId());
        requireNewTourId(tour);
        assignOwner(tour, owner);
        enrichWithOrsData(tour);
        TourMetricsCalculator.updateChildFriendliness(tour);
        repository.save(tour);
    }

    @Transactional
    public boolean deleteTour(String id) {
        log.info("deleteTour: {}", id);
        if (!repository.existsById(id)) return false;
        tourLogRepository.deleteByTourID(id);
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public boolean deleteTour(String id, AppUser owner) {
        log.info("deleteTour: {} for user {}", id, owner.getId());
        Tour existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + id));
        requireOwner(existing, owner);
        tourLogRepository.deleteByTourID(id);
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<Tour> updateTour(String tourId, Tour tour) {
        log.info("updateTour: {}", tourId);
        if (!repository.existsById(tourId)) {
            throw new ResourceNotFoundException("Tour not found: " + tourId);
        }
        enrichWithOrsData(tour);
        TourMetricsCalculator.updateChildFriendliness(tour);
        return Optional.of(repository.save(tour));
    }

    @Transactional
    public Optional<Tour> updateTour(String tourId, Tour tour, AppUser owner) {
        log.info("updateTour: {} for user {}", tourId, owner.getId());
        Tour existing = repository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));
        requireOwner(existing, owner);
        tour.setId(tourId);
        assignOwner(tour, owner);
        enrichWithOrsData(tour);
        TourMetricsCalculator.updateChildFriendliness(tour);
        return Optional.of(repository.save(tour));
    }

    private void updateTourMetrics(Tour tour) {
        TourMetricsCalculator.updateChildFriendliness(tour);
        tour.setDifficulty(calculateAverageDifficulty(tourLogRepository.findByTourID(tour.getId())));
    }

    private void updateTourMetrics(Tour tour, AppUser owner) {
        TourMetricsCalculator.updateChildFriendliness(tour);
        tour.setDifficulty(calculateAverageDifficulty(
                tourLogRepository.findByTourIDAndOwnerUserId(tour.getId(), owner.getId())));
    }

    private Double calculateAverageDifficulty(List<TourLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return null;
        }

        double average = logs.stream()
                .mapToInt(TourLog::getDifficulty)
                .average()
                .orElse(0);
        return Math.round(average * 10.0) / 10.0;
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
            OrsRouteResult result = hasCoordinates(tour)
                    // Primary: route from the exact points picked in autocomplete.
                    ? orsService.getRoute(
                            new double[]{tour.getStartLng(), tour.getStartLat()},
                            new double[]{tour.getEndLng(),   tour.getEndLat()},
                            tour.getTransportType())
                    // Fallback: geocode the free-text addresses, then route.
                    : orsService.getRoute(from, to, tour.getTransportType());

            tour.setDistance(Math.round(result.distanceKm() * 100.0) / 100.0);
            tour.setEstimatedTime(result.durationSeconds());

            String geometryJson = objectMapper.writeValueAsString(result.coordinates());
            tour.setRouteGeometry(geometryJson);

            log.info("ORS enrichment ok: {} km, {} s",
                    Math.round(result.distanceKm() * 100.0) / 100.0,
                    (long) result.durationSeconds());

        } catch (OrsServiceException e) {
            log.warn("ORS enrichment failed for '{}' -> '{}': {}", from, to, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("ORS enrichment: could not serialize route geometry: {}", e.getMessage());
        }
    }

    // True when both endpoints carry exact coordinates from the autocomplete,
    // so we can route directly instead of geocoding the address strings.
    private boolean hasCoordinates(Tour tour) {
        return tour.getStartLat() != null && tour.getStartLng() != null
                && tour.getEndLat() != null && tour.getEndLng() != null;
    }

    private void assignOwner(Tour tour, AppUser owner) {
        tour.setOwnerUserId(owner.getId());
        tour.setCreatorName(owner.getName());
    }

    private void requireNewTourId(Tour tour) {
        if (tour.getId() != null && repository.existsById(tour.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tour already exists: " + tour.getId());
        }
    }

    private void requireOwner(Tour tour, AppUser owner) {
        if (!Objects.equals(tour.getOwnerUserId(), owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tour belongs to another user.");
        }
    }
}
