package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.exception.ResourceNotFoundException;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TourLogService {

    private static final Logger log = LoggerFactory.getLogger(TourLogService.class);

    private final TourLogRepository repository;
    private final TourRepository tourRepository;

    public TourLogService(TourLogRepository repository, TourRepository tourRepository) {
        this.repository = repository;
        this.tourRepository = tourRepository;
    }

    public List<TourLog> getAllTourLogs() {
        List<TourLog> tourLogs = repository.findAll();
        log.info("getAllTours: {} found", tourLogs.size());
        return tourLogs;
    }

    public List<TourLog> getTourLogsForUser(AppUser owner) {
        log.info("getTourLogUser: {} found", owner.getId());
        return repository.findByOwnerUserId(owner.getId());
    }

    public Optional<TourLog> getTourLogById(String id) {
        log.info("getTourLogById: {} found", id);
        return repository.findById(id);
    }

    @Transactional
    public void createTourLog(TourLog tourLog) {
        log.info("createTourLog: {} found", tourLog);
        requireExistingTour(tourLog.getTourID());
        repository.save(tourLog);
    }

    @Transactional
    public void createTourLog(TourLog tourLog, AppUser owner) {
        log.info("createTourLog: {} found", tourLog);
        requireExistingTour(tourLog.getTourID(), owner);
        assignOwner(tourLog, owner);
        repository.save(tourLog);
    }

    @Transactional
    public boolean deleteTourLog(String id) {
        log.info("deleteTourLog: {} found", id);
        if(!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public boolean deleteTourLog(String id, AppUser owner) {
        log.info("deleteTourLog: {} found", id);
        Optional<TourLog> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        requireOwner(existing.get(), owner);
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<TourLog> updateTourLog(String logID, TourLog tourLog) {
        log.info("updateTourLog: {} found", logID);
        if (!repository.existsById(logID)) {
            throw new ResourceNotFoundException("Log not found: " + logID);
        }

        requireExistingTour(tourLog.getTourID());
        TourLog saved = repository.save(tourLog);
        return Optional.of(saved);
    }

    @Transactional
    public Optional<TourLog> updateTourLog(String logID, TourLog tourLog, AppUser owner) {
        log.info("updateTourLog: {} found", logID);
        TourLog existing = repository.findById(logID)
                .orElseThrow(() -> new ResourceNotFoundException("Log not found: " + logID));

        requireOwner(existing, owner);
        requireExistingTour(tourLog.getTourID(), owner);
        tourLog.setLogID(logID);
        assignOwner(tourLog, owner);

        return Optional.of(repository.save(tourLog));
    }

    private void requireExistingTour(String tourID) {
        log.info("checking if tour exists: {}", tourID);
        if (tourID == null || tourID.isBlank()) {
            throw new IllegalArgumentException("tourID is required");
        }
        if (!tourRepository.existsById(tourID)) {
            throw new IllegalArgumentException("Referenced tour does not exist: " + tourID);
        }
    }

    private void requireExistingTour(String tourID, AppUser owner) {
        log.info("checking if tour exists: {} with owner", tourID);
        if (tourID == null || tourID.isBlank()) {
            throw new IllegalArgumentException("tourID is required");
        }
        if (!tourRepository.existsByIdAndOwnerUserId(tourID, owner.getId())) {
            throw new IllegalArgumentException("Referenced tour does not exist: " + tourID);
        }
    }

    private void assignOwner(TourLog tourLog, AppUser owner) {
        log.info("checking if tour exists: {}", tourLog.getTourID());
        tourLog.setOwnerUserId(owner.getId());
        tourLog.setCreatorName(owner.getName());
    }

    private void requireOwner(TourLog tourLog, AppUser owner) {
        log.info("checking if tour exists: {}", tourLog.getTourID());
        if (!Objects.equals(tourLog.getOwnerUserId(), owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Log belongs to another user.");
        }
    }
}
