package at.fhtechnikum.tourplanner.repository;


import at.fhtechnikum.tourplanner.model.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, String> {
    List<Tour> findByOwnerUserId(Long ownerUserId);

    Optional<Tour> findByIdAndOwnerUserId(String id, Long ownerUserId);

    boolean existsByIdAndOwnerUserId(String id, Long ownerUserId);
}
