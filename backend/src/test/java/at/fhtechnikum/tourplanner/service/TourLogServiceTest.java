package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TourLogServiceTest {
    @Mock
    private TourLogRepository repository;

    @InjectMocks
    private TourLogService tourLogService;

    /*  //Falls mal gebraucht wird
    private TourLog createTourLog(String id) {
        TourLog log = new TourLog();
        log.setLogID(id);
        log.setDate("2026-06-21");
        log.setTime("12:00");
        log.setComment("Nice tour");
        log.setDifficulty(3);
        log.setTotalDistance(10.5);
        log.setTotalTime(90);
        log.setRating(4);
        log.setTourID("tour-1");
        log.setCreatorName("Example");
        return log;
    }
     */

    @Test
    //Testet ob getAllTourLogs korrekt repository.findAll() verwendet und dessen Ergebnis zurückgibt
    void getAllTourLogs_returnsLogsFromRepository() {
        TourLog log1 = new TourLog();
        TourLog log2 = new TourLog();

        when(repository.findAll()).thenReturn(List.of(log1, log2));

        List<TourLog> result = tourLogService.getAllTourLogs();

        assertThat(result).containsExactly(log1, log2);
        verify(repository).findAll();
    }

    @Test
    void getTourLogById_returnsTheLog() {
        TourLog log = new TourLog();

        when(repository.findById("1")).thenReturn(Optional.of(log));

        Optional<TourLog> result = tourLogService.getTourLogById("1");

        assertThat(result).contains(log);
        verify(repository).findById("1");
    }

    @Test
    void createTourLog_savesTheLog() {
        TourLog log = new TourLog();
        //wieso hier kein when?
        tourLogService.createTourLog(log);

        verify(repository).save(log);
    }

    @Test
    void deleteTourLog_returnsTrue_whenLogExists() {
        when(repository.existsById("1")).thenReturn(true);

        boolean result = tourLogService.deleteTourLog("1");

        assertThat(result).isTrue();
        verify(repository).existsById("1");
        verify(repository).deleteById("1");
    }

    @Test
    void deleteTourLog_returnsFalse_whenLogDoesNotExist() {
        when(repository.existsById("1")).thenReturn(false);

        boolean result = tourLogService.deleteTourLog("1");

        assertThat(result).isFalse();
        verify(repository).existsById("1");
        verify(repository, never()).deleteById("1");
    }

    @Test
    void updateTourLog_returnsUpdatedLog_whenLogExists() {
        TourLog log = new TourLog();

        when(repository.existsById("1")).thenReturn(true);
        when(repository.save(log)).thenReturn(log);

        Optional<TourLog> result = tourLogService.updateTourLog("1", log);

        assertThat(result).contains(log);
        verify(repository).existsById("1");
        verify(repository).save(log);
    }

    @Test
    void updateTourLog_throwsException_whenLogDoesNotExist() {
        TourLog log = new TourLog();

        when(repository.existsById("1")).thenReturn(false);

        assertThatThrownBy(() -> tourLogService.updateTourLog("1", log))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Log not found: 1");

        verify(repository).existsById("1");
        verify(repository, never()).save(any(TourLog.class));
    }
}
