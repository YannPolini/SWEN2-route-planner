package at.fhtechnikum.tourplanner.dto.tourlog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Entity
public class TourLog {

    @Id
    @Column(nullable = false)
    @JsonProperty("logID")
    private Long logID;

    @Column(nullable = false)
    private String date;

    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private String comment;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private double totalDistance;

    @Column(nullable = false)
    private double totalTime;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    @JsonProperty("tourID")
    private String tourID;

    @Column(nullable = false)
    private String creatorName;

}
