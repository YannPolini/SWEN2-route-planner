package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.exception.ResourceNotFoundException;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TourLogService {

    private final TourLogRepository repository;
    private final TourRepository tourRepository;

    public TourLogService(TourLogRepository repository, TourRepository tourRepository) {
        this.repository = repository;
        this.tourRepository = tourRepository;
    }

    public List<TourLog> getAllTourLogs() {
        return repository.findAll();
    }

    public List<TourLog> getTourLogsForUser(AppUser owner) {
        return repository.findByOwnerUserId(owner.getId());
    }

    public Optional<TourLog> getTourLogById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public void createTourLog(TourLog tourLog) {
        requireExistingTour(tourLog.getTourID());
        repository.save(tourLog);
    }

    @Transactional
    public void createTourLog(TourLog tourLog, AppUser owner) {
        requireExistingTour(tourLog.getTourID(), owner);
        assignOwner(tourLog, owner);
        repository.save(tourLog);
    }

    @Transactional
    public boolean deleteTourLog(String id) {
        if(!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public boolean deleteTourLog(String id, AppUser owner) {
        Optional<TourLog> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        requireOwner(existing.get(), owner);
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<TourLog> updateTourLog(String logID, TourLog log) {
        if (!repository.existsById(logID)) {
            throw new ResourceNotFoundException("Log not found: " + logID);
        }

        requireExistingTour(log.getTourID());
        TourLog saved = repository.save(log);
        return Optional.of(saved);
    }

    @Transactional
    public Optional<TourLog> updateTourLog(String logID, TourLog log, AppUser owner) {
        TourLog existing = repository.findById(logID)
                .orElseThrow(() -> new ResourceNotFoundException("Log not found: " + logID));

        requireOwner(existing, owner);
        requireExistingTour(log.getTourID(), owner);
        log.setLogID(logID);
        assignOwner(log, owner);

        return Optional.of(repository.save(log));
    }

    private void requireExistingTour(String tourID) {
        if (tourID == null || tourID.isBlank()) {
            throw new IllegalArgumentException("tourID is required");
        }
        if (!tourRepository.existsById(tourID)) {
            throw new IllegalArgumentException("Referenced tour does not exist: " + tourID);
        }
    }

    private void requireExistingTour(String tourID, AppUser owner) {
        if (tourID == null || tourID.isBlank()) {
            throw new IllegalArgumentException("tourID is required");
        }
        if (!tourRepository.existsByIdAndOwnerUserId(tourID, owner.getId())) {
            throw new IllegalArgumentException("Referenced tour does not exist: " + tourID);
        }
    }

    private void assignOwner(TourLog log, AppUser owner) {
        log.setOwnerUserId(owner.getId());
        log.setCreatorName(owner.getName());
    }

    private void requireOwner(TourLog log, AppUser owner) {
        if (!Objects.equals(log.getOwnerUserId(), owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Log belongs to another user.");
        }
    }
}
