package at.fhtechnikum.tourplanner.dto.weather;

import java.time.LocalDate;

public record WeatherForecastDayDto(
        LocalDate date,
        double temperature,
        double minTemperature,
        double maxTemperature,
        String description,
        String icon,
        Integer humidity,
        Double windSpeed
) {
}
