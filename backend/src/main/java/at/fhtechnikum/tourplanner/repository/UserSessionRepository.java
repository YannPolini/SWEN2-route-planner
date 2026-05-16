package at.fhtechnikum.tourplanner.repository;

import at.fhtechnikum.tourplanner.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {
}
