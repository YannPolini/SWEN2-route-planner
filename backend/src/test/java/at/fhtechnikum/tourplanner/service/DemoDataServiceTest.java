package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.demo.DemoSeedResponse;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourLogRepository tourLogRepository;

    private DemoDataService demoDataService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        demoDataService = new DemoDataService(tourRepository, tourLogRepository, objectMapper);
    }

    @Test
    void seedDemoData_createsBundledDemoToursAndLogsForCurrentUser() {
        AppUser owner = user(42L, "New User");

        when(tourRepository.findByOwnerUserId(42L)).thenReturn(List.of());

        DemoSeedResponse response = demoDataService.seedDemoData(owner);

        assertThat(response.tourCount()).isEqualTo(12);
        assertThat(response.logCount()).isEqualTo(41);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository, times(12)).save(tourCaptor.capture());
        List<Tour> copiedTours = tourCaptor.getAllValues();

        assertThat(copiedTours)
                .extracting(Tour::getId)
                .contains("demo-donauinsel-bike-user-42", "demo-rax-plateau-hike-user-42");
        assertThat(copiedTours)
                .allSatisfy(tour -> {
                    assertThat(tour.getOwnerUserId()).isEqualTo(42L);
                    assertThat(tour.getCreatorName()).isEqualTo("New User");
                });

        ArgumentCaptor<TourLog> logCaptor = ArgumentCaptor.forClass(TourLog.class);
        verify(tourLogRepository, times(41)).save(logCaptor.capture());
        List<TourLog> copiedLogs = logCaptor.getAllValues();

        assertThat(copiedLogs)
                .anySatisfy(log -> {
                    assertThat(log.getLogID()).isEqualTo("demo-donauinsel-bike-log-01-user-42");
                    assertThat(log.getTourID()).isEqualTo("demo-donauinsel-bike-user-42");
                });
        assertThat(copiedLogs)
                .allSatisfy(log -> {
                    assertThat(log.getOwnerUserId()).isEqualTo(42L);
                    assertThat(log.getCreatorName()).isEqualTo("New User");
                });
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
}
