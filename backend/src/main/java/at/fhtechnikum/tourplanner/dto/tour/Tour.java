package at.fhtechnikum.tourplanner.dto.tour;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tour")
@Getter
@Setter
@NoArgsConstructor
public class Tour {

    @Id
    private String id;

    private String name;
    private String description;

    @JsonProperty("from")
    @Column(name = "start_location")
    private String startLocation;

    @JsonProperty("to")
    @Column(name = "end_location")
    private String endLocation;

    @Enumerated(EnumType.STRING)
    private TransportType transportType;

    private double distance;
    private double estimatedTime;
    private String routeImagePath;
    private LocalDateTime createdAt;

    // getter/setter
}