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
        List<TourLog> logs = repository.findAll();
        System.out.println("Logs aus DB: " + logs.size());
        return logs;
    }

    public List<TourLog> getTourLogsForUser(AppUser owner) {
        List<TourLog> logs = repository.findByOwnerUserId(owner.getId());
        System.out.println("Logs aus DB fuer User " + owner.getId() + ": " + logs.size());
        return logs;
    }

    //Long?
    public Optional<TourLog> getTourLogById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public void createTourLog(TourLog tourLog) {
        System.out.println("Creating a new tour log_service");
        requireExistingTour(tourLog.getTourID());
        repository.save(tourLog);
    }

    @Transactional
    public void createTourLog(TourLog tourLog, AppUser owner) {
        System.out.println("Creating a new owned tour log_service");
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
        //damit falls es nicht existiert nich ausversehen neues erstellen
        System.out.println("Updating a tour log_service");
        if (!repository.existsById(logID)) {
            throw new ResourceNotFoundException("Log not found: " + logID);
        }

        //log.setLogID(logID);
        requireExistingTour(log.getTourID());
        TourLog saved = repository.save(log);
        System.out.println("Saved log");

        /*
        public TourLog updateTourLog(Long logID, TourLog dto) {
            TourLog existing = repository.findById(logID)
            .orElseThrow(() -> new RuntimeException("Log not found"));

        existing.setDate(dto.getDate());
        existing.setTime(dto.getTime());
        existing.setComment(dto.getComment());
        existing.setDifficulty(dto.getDifficulty());
        existing.setTotalDistance(dto.getTotalDistance());
        existing.setTotalTime(dto.getTotalTime());
        existing.setRating(dto.getRating());
        existing.setTourID(dto.getTourID());
        existing.setCreatorName(dto.getCreatorName());

        return repository.save(existing);
        }
         */
        return Optional.of(saved);
    }

    @Transactional
    public Optional<TourLog> updateTourLog(String logID, TourLog log, AppUser owner) {
        System.out.println("Updating an owned tour log_service");
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
