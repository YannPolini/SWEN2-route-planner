package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TourService {

    private final TourRepository repository;

    public TourService(TourRepository repository) {
        this.repository = repository;
    }

    public List<Tour> getAllTours() {
        List<Tour> tours = repository.findAll();
        System.out.println("getting all tours_service");
        System.out.println("Tours aus DB: " + tours.size());
        return tours;
    }

    public Optional<Tour> getTourById(Long id) {
        return repository.findById(id);
    }

    public Tour createTour(Tour tour) {
        System.out.println("Creating a new tour log_service");
        return repository.save(tour);
    }

    public boolean deleteTour(Long id) {
        if(!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    public Optional<Tour> updateTour(Long logID, Tour tour) {
        //damit falls es nicht existiert nich ausversehen neues erstellen
        System.out.println("Updating a tour service");
        if (!repository.existsById(logID)) {
            throw new RuntimeException("tour not found");
        }

        //log.setLogID(logID);
        Tour saved = repository.save(tour);
        System.out.println("Saved Tour: " + saved.getDescription());

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