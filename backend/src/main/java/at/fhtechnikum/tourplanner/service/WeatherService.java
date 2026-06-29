package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.dto.weather.WeatherForecastDayDto;
import at.fhtechnikum.tourplanner.dto.weather.WeatherForecastDto;
import at.fhtechnikum.tourplanner.exception.OrsServiceException;
import at.fhtechnikum.tourplanner.exception.WeatherServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private static final int FORECAST_DAYS = 3;

    @Value("${openweather.api.key:}")
    private String apiKey;

    @Value("${openweather.api.base-url:https://api.openweathermap.org}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrsService orsService;

    public WeatherService(OrsService orsService) {
        this.orsService = orsService;
    }

    public WeatherForecastDto getForecastForTour(Tour tour) {
        if (tour.getEndLat() != null && tour.getEndLng() != null) {
            return getForecastByCoordinates(tour.getId(), tour.getEndLat(), tour.getEndLng());
        }

        if (tour.getEndLocation() != null && !tour.getEndLocation().isBlank()) {
            return getForecastByResolvedLocation(tour.getId(), tour.getEndLocation());
        }

        throw new IllegalArgumentException("Tour has no usable destination for weather forecast.");
    }

    WeatherForecastDto mapForecastResponse(String tourId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String locationName = root.path("city").path("name").asText(null);
            JsonNode list = root.path("list");
            if (!list.isArray() || list.isEmpty()) {
                throw new WeatherServiceException("OpenWeather returned no forecast data.");
            }

            Map<LocalDate, List<ForecastEntry>> entriesByDate = new LinkedHashMap<>();
            for (JsonNode item : list) {
                ForecastEntry entry = toForecastEntry(item);
                entriesByDate.computeIfAbsent(entry.date(), ignored -> new ArrayList<>()).add(entry);
            }

            List<WeatherForecastDayDto> days = entriesByDate.entrySet().stream()
                    .limit(FORECAST_DAYS)
                    .map(entry -> toDayDto(entry.getKey(), entry.getValue()))
                    .toList();

            return new WeatherForecastDto(tourId, locationName, days);
        } catch (JsonProcessingException e) {
            throw new WeatherServiceException("OpenWeather forecast: invalid response", e);
        }
    }

    private WeatherForecastDto getForecastByCoordinates(String tourId, double lat, double lng) {
        String url = baseForecastUrl()
                + "&lat=" + lat
                + "&lon=" + lng;
        return requestForecast(tourId, url);
    }

    private WeatherForecastDto getForecastByLocation(String tourId, String location) {
        String url = baseForecastUrl()
                + "&q=" + URLEncoder.encode(location, StandardCharsets.UTF_8);
        return requestForecast(tourId, url);
    }

    private WeatherForecastDto getForecastByResolvedLocation(String tourId, String location) {
        try {
            double[] coords = orsService.geocode(location);
            return getForecastByCoordinates(tourId, coords[1], coords[0]);
        } catch (OrsServiceException e) {
            return getForecastByLocation(tourId, location);
        }
    }

    private String baseForecastUrl() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new WeatherServiceException("OpenWeather API key is not configured.");
        }

        return baseUrl + "/data/2.5/forecast"
                + "?appid=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&units=metric"
                + "&lang=de";
    }

    private WeatherForecastDto requestForecast(String tourId, String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new WeatherServiceException("OpenWeather forecast failed (HTTP " + response.statusCode() + ")");
        }

        return mapForecastResponse(tourId, response.body());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new WeatherServiceException("OpenWeather request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException("OpenWeather request interrupted", e);
        }
    }

    private ForecastEntry toForecastEntry(JsonNode item) {
        LocalDateTime dateTime = parseDateTime(item);
        JsonNode main = item.path("main");
        JsonNode weather = item.path("weather").isArray() && !item.path("weather").isEmpty()
                ? item.path("weather").get(0)
                : objectMapper.createObjectNode();
        double temperature = main.path("temp").asDouble();

        return new ForecastEntry(
                dateTime.toLocalDate(),
                dateTime.getHour(),
                temperature,
                main.path("temp_min").asDouble(temperature),
                main.path("temp_max").asDouble(temperature),
                weather.path("description").asText(""),
                weather.path("icon").asText(""),
                main.has("humidity") ? main.path("humidity").asInt() : null,
                item.path("wind").has("speed") ? item.path("wind").path("speed").asDouble() : null
        );
    }

    private LocalDateTime parseDateTime(JsonNode item) {
        String dtTxt = item.path("dt_txt").asText(null);
        if (dtTxt != null && !dtTxt.isBlank()) {
            return LocalDateTime.parse(dtTxt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        long epochSeconds = item.path("dt").asLong();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private WeatherForecastDayDto toDayDto(LocalDate date, List<ForecastEntry> entries) {
        double averageTemp = entries.stream().mapToDouble(ForecastEntry::temperature).average().orElse(0.0);
        double minTemp = entries.stream().mapToDouble(ForecastEntry::minTemperature).min().orElse(0.0);
        double maxTemp = entries.stream().mapToDouble(ForecastEntry::maxTemperature).max().orElse(0.0);
        Integer averageHumidity = entries.stream()
                .filter(entry -> entry.humidity() != null)
                .mapToInt(ForecastEntry::humidity)
                .average()
                .stream()
                .mapToObj(value -> (int) Math.round(value))
                .findFirst()
                .orElse(null);
        var averageWindValue = entries.stream()
                .filter(entry -> entry.windSpeed() != null)
                .mapToDouble(ForecastEntry::windSpeed)
                .average();
        Double averageWind = averageWindValue.isPresent()
                ? roundOneDecimal(averageWindValue.getAsDouble())
                : null;
        ForecastEntry displayEntry = entries.stream()
                .min(Comparator.comparingInt(entry -> Math.abs(entry.hour() - 12)))
                .orElse(entries.getFirst());

        return new WeatherForecastDayDto(
                date,
                roundOneDecimal(averageTemp),
                roundOneDecimal(minTemp),
                roundOneDecimal(maxTemp),
                displayEntry.description(),
                displayEntry.icon(),
                averageHumidity,
                averageWind
        );
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record ForecastEntry(
            LocalDate date,
            int hour,
            double temperature,
            double minTemperature,
            double maxTemperature,
            String description,
            String icon,
            Integer humidity,
            Double windSpeed
    ) {
    }
}
