package at.fhtechnikum.tourplanner.dto.tourlog;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TourLog {

    @Id
    @Column(nullable = false)
    @JsonProperty("logID")
    private String logID;

    //maybe use localdate type?
    @Column(nullable = false)
    private String date;

    //time type?
    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private String comment;

    @Column(nullable = false)
    @Min(value = 0, message = "Difficulty should not be less than 0")
    @Max(value = 5, message = "Difficulty should not be more than 5")
    private int difficulty;

    @Column(nullable = false)
    @Min(value = 0, message = "Total distance should not be less than 0")
    private double totalDistance;

    @Column(nullable = false)
    @Min(value = 0, message = "Total time taken should not be less than 0")
    private double totalTime;

    @Column(nullable = false)
    @Min(value = 0, message = "Rating should not be less than 0")
    @Max(value = 5, message = "Rating should not be more than 5")
    private int rating;

    @Column(nullable = false)
    @JsonProperty("tourID")
    private String tourID;

    @Column(nullable = false)
    //@ManyToOne                //does not work with string, maybe make a user type?
    //@JoinColumn(name = "??")  //user DB does not exist yet
    private String creatorName;

}
