package at.fhtechnikum.tourplanner.dto.search;

import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;

import java.util.List;

/** Full-text search result: matching tours and tour logs. */
public record SearchResultDto(List<Tour> tours, List<TourLog> tourLogs) {}
