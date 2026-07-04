package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.demo.DemoSeedResponse;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.model.TransportType;
import at.fhtechnikum.tourplanner.repository.AppUserRepository;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourLogRepository tourLogRepository;

    @InjectMocks
    private DemoDataService demoDataService;

    @Test
    void seedDemoData_copiesDemoToursAndLogsToCurrentUser() {
        AppUser owner = user(42L, "New User");
        AppUser demoUser = user(1L, null);
        Tour demoTour = demoTour();
        TourLog demoLog = demoLog();

        when(tourRepository.findByOwnerUserId(42L)).thenReturn(List.of());
        when(userRepository.findByEmail("demo@tourplanner.local")).thenReturn(Optional.of(demoUser));
        when(tourRepository.findByOwnerUserId(1L)).thenReturn(List.of(demoTour));
        when(tourLogRepository.findByOwnerUserId(1L)).thenReturn(List.of(demoLog));

        DemoSeedResponse response = demoDataService.seedDemoData(owner);

        assertThat(response.tourCount()).isEqualTo(1);
        assertThat(response.logCount()).isEqualTo(1);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());
        Tour copiedTour = tourCaptor.getValue();
        assertThat(copiedTour.getId()).isEqualTo("demo-tour-user-42");
        assertThat(copiedTour.getOwnerUserId()).isEqualTo(42L);
        assertThat(copiedTour.getCreatorName()).isEqualTo("New User");

        ArgumentCaptor<TourLog> logCaptor = ArgumentCaptor.forClass(TourLog.class);
        verify(tourLogRepository).save(logCaptor.capture());
        TourLog copiedLog = logCaptor.getValue();
        assertThat(copiedLog.getLogID()).isEqualTo("demo-log-user-42");
        assertThat(copiedLog.getTourID()).isEqualTo("demo-tour-user-42");
        assertThat(copiedLog.getOwnerUserId()).isEqualTo(42L);
        assertThat(copiedLog.getCreatorName()).isEqualTo("New User");
    }

    @Test
    void seedDemoData_rejectsAccountWithExistingTours() {
        AppUser owner = user(42L, null);
        when(tourRepository.findByOwnerUserId(42L)).thenReturn(List.of(new Tour()));

        assertThatThrownBy(() -> demoDataService.seedDemoData(owner))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(tourRepository, never()).save(any(Tour.class));
        verify(tourLogRepository, never()).save(any(TourLog.class));
    }

    private AppUser user(Long id, String name) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        if (name != null) {
            when(user.getName()).thenReturn(name);
        }
        return user;
    }

    private Tour demoTour() {
        Tour tour = new Tour();
        tour.setId("demo-tour");
        tour.setName("Demo Tour");
        tour.setDescription("A useful demo route.");
        tour.setStartLocation("Start");
        tour.setEndLocation("End");
        tour.setTransportType(TransportType.BIKE);
        tour.setStartLat(48.2);
        tour.setStartLng(16.3);
        tour.setEndLat(48.3);
        tour.setEndLng(16.4);
        tour.setDistance(12.4);
        tour.setEstimatedTime(3600);
        tour.setChildFriendliness(3);
        tour.setOwnerUserId(1L);
        tour.setCreatorName("Demo User");
        tour.setRouteImagePath("");
        tour.setRouteGeometry("[[48.2,16.3],[48.3,16.4]]");
        tour.setCreatedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        return tour;
    }

    private TourLog demoLog() {
        TourLog log = new TourLog();
        log.setLogID("demo-log");
        log.setDate("2026-07-01");
        log.setTime("08:00");
        log.setComment("Good demo log.");
        log.setDifficulty(2);
        log.setTotalDistance(12.4);
        log.setTotalTime(60);
        log.setRating(4);
        log.setTourID("demo-tour");
        log.setOwnerUserId(1L);
        log.setCreatorName("Demo User");
        return log;
    }
}
