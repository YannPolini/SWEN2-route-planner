package at.fhtechnikum.tourplanner.service;

import at.fhtechnikum.tourplanner.dto.demo.DemoSeedResponse;
import at.fhtechnikum.tourplanner.model.AppUser;
import at.fhtechnikum.tourplanner.model.Tour;
import at.fhtechnikum.tourplanner.model.TourLog;
import at.fhtechnikum.tourplanner.repository.AppUserRepository;
import at.fhtechnikum.tourplanner.repository.TourLogRepository;
import at.fhtechnikum.tourplanner.repository.TourRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoDataService {

    private static final String DEMO_EMAIL = "demo@tourplanner.local";

    private final AppUserRepository userRepository;
    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;

    public DemoDataService(AppUserRepository userRepository,
                           TourRepository tourRepository,
                           TourLogRepository tourLogRepository) {
        this.userRepository = userRepository;
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
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

        AppUser demoUser = userRepository.findByEmail(DEMO_EMAIL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo account not found."));

        List<Tour> demoTours = tourRepository.findByOwnerUserId(demoUser.getId());
        if (demoTours.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo account has no tours to copy.");
        }

        Map<String, String> copiedTourIds = new HashMap<>();
        for (Tour demoTour : demoTours) {
            Tour copy = copyTour(demoTour, owner);
            copiedTourIds.put(demoTour.getId(), copy.getId());
            tourRepository.save(copy);
        }

        int copiedLogs = 0;
        for (TourLog demoLog : tourLogRepository.findByOwnerUserId(demoUser.getId())) {
            String copiedTourId = copiedTourIds.get(demoLog.getTourID());
            if (copiedTourId == null) {
                continue;
            }

            tourLogRepository.save(copyLog(demoLog, copiedTourId, owner));
            copiedLogs++;
        }

        return new DemoSeedResponse(copiedTourIds.size(), copiedLogs);
    }

    private Tour copyTour(Tour source, AppUser owner) {
        Tour copy = new Tour();
        copy.setId(source.getId() + "-user-" + owner.getId());
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setStartLocation(source.getStartLocation());
        copy.setEndLocation(source.getEndLocation());
        copy.setTransportType(source.getTransportType());
        copy.setStartLat(source.getStartLat());
        copy.setStartLng(source.getStartLng());
        copy.setEndLat(source.getEndLat());
        copy.setEndLng(source.getEndLng());
        copy.setDistance(source.getDistance());
        copy.setEstimatedTime(source.getEstimatedTime());
        copy.setChildFriendliness(source.getChildFriendliness());
        copy.setOwnerUserId(owner.getId());
        copy.setCreatorName(owner.getName());
        copy.setRouteImagePath(source.getRouteImagePath());
        copy.setRouteGeometry(source.getRouteGeometry());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }

    private TourLog copyLog(TourLog source, String copiedTourId, AppUser owner) {
        TourLog copy = new TourLog();
        copy.setLogID(source.getLogID() + "-user-" + owner.getId());
        copy.setDate(source.getDate());
        copy.setTime(source.getTime());
        copy.setComment(source.getComment());
        copy.setDifficulty(source.getDifficulty());
        copy.setTotalDistance(source.getTotalDistance());
        copy.setTotalTime(source.getTotalTime());
        copy.setRating(source.getRating());
        copy.setTourID(copiedTourId);
        copy.setOwnerUserId(owner.getId());
        copy.setCreatorName(owner.getName());
        return copy;
    }
}
