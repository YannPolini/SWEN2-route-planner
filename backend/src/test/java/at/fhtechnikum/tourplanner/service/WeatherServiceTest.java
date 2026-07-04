package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.weather.WeatherForecastDto;
import at.fhtechnikum.tourplanner.exception.WeatherServiceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WeatherServiceTest {

    private final WeatherService weatherService = new WeatherService(mock(OrsService.class));

    @Test
    void mapForecastResponse_returnsFiveDailyForecasts() {
        String json = """
                {
                  "city": { "name": "Vienna" },
                  "list": [
                    { "dt_txt": "2026-06-26 09:00:00", "main": { "temp": 20.0, "temp_min": 19.0, "temp_max": 21.0, "humidity": 60 }, "weather": [{ "description": "bewoelkt", "icon": "04d" }], "wind": { "speed": 2.5 } },
                    { "dt_txt": "2026-06-26 12:00:00", "main": { "temp": 22.0, "temp_min": 21.0, "temp_max": 24.0, "humidity": 50 }, "weather": [{ "description": "sonnig", "icon": "01d" }], "wind": { "speed": 3.0 } },
                    { "dt_txt": "2026-06-27 12:00:00", "main": { "temp": 18.0, "temp_min": 16.0, "temp_max": 20.0, "humidity": 65 }, "weather": [{ "description": "regen", "icon": "10d" }], "wind": { "speed": 4.0 } },
                    { "dt_txt": "2026-06-28 12:00:00", "main": { "temp": 24.0, "temp_min": 22.0, "temp_max": 26.0, "humidity": 45 }, "weather": [{ "description": "klar", "icon": "01d" }], "wind": { "speed": 2.0 } },
                    { "dt_txt": "2026-06-29 12:00:00", "main": { "temp": 25.0, "temp_min": 23.0, "temp_max": 27.0, "humidity": 40 }, "weather": [{ "description": "heiss", "icon": "02d" }], "wind": { "speed": 1.0 } },
                    { "dt_txt": "2026-06-30 12:00:00", "main": { "temp": 19.0, "temp_min": 18.0, "temp_max": 22.0, "humidity": 58 }, "weather": [{ "description": "windig", "icon": "03d" }], "wind": { "speed": 5.0 } }
                  ]
                }
                """;

        WeatherForecastDto result = weatherService.mapForecastResponse("tour-1", json);

        assertThat(result.tourId()).isEqualTo("tour-1");
        assertThat(result.locationName()).isEqualTo("Vienna");
        assertThat(result.days()).hasSize(5);
        assertThat(result.days().getFirst().date()).isEqualTo(LocalDate.of(2026, 6, 26));
        assertThat(result.days().getFirst().temperature()).isEqualTo(21.0);
        assertThat(result.days().getFirst().minTemperature()).isEqualTo(19.0);
        assertThat(result.days().getFirst().maxTemperature()).isEqualTo(24.0);
        assertThat(result.days().getFirst().description()).isEqualTo("sonnig");
        assertThat(result.days().getFirst().icon()).isEqualTo("01d");
        assertThat(result.days().getFirst().humidity()).isEqualTo(55);
        assertThat(result.days().getFirst().windSpeed()).isEqualTo(2.8);
    }

    @Test
    void mapForecastResponse_throwsForInvalidJson() {
        assertThatThrownBy(() -> weatherService.mapForecastResponse("tour-1", "{"))
                .isInstanceOf(WeatherServiceException.class)
                .hasMessageContaining("invalid response");
    }
}
