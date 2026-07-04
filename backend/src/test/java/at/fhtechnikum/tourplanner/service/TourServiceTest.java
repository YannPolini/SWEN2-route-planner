package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.dto.tour.OrsRouteResult;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Validator;

import at.fhtechnikum.tourplanner.model.TransportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TourServiceTest {

    @Mock
    private TourRepository repository;

    @Mock
    private TourLogRepository tourLogRepository;

    @Mock
    private OrsService orsService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TourService tourService;

    @Test
    void getAllTours_returnsToursFromRepository() {
        Tour tour = new Tour();
        Tour tour2 = new Tour();

        when(repository.findAll()).thenReturn(List.of(tour, tour2));
        List<Tour> result = tourService.getAllTours();
        assertThat(result).containsExactly(tour, tour2);
        verify(repository).findAll();
    }

    @Test
    void getTourById_returnsTheTour() {
        Tour tour = new Tour();
        when(repository.findById("1")).thenReturn(Optional.of(tour));
        Optional<Tour> result = tourService.getTourById("1");
        assertThat(result).contains(tour);
        verify(repository).findById("1");
    }

    @Test
    void getToursForUser_returnsOnlyOwnedTours() {
        AppUser owner = user(42L, null);
        Tour tour = new Tour();

        when(repository.findByOwnerUserId(42L)).thenReturn(List.of(tour));

        List<Tour> result = tourService.getToursForUser(owner);

        assertThat(result).containsExactly(tour);
        verify(repository).findByOwnerUserId(42L);
    }

    @Test
    void getToursForUser_setsAverageDifficultyFromOwnedLogs() {
        AppUser owner = user(42L, null);
        Tour tour = new Tour();
        tour.setId("tour-1");
        TourLog easyLog = logWithDifficulty(2);
        TourLog hardLog = logWithDifficulty(5);

        when(repository.findByOwnerUserId(42L)).thenReturn(List.of(tour));
        when(tourLogRepository.findByTourIDAndOwnerUserId("tour-1", 42L))
                .thenReturn(List.of(easyLog, hardLog));

        List<Tour> result = tourService.getToursForUser(owner);

        assertThat(result.get(0).getDifficulty()).isEqualTo(3.5);
    }

    @Test
    void createTour_returnsTheTour() {
        Tour tour = new Tour();
        tourService.createTour(tour);

        when(repository.findAll()).thenReturn(List.of(tour));
        List<Tour> result = tourService.getAllTours();
        assertThat(result).containsExactly(tour);
        verify(repository).findAll();
    }

    @Test
    void createTour_savesTour() {
        Tour tour = new Tour();

        tourService.createTour(tour);

        verify(repository).save(tour);
    }

    @Test
    void createTour_withOwner_setsServerSideOwnerFields() {
        AppUser owner = user(42L, "Demo User");
        Tour tour = new Tour();
        tour.setOwnerUserId(999L);
        tour.setCreatorName("Spoofed User");

        tourService.createTour(tour, owner);

        assertThat(tour.getOwnerUserId()).isEqualTo(42L);
        assertThat(tour.getCreatorName()).isEqualTo("Demo User");
        verify(repository).save(tour);
    }

    @Test
    void createTour_withOwner_rejectsDuplicateTourId() {
        AppUser owner = user(42L, null);
        Tour tour = new Tour();
        tour.setId("tour-1");

        when(repository.existsById("tour-1")).thenReturn(true);

        assertThatThrownBy(() -> tourService.createTour(tour, owner))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(repository, never()).save(any(Tour.class));
    }

    @Test
    void createTour_computesChildFriendlinessFromEnrichedRoute() throws Exception {
        Tour tour = new Tour();
        tour.setName("Prater Run");
        tour.setStartLocation("Praterstern");
        tour.setEndLocation("Lusthaus");
        tour.setTransportType(TransportType.RUNNING);
        tour.setStartLat(48.2182);
        tour.setStartLng(16.3920);
        tour.setEndLat(48.1927);
        tour.setEndLng(16.4396);
        tour.setChildFriendliness(5);

        when(orsService.getRoute(any(double[].class), any(double[].class), eq(TransportType.RUNNING)))
                .thenReturn(new OrsRouteResult(4.7, 34 * 60, List.of(new double[]{48.2, 16.4})));
        when(objectMapper.writeValueAsString(any())).thenReturn("[[48.2,16.4]]");

        tourService.createTour(tour);

        assertThat(tour.getDistance()).isEqualTo(4.7);
        assertThat(tour.getEstimatedTime()).isEqualTo(34 * 60);
        assertThat(tour.getChildFriendliness()).isEqualTo(3);
        verify(repository).save(tour);
    }

    @Test
    void deleteTour_returnsTheTourID_Successfully() {
        when(repository.existsById("1")).thenReturn(true);
        boolean result = tourService.deleteTour("1");
        assertThat(result).isTrue();
        verify(tourLogRepository).deleteByTourID("1");
        verify(repository).deleteById("1");
    }

    @Test
    void deleteTour_returnsTheTourID_Fails() {
        when(repository.existsById("1")).thenReturn(false);
        boolean result = tourService.deleteTour("1");
        assertThat(result).isFalse();
        verify(tourLogRepository, never()).deleteByTourID(anyString());
        verify(repository, never()).deleteById("1");
    }

    @Test
    void deleteTour_withOwner_rejectsOtherUsersTour() {
        AppUser owner = user(42L, null);
        Tour existing = new Tour();
        existing.setOwnerUserId(99L);

        when(repository.findById("tour-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tourService.deleteTour("tour-1", owner))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");

        verify(tourLogRepository, never()).deleteByTourID(anyString());
        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void updateTour_returnsTheTour_successfully() {
        Tour tour = new Tour();
        tour.setId("tour-1");

        when(repository.existsById("tour-1")).thenReturn(true);
        when(repository.save(tour)).thenReturn(tour);

        Optional<Tour> result = tourService.updateTour("tour-1", tour);
        assertThat(result).contains(tour);
        verify(repository).existsById("tour-1");
        verify(repository).save(tour);
    }

    @Test
    void updateTour_returnsTheTour_fails() {
        Tour tour = new Tour();
        tour.setId("tour-1");

        when(repository.existsById("tour-1")).thenReturn(false);

        assertThatThrownBy(() -> tourService.updateTour("tour-1", tour))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tour not found: tour-1");

        //prüft ob diese methode wirklich aufgeruden wurde
        verify(repository).existsById("tour-1");
        verify(repository, never()).save(any());
    }

    @Test
    void updateTour_withOwner_preservesServerSideOwnerFields() {
        AppUser owner = user(42L, "Demo User");
        Tour existing = new Tour();
        existing.setOwnerUserId(42L);
        Tour update = new Tour();
        update.setOwnerUserId(999L);
        update.setCreatorName("Spoofed User");

        when(repository.findById("tour-1")).thenReturn(Optional.of(existing));
        when(repository.save(update)).thenReturn(update);

        Optional<Tour> result = tourService.updateTour("tour-1", update, owner);

        assertThat(result).contains(update);
        assertThat(update.getId()).isEqualTo("tour-1");
        assertThat(update.getOwnerUserId()).isEqualTo(42L);
        assertThat(update.getCreatorName()).isEqualTo("Demo User");
        verify(repository).save(update);
    }

    @Test
    void updateTour_withOwner_rejectsOtherUsersTour() {
        AppUser owner = user(42L, null);
        Tour existing = new Tour();
        existing.setOwnerUserId(99L);

        when(repository.findById("tour-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tourService.updateTour("tour-1", new Tour(), owner))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");

        verify(repository, never()).save(any(Tour.class));
    }

    private AppUser user(Long id, String name) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        if (name != null) {
            when(user.getName()).thenReturn(name);
        }
        return user;
    }

    private TourLog logWithDifficulty(int difficulty) {
        TourLog log = new TourLog();
        log.setDifficulty(difficulty);
        return log;
    }
}
