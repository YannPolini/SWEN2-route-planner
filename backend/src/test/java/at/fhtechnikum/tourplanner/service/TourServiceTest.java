package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.dto.tour.OrsRouteResult;
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

    /*  //Falls irgendwann ein bsp gebraucht wird (zb ors enriching testing
    private Tour createTour(String id){
        Tour tour = new Tour();
        tour.setId(id);
        tour.setName("Vienna City Walk");
        tour.setDescription("Short test tour through Vienna");
        tour.setStartLocation("Stephansplatz, Vienna");
        tour.setEndLocation("Karlsplatz, Vienna");
        tour.setTransportType(TransportType.HIKE);
        tour.setDistance(2.4);
        tour.setEstimatedTime(1800);
        tour.setChildFriendliness(4);
        tour.setRouteImagePath("/images/routes/tour-1.png");
        tour.setRouteGeometry("[[48.2082,16.3738],[48.2000,16.3700]]");
        tour.setCreatedAt(LocalDateTime.of(2026, 6, 21, 12, 0));

        return tour;
    }
     */

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
}
