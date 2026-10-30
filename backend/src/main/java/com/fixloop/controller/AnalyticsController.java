package com.fixloop.controller;

import com.fixloop.dto.analytics.CategoryVolumeDto;
import com.fixloop.dto.analytics.HotspotDto;
import com.fixloop.dto.analytics.SlaTrendDto;
import com.fixloop.dto.analytics.StaffPerformanceDto;
import com.fixloop.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/hotspots")
    public ResponseEntity<List<HotspotDto>> getHotspots(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getHotspots(days));
    }

    @GetMapping("/sla-trends")
    public ResponseEntity<List<SlaTrendDto>> getSlaTrends(@RequestParam(defaultValue = "12") int weeks) {
        return ResponseEntity.ok(analyticsService.getSlaTrends(weeks));
    }

    @GetMapping("/category-volume")
    public ResponseEntity<List<CategoryVolumeDto>> getCategoryVolume(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getCategoryVolume(days));
    }

    @GetMapping("/staff-performance")
    public ResponseEntity<List<StaffPerformanceDto>> getStaffPerformance(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getStaffPerformance(days));
    }
}
