package at.fhtechnikum.tourplanner.repository;


import at.fhtechnikum.tourplanner.dto.tour.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
}
