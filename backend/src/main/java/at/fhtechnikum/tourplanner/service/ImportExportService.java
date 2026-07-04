package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.importexport.ImportResultDto;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.model.TransportType;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

@Service
public class ImportExportService {

    private static final Logger log = LoggerFactory.getLogger(ImportExportService.class);

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;

    public ImportExportService(TourRepository tourRepository, TourLogRepository tourLogRepository) {
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
    }

    @Transactional
    public ImportResultDto importTours(MultipartFile file, AppUser owner) {
        log.info("Importing tours from file {}", file.getOriginalFilename());
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
            for (CSVRecord record : parser) {
                try {
                    String type = getRequiredValue(record, "type").toUpperCase();

                    if ("TOUR".equals(type)) {
                        log.info("Importing tour from file {}", file.getOriginalFilename());
                        Tour tour = mapCsvRecordToTour(record, owner);
                        requireWritableTourId(tour, owner);
                        tourRepository.save(tour);
                        log.info("Successfully imported tour from file {}", file.getOriginalFilename());

                    } else if ("LOG".equals(type)) {
                        log.info("Importing log from file {}", file.getOriginalFilename());
                        TourLog log = mapCsvRecordToTourLog(record, owner);
                        if (log != null) {
                            tourLogRepository.save(log);
                        }

                    } else {
                        log.warn("Unknown CSV record type {}", type);
                        throw new IllegalArgumentException("Unknown row type: " + type);
                    }

                    importedRows++;
                } catch (Exception rowException) {
                    failedRows++;
                    errors.add("Row " + record.getRecordNumber() + ": " + rowException.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read uploaded CSV file", e);
        }

        return new ImportResultDto(importedRows, failedRows, errors);
    }

    @Transactional(readOnly = true)
    public byte[] exportAsCsv(AppUser owner) {
        log.info("Exporting TOurs and Logs as CSV");
        List<Tour> tours = tourRepository.findByOwnerUserId(owner.getId());

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                CSVPrinter csv = new CSVPrinter(
                        writer,
                        CSVFormat.DEFAULT.builder()
                                .setHeader( //keine Description, startLat, StartLng, endLat, endLng, difficulty, OwnerUserId, routeImagePath, routeGeometry, createdAt
                                            // done,
                                        "type", "tour_id", "tour_name", "description", "from", "to", "transport_type",
                                        "startLat", "startLng", "endLat", "endLng", "distance",
                                        "estimated_time", "child_friendliness", "difficulty", "routeImagePath", "routeGeometry", "createdAt",
                                        "log_id", "log_date", "log_time", "log_comment", "log_difficulty",
                                        "log_total_distance", "log_total_time", "log_rating", "log_creator"
                                )
                                .build()
                )
        ) {
            for (Tour tour : tours) {
                List<TourLog> logs = tourLogRepository.findByTourIDAndOwnerUserId(tour.getId(), owner.getId());
                TourMetricsCalculator.updateChildFriendliness(tour);

                if (logs.isEmpty()) {
                    csv.printRecord(
                            "TOUR", tour.getId(), tour.getName(), tour.getDescription(), tour.getStartLocation(), tour.getEndLocation(), tour.getTransportType(),
                            tour.getStartLat(), tour.getStartLng(), tour.getEndLat(), tour.getEndLng(), tour.getDistance(), tour.getEstimatedTime(),
                            tour.getChildFriendliness(), tour.getDifficulty(), tour.getRouteImagePath(), tour.getRouteGeometry(), tour.getCreatedAt(), "", "", "", "", "", "", "", "", "", ""
                    );
                    continue;
                } else {
                    csv.printRecord(
                            "TOUR", tour.getId(), tour.getName(), tour.getDescription(), tour.getStartLocation(), tour.getEndLocation(), tour.getTransportType(),
                            tour.getStartLat(), tour.getStartLng(), tour.getEndLat(), tour.getEndLng(), tour.getDistance(), tour.getEstimatedTime(),
                            tour.getChildFriendliness(), tour.getDifficulty(), tour.getRouteImagePath(), tour.getRouteGeometry(), tour.getCreatedAt(), "", "", "", "", "", "", "", "", "", ""
                    );
                }

                for (TourLog log : logs) {

                    csv.printRecord(
                            "LOG",
                            //tour.getId(), tour.getName(), tour.getStartLocation(), tour.getEndLocation(),
                            //tour.getTransportType(), tour.getDistance(), tour.getEstimatedTime(),
                            //tour.getChildFriendliness(),
                            tour.getId(),
                            "",             // tour_name
                            "",             // description
                            "",             // from
                            "",             // to
                            "",             // transport_type
                            "",             // startLat
                            "",             // startLng
                            "",             // endLat
                            "",             // endLng
                            "",             // distance
                            "",             // estimated_time
                            "",             // child_friendliness
                            "",             // difficulty
                            "",             // routImagePath
                            "",             // routeGeometry
                            "",             // createdAt
                            log.getLogID(), log.getDate(), log.getTime(),
                            log.getComment(), log.getDifficulty(), log.getTotalDistance(), log.getTotalTime(),
                            log.getRating(), log.getOwnerUserId()
                    );
                }
            }

            csv.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not export tours + logs as CSV", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are allowed");
        }
    }

    private Tour mapCsvRecordToTour(CSVRecord record, AppUser owner) {
        Tour tour = new Tour();
        //Difficulty fehlt
        /*
        tour.setId(getRequiredValue(record, "tour_id"));
        tour.setName(getRequiredValue(record, "tour_name"));
        tour.setDescription(getRequiredValue(record, "description"));
        tour.setStartLocation(getRequiredValue(record, "from"));
        tour.setEndLocation(getRequiredValue(record, "to"));
        tour.setTransportType(TransportType.valueOf(getRequiredValue(record, "transport_type")));

        tour.setStartLat(parseDouble(getRequiredValue(record, "startLat")));
        tour.setStartLng(parseDouble(getRequiredValue(record, "startLng")));
        tour.setEndLat(parseDouble(getRequiredValue(record, "endLat")));
        tour.setEndLng(parseDouble(getRequiredValue(record, "endLng")));

        tour.setDistance(parseDouble(getRequiredValue(record, "distance")));
        tour.setEstimatedTime(parseDouble(getRequiredValue(record, "estimated_time")));

        TourMetricsCalculator.updateChildFriendliness(tour);

        tour.setRouteImagePath(getRequiredValue(record, "routeImagePath"));
        tour.setRouteGeometry(getRequiredValue(record, "routeGeometry"));
        tour.setCreatedAt(LocalDateTime.parse(getRequiredValue(record, "created_at")));

         */

        System.out.println("----- CSV TOUR ROW " + record.getRecordNumber() + " -----");

        String tourId = getRequiredValue(record, "tour_id");
        System.out.println("tour_id: " + tourId);
        tour.setId(tourId);

        String tourName = getRequiredValue(record, "tour_name");
        System.out.println("tour_name: " + tourName);
        tour.setName(tourName);

        String description = getRequiredValue(record, "description");
        System.out.println("description: " + description);
        tour.setDescription(description);

        String from = getRequiredValue(record, "from");
        System.out.println("from: " + from);
        tour.setStartLocation(from);

        String to = getRequiredValue(record, "to");
        System.out.println("to: " + to);
        tour.setEndLocation(to);

        String transportType = getRequiredValue(record, "transport_type");
        System.out.println("transport_type: " + transportType);
        tour.setTransportType(TransportType.valueOf(transportType));

        String startLat = getRequiredValue(record, "startLat");
        System.out.println("startLat: " + startLat);
        tour.setStartLat(parseDouble(startLat));

        String startLng = getRequiredValue(record, "startLng");
        System.out.println("startLng: " + startLng);
        tour.setStartLng(parseDouble(startLng));

        String endLat = getRequiredValue(record, "endLat");
        System.out.println("endLat: " + endLat);
        tour.setEndLat(parseDouble(endLat));

        String endLng = getRequiredValue(record, "endLng");
        System.out.println("endLng: " + endLng);
        tour.setEndLng(parseDouble(endLng));

        String distance = getRequiredValue(record, "distance");
        System.out.println("distance: " + distance);
        tour.setDistance(parseDouble(distance));

        String estimatedTime = getRequiredValue(record, "estimated_time");
        System.out.println("estimated_time: " + estimatedTime);
        tour.setEstimatedTime(parseDouble(estimatedTime));

        TourMetricsCalculator.updateChildFriendliness(tour);
        System.out.println("child_friendliness calculated: " + tour.getChildFriendliness());

        //Falls routeImagePath dableit einfach optional machen
        String routeImagePath = getRequiredValue(record, "routeImagePath");
        if(routeImagePath == null || routeImagePath.isEmpty()) {
            tour.setRouteImagePath("placeholder");  //weil not_null
        } else {
            tour.setRouteImagePath(routeImagePath);
        }

        String routeGeometry = getRequiredValue(record, "routeGeometry");
        System.out.println("routeGeometry: " + routeGeometry);
        tour.setRouteGeometry(routeGeometry);

        String createdAt = getRequiredValue(record, "createdAt");
        System.out.println("created_at: " + createdAt);
        tour.setCreatedAt(LocalDateTime.parse(createdAt));

        assignOwner(tour, owner);
        validateTour(tour);

        return tour;
    }

    private TourLog mapCsvRecordToTourLog(CSVRecord record, AppUser owner) {
        String logId = getOptionalValue(record, "log_id");
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
        log.setOwnerUserId(owner.getId());
        log.setCreatorName(owner.getName());
        log.setOwnerUserId(owner.getId());

        validateTourLogBusinessRules(log, owner);
        return log;
    }

    private void assignOwner(Tour tour, AppUser owner) {
        tour.setOwnerUserId(owner.getId());
        tour.setCreatorName(owner.getName());
    }

    private void requireWritableTourId(Tour tour, AppUser owner) {
        if (tourRepository.existsById(tour.getId())
                && !tourRepository.existsByIdAndOwnerUserId(tour.getId(), owner.getId())) {
            throw new IllegalArgumentException("Tour belongs to another user: " + tour.getId());
        }
    }

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

    private String getOptionalValue(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) {
            return "";
        }

        String value = record.get(columnName);
        return value == null ? "" : value.trim();
    }

    private void validateTourLogBusinessRules(TourLog log, AppUser owner) {
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
        if (log.getOwnerUserId() == null) {
            throw new IllegalArgumentException("creatorName is required");
        }
        if (!tourRepository.existsByIdAndOwnerUserId(log.getTourID(), owner.getId())) {
            throw new IllegalArgumentException("Referenced tour does not exist: " + log.getTourID());
        }
    }
}
