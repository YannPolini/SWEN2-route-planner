package at.fhtechnikum.tourplanner.repository;


import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourLogRepository extends JpaRepository<TourLog, Long> {
    List<TourLog> findByTourID(String tourID);
}
