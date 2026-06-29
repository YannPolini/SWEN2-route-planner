package at.fhtechnikum.tourplanner.dto.weather;

import java.util.List;

public record WeatherForecastDto(
        String tourId,
        String locationName,
        List<WeatherForecastDayDto> days
) {
}
