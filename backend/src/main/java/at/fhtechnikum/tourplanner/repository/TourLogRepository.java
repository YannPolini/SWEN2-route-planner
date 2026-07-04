package at.fhtechnikum.tourplanner.repository;


import at.fhtechnikum.tourplanner.model.TourLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourLogRepository extends JpaRepository<TourLog, String> {
    void deleteByTourID(String tourID);

    List<TourLog> findByTourID(String tourID);

    List<TourLog> findByOwnerUserId(Long ownerUserId);

    List<TourLog> findByTourIDAndOwnerUserId(String tourID, Long ownerUserId);
}
