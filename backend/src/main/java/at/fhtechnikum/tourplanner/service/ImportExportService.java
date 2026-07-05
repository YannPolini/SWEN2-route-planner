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
        log.info("Starting CSV import. file={}, ownerUserId={}",
                file != null ? file.getOriginalFilename() : null,
                owner != null ? owner.getId() : null
        );

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
            log.debug("CSV headers: {}", parser.getHeaderMap().keySet());
            for (CSVRecord record : parser) {
                String type = null;

                try {
                    type = getRequiredValue(record, "type").toUpperCase();

                    log.debug("Processing CSV row {}. type={}",
                            record.getRecordNumber(),
                            type
                    );

                    if ("TOUR".equals(type)) {
                        Tour tour = mapCsvRecordToTour(record, owner);
                        log.debug("Checking write permission for tour. row={}, tourId={}",
                                record.getRecordNumber(),
                                tour.getId()
                        );
                        requireWritableTourId(tour, owner);
                        log.debug("Saving tour. row={}, tourId={}, name={}",
                                record.getRecordNumber(),
                                tour.getId(),
                                tour.getName()
                        );
                        tourRepository.save(tour);
                        log.info("Imported TOUR row {} successfully. tourId={}",
                                record.getRecordNumber(),
                                tour.getId()
                        );

                    } else if ("LOG".equals(type)) {
                        TourLog tourLog = mapCsvRecordToTourLog(record, owner);

                        if (tourLog != null) {
                            log.debug("Saving tour log. row={}, logId={}, tourId={}",
                                    record.getRecordNumber(),
                                    tourLog.getLogID(),
                                    tourLog.getTourID()
                            );

                            tourLogRepository.save(tourLog);

                            log.info("Imported LOG row {} successfully. logId={}, tourId={}",
                                    record.getRecordNumber(),
                                    tourLog.getLogID(),
                                    tourLog.getTourID()
                            );
                        }

                    } else {
                        log.debug("Failed LOG row {} because log_id is empty",
                                record.getRecordNumber()
                        );
                        throw new IllegalArgumentException("Unknown row type: " + type);
                    }

                    importedRows++;
                } catch (Exception rowException) {
                    failedRows++;
                    String error = "Row " + record.getRecordNumber() + ": " + rowException.getMessage();
                    errors.add(error);

                    log.warn("Failed to import CSV row {}. type={}, error={}",
                            record.getRecordNumber(),
                            type,
                            rowException.getMessage(),
                            rowException
                    );
                }
            }
        } catch (IOException e) {
            log.error("Could not read uploaded CSV file. file={}",
                    file.getOriginalFilename(),
                    e
            );//e ist die gefangene exception
            throw new RuntimeException("Could not read uploaded CSV file", e);
        }

        log.info("CSV import finished. file={}, importedRows={}, failedRows={}",
                file.getOriginalFilename(),
                importedRows,
                failedRows
        );

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

        String tourId = getRequiredValue(record, "tour_id");

        if(tourRepository.findById(tourId).isPresent()) {
            log.info("Tour with this ID already exists, failed import, tourId={}", tourId);
            return null;
        }

        String tourName = getRequiredValue(record, "tour_name");
        String description = getRequiredValue(record, "description");
        String from = getRequiredValue(record, "from");
        String to = getRequiredValue(record, "to");
        String transportType = getRequiredValue(record, "transport_type");

        String startLat = getRequiredValue(record, "startLat");
        String startLng = getRequiredValue(record, "startLng");
        String endLat = getRequiredValue(record, "endLat");
        String endLng = getRequiredValue(record, "endLng");

        String distance = getRequiredValue(record, "distance");
        String estimatedTime = getRequiredValue(record, "estimated_time");

        String routeImagePath = getOptionalValue(record, "routeImagePath");
        String routeGeometry = getRequiredValue(record, "routeGeometry");

        String createdAt = getRequiredValue(record, "createdAt");

        log.debug(
                "TOUR row {} values: tourId={}, name={}, from={}, to={}, transportType={}, startLat={}, startLng={}, endLat={}, endLng={}, distance={}, estimatedTime={}, routeImagePath={}, routeGeometryLength={}, createdAt={}",
                record.getRecordNumber(),
                tourId,
                tourName,
                from,
                to,
                transportType,
                startLat,
                startLng,
                endLat,
                endLng,
                distance,
                estimatedTime,
                routeImagePath,
                routeGeometry.length(),
                createdAt
        );

        tour.setId(tourId);
        tour.setName(tourName);
        tour.setDescription(description);
        tour.setStartLocation(from);
        tour.setEndLocation(to);
        tour.setTransportType(TransportType.valueOf(transportType));

        tour.setStartLat(parseDouble(startLat));
        tour.setStartLng(parseDouble(startLng));
        tour.setEndLat(parseDouble(endLat));
        tour.setEndLng(parseDouble(endLng));

        tour.setDistance(parseDouble(distance));
        tour.setEstimatedTime(parseDouble(estimatedTime));

        TourMetricsCalculator.updateChildFriendliness(tour);

        if (routeImagePath.isBlank()) {
            log.debug("tour row {} empty routeImagePath, using placeholder.", record.getRecordNumber());
            tour.setRouteImagePath("placeholder");
        } else {
            tour.setRouteImagePath(routeImagePath);
        }

        tour.setRouteGeometry(routeGeometry);
        tour.setCreatedAt(LocalDateTime.parse(createdAt));

        assignOwner(tour, owner);
        validateTour(tour);

        return tour;
    }

    private TourLog mapCsvRecordToTourLog(CSVRecord record, AppUser owner) {
        String logId = getOptionalValue(record, "log_id");

        if(tourLogRepository.findById(logId).isPresent()) {
            log.info("LogID already exists, failed import, tourId={}", logId);
            return null;
        }

        String tourId = getRequiredValue(record, "tour_id");
        String logDate = getRequiredValue(record, "log_date");
        String logTime = getRequiredValue(record, "log_time");
        String logComment = getRequiredValue(record, "log_comment");
        String logDifficulty = getRequiredValue(record, "log_difficulty");
        String totalDistance = getRequiredValue(record, "log_total_distance");
        String totalTime = getRequiredValue(record, "log_total_time");
        String rating = getRequiredValue(record, "log_rating");

        log.debug(
                "log row {} values: logId={}, tourId={}, date={}, time={}, difficulty={}, totalDistance={}, totalTime={}, rating={}",
                record.getRecordNumber(),
                logId,
                tourId,
                logDate,
                logTime,
                logDifficulty,
                totalDistance,
                totalTime,
                rating
        );

        TourLog tourLog = new TourLog();
        tourLog.setLogID(logId);
        tourLog.setTourID(tourId);
        tourLog.setDate(logDate);
        tourLog.setTime(logTime);
        tourLog.setComment(logComment);
        tourLog.setDifficulty(parseInt(logDifficulty));
        tourLog.setTotalDistance(parseDouble(totalDistance));
        tourLog.setTotalTime(parseDouble(totalTime));
        tourLog.setRating(parseInt(rating));
        tourLog.setOwnerUserId(owner.getId());
        tourLog.setCreatorName(owner.getName());

        log.debug("validating log row {}. logId={}, tourId={}",
                record.getRecordNumber(),
                tourLog.getLogID(),
                tourLog.getTourID()
        );

        validateTourLog(tourLog, owner);

        log.debug("mapped log row {} successfully, logId={}",
                record.getRecordNumber(),
                tourLog.getLogID()
        );

        return tourLog;
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

    //dont think i need these actually
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

    private void validateTourLog(TourLog log, AppUser owner) {
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
