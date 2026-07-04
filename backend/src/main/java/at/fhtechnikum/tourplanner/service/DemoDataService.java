package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.demo.DemoSeedResponse;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoDataService {

    private static final String DEMO_DATA_PATH = "demo/demo-data.json";

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;
    private final ObjectMapper objectMapper;

    public DemoDataService(TourRepository tourRepository,
                           TourLogRepository tourLogRepository,
                           ObjectMapper objectMapper) {
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DemoSeedResponse seedDemoData(AppUser owner) {
        List<Tour> existingTours = tourRepository.findByOwnerUserId(owner.getId());
        if (!existingTours.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Demo data can only be added to an empty route library."
            );
        }

        DemoDataFile demoData = readDemoData();
        if (demoData.tours().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bundled demo data has no tours.");
        }

        Map<String, String> copiedTourIds = new LinkedHashMap<>();
        for (DemoTour demoTour : demoData.tours()) {
            Tour copy = createTour(demoTour, owner);
            copiedTourIds.put(demoTour.id(), copy.getId());
            tourRepository.save(copy);
        }

        int copiedLogs = 0;
        for (DemoTourLog demoLog : demoData.logs()) {
            String copiedTourId = copiedTourIds.get(demoLog.tourID());
            if (copiedTourId == null) {
                continue;
            }

            tourLogRepository.save(createLog(demoLog, copiedTourId, owner));
            copiedLogs++;
        }

        return new DemoSeedResponse(copiedTourIds.size(), copiedLogs);
    }

    private DemoDataFile readDemoData() {
        try {
            ClassPathResource resource = new ClassPathResource(DEMO_DATA_PATH);
            return objectMapper.readValue(resource.getInputStream(), DemoDataFile.class);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read bundled demo data.",
                    e
            );
        }
    }

    private Tour createTour(DemoTour source, AppUser owner) {
        Tour tour = new Tour();
        tour.setId(source.id() + "-user-" + owner.getId());
        tour.setName(source.name());
        tour.setDescription(source.description());
        tour.setStartLocation(source.from());
        tour.setEndLocation(source.to());
        tour.setTransportType(source.transportType());
        tour.setStartLat(source.fromLat());
        tour.setStartLng(source.fromLng());
        tour.setEndLat(source.toLat());
        tour.setEndLng(source.toLng());
        tour.setDistance(source.distance());
        tour.setEstimatedTime(source.estimatedTime());
        tour.setChildFriendliness(source.childFriendliness());
        tour.setOwnerUserId(owner.getId());
        tour.setCreatorName(owner.getName());
        tour.setRouteImagePath(source.routeImagePath());
        tour.setRouteGeometry(source.routeGeometry());
        tour.setCreatedAt(source.createdAt());
        return tour;
    }

    private TourLog createLog(DemoTourLog source, String copiedTourId, AppUser owner) {
        TourLog log = new TourLog();
        log.setLogID(source.logID() + "-user-" + owner.getId());
        log.setDate(source.date());
        log.setTime(source.time());
        log.setComment(source.comment());
        log.setDifficulty(source.difficulty());
        log.setTotalDistance(source.totalDistance());
        log.setTotalTime(source.totalTime());
        log.setRating(source.rating());
        log.setTourID(copiedTourId);
        log.setOwnerUserId(owner.getId());
        log.setCreatorName(owner.getName());
        return log;
    }

    private record DemoDataFile(List<DemoTour> tours, List<DemoTourLog> logs) {
    }

    private record DemoTour(
            String id,
            String name,
            String description,
            String from,
            String to,
            at.fhtechnikum.tourplanner.model.TransportType transportType,
            Double fromLat,
            Double fromLng,
            Double toLat,
            Double toLng,
            double distance,
            double estimatedTime,
            int childFriendliness,
            String routeImagePath,
            String routeGeometry,
            LocalDateTime createdAt
    ) {
    }

    private record DemoTourLog(
            String logID,
            String date,
            String time,
            String comment,
            int difficulty,
            double totalDistance,
            double totalTime,
            int rating,
            String tourID
    ) {
    }
}
