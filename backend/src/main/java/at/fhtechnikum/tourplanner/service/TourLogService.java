package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TourLogService {

    private final TourLogRepository repository;

    public TourLogService(TourLogRepository repository) {
        this.repository = repository;
    }

    public List<TourLog> getAllTourLogs() {
        List<TourLog> logs = repository.findAll();
        System.out.println("Logs aus DB: " + logs.size());
        return logs;
    }

    public Optional<TourLog> getTourLogById(Long id) {
        return repository.findById(id);
    }

    public TourLog createTourLog(TourLog tourLog) {
        System.out.println("Creating a new tour log_service");
        return repository.save(tourLog);
    }

    public boolean deleteTourLog(Long id) {
        if(!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    public Optional<TourLog> updateTourLog(Long logID, TourLog log) {
        //damit falls es nicht existiert nich ausversehen neues erstellen
        System.out.println("Updating a tour log_service");
        if (!repository.existsById(logID)) {
            throw new RuntimeException("Log not found");
        }

        //log.setLogID(logID);
        TourLog saved = repository.save(log);
        System.out.println("Saved log: " + saved.getComment());

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
}