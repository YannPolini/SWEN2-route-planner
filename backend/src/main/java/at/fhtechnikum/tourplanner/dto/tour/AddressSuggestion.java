package at.fhtechnikum.tourplanner.dto.tour;

/** One autocomplete suggestion: display label plus exact coordinates. */
public record AddressSuggestion(String label, double lat, double lng) {}
