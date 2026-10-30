package com.fixloop.controller;

import com.fixloop.domain.Location;
import com.fixloop.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationRepository locationRepo;

    /**
     * GET /api/locations/buildings
     * Returns distinct building names — drives LocationPickerBottomSheet step 1
     */
    @GetMapping("/buildings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getBuildings() {
        return ResponseEntity.ok(locationRepo.findDistinctBuildings());
    }

    /**
     * GET /api/locations?building=X&floor=Y
     * Returns matching location entries — drives step 2 (floor) and step 3 (room)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getLocations(
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String floor) {

        List<Location> locations;

        if (building != null && floor != null) {
            locations = locationRepo.findByBuildingAndFloor(building, floor);
        } else if (building != null) {
            locations = locationRepo.findByBuilding(building);
        } else {
            locations = locationRepo.findAll();
        }

        List<Map<String, Object>> result = locations.stream()
            .map(l -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", l.getId());
                map.put("building", l.getBuilding());
                map.put("floor", l.getFloor());
                map.put("room", l.getRoom());
                map.put("campusZone", l.getCampusZone());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
