package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.importexport.ImportResultDto;
import at.fhtechnikum.tourplanner.dto.tour.TransportType;
import at.fhtechnikum.tourplanner.dto.tourlog.TourLog;
import at.fhtechnikum.tourplanner.dto.tour.Tour;
import at.fhtechnikum.tourplanner.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

@Service
public class ImportExportService {

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;

    public ImportExportService(TourRepository tourRepository, TourLogRepository tourLogRepository) {
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
    }

     //Importiert aus einer CSV-Datei.
    @Transactional
    public ImportResultDto importTours(MultipartFile file) {
        System.out.println("importing");
        validateFile(file);

        int importedRows = 0;
        int failedRows = 0;
        List<String> errors = new ArrayList<>();

        try (
                Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)
        ) {
            System.out.println("trying");
            for (CSVRecord record : parser) {
                try {
                    Tour tour = mapCsvRecordToTour(record);

                    // Save/update tour first
                    tourRepository.save(tour);

                    // Then save log if the row contains log data
                    TourLog log = mapCsvRecordToTourLog(record);

                    if (log != null) {
                        tourLogRepository.save(log);
                    }

                    importedRows++;
                    System.out.println("imported rows: " + importedRows);

                } catch (Exception rowException) {
                    failedRows++;
                    String message = "Row " + record.getRecordNumber() + ": " + rowException.getMessage();
                    errors.add(message);
                    System.err.println(message);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading file " + file.getOriginalFilename());
            throw new RuntimeException("Could not read uploaded CSV file", e);
        }

        System.out.println("Done importing");
        return new ImportResultDto(importedRows, failedRows, errors);
    }

     //Exportiert alle Tours und Tourlogs als CSV-Datei.
    @Transactional(readOnly = true)
    public byte[] exportAsCsv() {
        List<Tour> tours = tourRepository.findAll();

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                CSVPrinter csv = new CSVPrinter(
                        writer,
                        CSVFormat.DEFAULT.builder()
                                .setHeader(
                                        "tour_id","tour_name","from","to","transport_type","distance","estimated_time","child_friendliness",
                                        "log_id","log_date","log_time","log_comment","log_difficulty","log_total_distance","log_total_time","log_rating","log_creator"
                                )
                                .build()
                )
        ) {

            for (Tour tour : tours) {
                List<TourLog> logs = tourLogRepository.findByTourID(tour.getId());

                // keep tours without logs in export
                if (logs.isEmpty()) {
                    csv.printRecord(
                            tour.getId(), tour.getName(), tour.getStartLocation(), tour.getEndLocation(),
                            tour.getTransportType(), tour.getDistance(), tour.getEstimatedTime(), tour.getChildFriendliness(),
                            "", "", "", "", "", "", "", "", ""
                    );
                    continue;
                }

                for (TourLog log : logs) {
                    csv.printRecord(
                            tour.getId(), tour.getName(), tour.getStartLocation(), tour.getEndLocation(),
                            tour.getTransportType(), tour.getDistance(), tour.getEstimatedTime(), tour.getChildFriendliness(),
                            log.getLogID(), log.getDate(), log.getTime(), log.getComment(), log.getDifficulty(),
                            log.getTotalDistance(), log.getTotalTime(), log.getRating(), log.getCreatorName()
                    );
                }
            }

            csv.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not export tours + logs as CSV", e);
        }
    }
    //
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are allowed");
        }
    }

    private Tour mapCsvRecordToTour(CSVRecord record) {
        Tour tour = new Tour();

        tour.setId(getRequiredValue(record, "tour_id"));
        tour.setName(getRequiredValue(record, "tour_name"));
        // imported Tour no desc -> use a fallback:
        tour.setDescription(getOptionalValue(record, "description"));
        if (tour.getDescription().isBlank()) {
            tour.setDescription("Imported tour");
        }
        tour.setStartLocation(getRequiredValue(record, "from"));
        tour.setEndLocation(getRequiredValue(record, "to"));
        tour.setTransportType(TransportType.valueOf(getRequiredValue(record, "transport_type")));
        tour.setDistance(parseDouble(getRequiredValue(record, "distance")));
        tour.setEstimatedTime(parseDouble(getRequiredValue(record, "estimated_time")));
        tour.setChildFriendliness(parseInt(getRequiredValue(record, "child_friendliness")));
        //Export has no route_image_path in the header
        tour.setRouteImagePath(getOptionalValue(record, "route_image_path"));
        //Export has no route_geometry column
        tour.setRouteGeometry(getOptionalValue(record, "route_geometry"));
        //Export has no created_at column
        String createdAt = getOptionalValue(record, "created_at");
        if (createdAt.isBlank()) {
            tour.setCreatedAt(LocalDateTime.now());
        } else {
            tour.setCreatedAt(LocalDateTime.parse(createdAt));
        }

        validateTour(tour);
        return tour;
    }

    private TourLog mapCsvRecordToTourLog(CSVRecord record) {
        String logId = getOptionalValue(record, "log_id");

        //If no log id, dieser teil wird übersprungen
        if (logId.isBlank()) {
            return null;
        }

        TourLog log = new TourLog();
        log.setLogID(logId);
        log.setTourID(getRequiredValue(record, "tour_id"));
        log.setDate(getRequiredValue(record, "log_date"));
        log.setTime(getRequiredValue(record, "log_time"));
        log.setComment(getRequiredValue(record, "log_comment"));
        log.setDifficulty(parseInt(getRequiredValue(record, "log_difficulty")));
        log.setTotalDistance(parseDouble(getRequiredValue(record, "log_total_distance")));
        log.setTotalTime(parseDouble(getRequiredValue(record, "log_total_time")));
        log.setRating(parseInt(getRequiredValue(record, "log_rating")));
        log.setCreatorName(getRequiredValue(record, "log_creator"));

        validateTourLogBusinessRules(log);
        return log;
    }

    //Fail bad rows early
    private void validateTour(Tour tour) {
        if (tour.getId() == null || tour.getId().isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        if (tour.getName() == null || tour.getName().length() < 3) {
            throw new IllegalArgumentException("name must have at least 3 characters");
        }
        if (tour.getDescription() == null || tour.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (tour.getStartLocation() == null || tour.getStartLocation().isBlank()) {
            throw new IllegalArgumentException("from is required");
        }
        if (tour.getEndLocation() == null || tour.getEndLocation().isBlank()) {
            throw new IllegalArgumentException("to is required");
        }
        if (tour.getTransportType() == null) {
            throw new IllegalArgumentException("transportType is required");
        }
        if (tour.getChildFriendliness() < 0 || tour.getChildFriendliness() > 5) {
            throw new IllegalArgumentException("childFriendliness must be between 0 and 5");
        }
        if (tour.getDistance() < 0) {
            throw new IllegalArgumentException("distance cannot be negative");
        }
        if (tour.getEstimatedTime() < 0) {
            throw new IllegalArgumentException("estimatedTime cannot be negative");
        }
    }

    //checks that the header exists, the value is not blank, trims it, and throws a clear error if invalid
    private String getRequiredValue(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) {
            throw new IllegalArgumentException("Missing column: " + columnName);
        }

        String value = record.get(columnName);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(columnName + " is required");
        }

        return value.trim();
    }

    //returns a trimmed value if present otherwise an empty string, so imports don't fail on missing optional data
    private String getOptionalValue(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) {
            return "";
        }

        String value = record.get(columnName);

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // Tour business rules (domain-level validation after parsing)
    private void validateTourBusinessRules(Tour tour) {
        if (tour.getId() == null || tour.getId().isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        if (tour.getName() == null || tour.getName().trim().length() < 3) {
            throw new IllegalArgumentException("name must have at least 3 characters");
        }
        if (tour.getDescription() == null || tour.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (tour.getStartLocation() == null || tour.getStartLocation().isBlank()) {
            throw new IllegalArgumentException("from is required");
        }
        if (tour.getEndLocation() == null || tour.getEndLocation().isBlank()) {
            throw new IllegalArgumentException("to is required");
        }
        if (tour.getTransportType() == null) {
            throw new IllegalArgumentException("transportType is required");
        }
        if (tour.getDistance() < 0) {
            throw new IllegalArgumentException("distance cannot be negative");
        }
        if (tour.getEstimatedTime() < 0) {
            throw new IllegalArgumentException("estimatedTime cannot be negative");
        }
        if (tour.getChildFriendliness() < 0 || tour.getChildFriendliness() > 5) {
            throw new IllegalArgumentException("childFriendliness must be between 0 and 5");
        }
        if (tour.getRouteImagePath() == null || tour.getRouteImagePath().isBlank()) {
            throw new IllegalArgumentException("routeImagePath is required");
        }
        if (tour.getCreatedAt() == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }

    // TourLog business rules (domain-level validation after parsing)
    private void validateTourLogBusinessRules(TourLog log) {
        if (log.getLogID() == null) {
            throw new IllegalArgumentException("logID is required");
        }
        if (log.getDate() == null || log.getDate().isBlank()) {
            throw new IllegalArgumentException("date is required");
        }
        if (log.getTime() == null || log.getTime().isBlank()) {
            throw new IllegalArgumentException("time is required");
        }
        if (log.getComment() == null || log.getComment().isBlank()) {
            throw new IllegalArgumentException("comment is required");
        }
        if (log.getDifficulty() < 0 || log.getDifficulty() > 5) {
            throw new IllegalArgumentException("difficulty must be between 0 and 5");
        }
        if (log.getTotalDistance() < 0) {
            throw new IllegalArgumentException("totalDistance cannot be negative");
        }
        if (log.getTotalTime() < 0) {
            throw new IllegalArgumentException("totalTime cannot be negative");
        }
        if (log.getRating() < 0 || log.getRating() > 5) {
            throw new IllegalArgumentException("rating must be between 0 and 5");
        }
        if (log.getTourID() == null || log.getTourID().isBlank()) {
            throw new IllegalArgumentException("tourID is required");
        }
        if (log.getCreatorName() == null || log.getCreatorName().isBlank()) {
            throw new IllegalArgumentException("creatorName is required");
        }

        //Coupled Tour to make sure it exists
        if (!tourRepository.existsById(log.getTourID())) {
            throw new IllegalArgumentException("Referenced tour does not exist: " + log.getTourID());
        }
    }
}
