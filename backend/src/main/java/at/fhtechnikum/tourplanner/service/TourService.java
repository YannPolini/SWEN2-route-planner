package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Optional<Tour> getTourById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public void createTour(Tour tour) {
        System.out.println("Creating a new tour log_service");
        repository.save(tour);
    }

    @Transactional
    public boolean deleteTour(String id) {
        System.out.println("Service delete: "+id);
        if(!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<Tour> updateTour(String tourID, Tour tour) {
        //damit falls es nicht existiert nich ausversehen neues erstellen
        System.out.println("Updating a tour service");
        if (!repository.existsById(tourID)) {
            throw new RuntimeException("tour not found");
        }
        System.out.println("tour exists, updating");
        Tour saved = repository.save(tour);
        System.out.println("Saved Tour");
        return Optional.of(saved);
    }
}
