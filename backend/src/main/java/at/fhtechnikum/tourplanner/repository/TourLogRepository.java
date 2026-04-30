package at.fhtechnikum.tourplanner.repository;


import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourLogRepository extends JpaRepository<TourLog, Long> {
}
