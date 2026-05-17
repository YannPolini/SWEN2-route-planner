package at.fhtechnikum.tourplanner.dto.tour;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tour")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Getter
@Setter
@NoArgsConstructor
public class Tour {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    @Size(min = 3, message = "Name should be at least 3 characters long")
    private String name;

    @Column(nullable = false)
    private String description;

    @JsonProperty("from")
    //@Column(name = "start_location") //is this needed?
    @Column(nullable = false)
    private String startLocation;

    @JsonProperty("to")
    //@Column(name = "end_location")
    @Column(nullable = false)
    private String endLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportType transportType;

    @Column(nullable = false)
    private double distance;

    @Column(nullable = false)
    private double estimatedTime;

    @Column(nullable = false)
    @Min(value = 0, message = "Child friendliness should not be less than 0")
    @Max(value = 5, message = "Child friendliness should not be more than 5")
    private int childFriendliness;

    @Column(nullable = false)
    private String routeImagePath;

    // ORS route polyline stored as JSON string: [[lat,lng],[lat,lng],...]
    @Column(columnDefinition = "TEXT")
    private String routeGeometry;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
