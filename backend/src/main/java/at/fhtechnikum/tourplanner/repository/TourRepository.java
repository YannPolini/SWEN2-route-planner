package at.fhtechnikum.tourplanner.repository;


import at.fhtechnikum.tourplanner.model.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourRepository extends JpaRepository<Tour, String> {
}
