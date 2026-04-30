package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public TourLog getTourLogById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("TourLog nicht gefunden"));
    }

    public TourLog createTourLog(TourLog tourLog) {
        return repository.save(tourLog);
    }

    public void deleteTourLog(Long id) {
        repository.deleteById(id);
    }
}