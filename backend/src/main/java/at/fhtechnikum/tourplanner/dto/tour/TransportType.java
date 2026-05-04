package at.fhtechnikum.tourplanner.dto.tour;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

//Damit angular auch versteht
public enum TransportType {
    BIKE("bike", "Bike"),
    HIKE("hike", "Hike"),
    RUNNING("running", "Running"),
    VACATION("vacation", "Vacation");

    private final String value;
    @Getter
    private final String label;

    TransportType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransportType fromValue(String value) {
        for (TransportType type : TransportType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown transport type: " + value);
    }
}