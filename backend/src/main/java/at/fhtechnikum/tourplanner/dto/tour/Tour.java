package at.fhtechnikum.tourplanner.dto.tour;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tour")
@Getter
@Setter
@NoArgsConstructor
public class Tour {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    @Min(value = 3, message = "Name should be at least 3 characters long")
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
    @DecimalMin(value = "0.1", message = "Value must be at least 0.1")  //inclusive 0.1
    private double distance;

    @Column(nullable = false)
    @Min(value=1, message = "Estimated Time must at least be 1 Minutes")
    private double estimatedTime;

    @Column(nullable = false)
    private String routeImagePath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}