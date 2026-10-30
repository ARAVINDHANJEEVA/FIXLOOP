package com.fixloop.service;

import com.fixloop.domain.StaffProfile;
import com.fixloop.dto.analytics.CategoryVolumeDto;
import com.fixloop.dto.analytics.HotspotDto;
import com.fixloop.dto.analytics.SlaTrendDto;
import com.fixloop.dto.analytics.StaffPerformanceDto;
import com.fixloop.repository.ComplaintRepository;
import com.fixloop.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private final ComplaintRepository complaintRepo;
    private final StaffProfileRepository staffRepo;
    
    // Category color map matching frontend mockData colors
    private static final Map<String, String> CATEGORY_COLORS = Map.of(
        "Hostel", "#6366f1",
        "Food", "#f59e0b",
        "Academic", "#3b82f6",
        "Health", "#ef4444",
        "Hygiene", "#22c55e",
        "Tech Support", "#06b6d4",
        "Harassment", "#ec4899",
        "Malpractice", "#f97316",
        "Ragging", "#dc2626"
    );
    
    public List<HotspotDto> getHotspots(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return complaintRepo.findHotspots(since).stream()
            .map(row -> HotspotDto.builder()
                .location((String) row[0])
                .category((String) row[1])
                .count((Long) row[2])
                .build())
            .collect(Collectors.toList());
    }
    
    public List<SlaTrendDto> getSlaTrends(int weeksBack) {
        LocalDateTime since = LocalDateTime.now().minusWeeks(weeksBack);
        return complaintRepo.findSlaTrends(since).stream()
            .map(row -> SlaTrendDto.builder()
                .period((String) row[0])
                .breached(((Number) row[1]).longValue())
                .resolved(((Number) row[2]).longValue())
                .atRisk(((Number) row[3]).longValue())
                .build())
            .collect(Collectors.toList());
    }
    
    public List<CategoryVolumeDto> getCategoryVolume(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return complaintRepo.findCategoryVolume(since).stream()
            .map(row -> {
                String name = (String) row[0];
                return CategoryVolumeDto.builder()
                    .name(name)
                    .value((Long) row[1])
                    .color(CATEGORY_COLORS.getOrDefault(name, "#6366f1"))
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    public List<StaffPerformanceDto> getStaffPerformance(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        // Get performance data
        List<Object[]> perfRows = complaintRepo.findStaffPerformance(since);
        List<Object[]> resolvedRows = complaintRepo.countResolvedSince(since);
        
        // Build resolved count map
        Map<Integer, Integer> resolvedThisMonth = resolvedRows.stream()
            .collect(Collectors.toMap(
                row -> (Integer) row[0],
                row -> ((Long) row[1]).intValue()
            ));
        
        // Get all staff with details for name/dept
        Map<Integer, StaffProfile> staffMap = staffRepo.findAllWithDetails().stream()
            .collect(Collectors.toMap(StaffProfile::getUserId, s -> s));
        
        return perfRows.stream()
            .map(row -> {
                Integer staffId = (Integer) row[0];
                StaffProfile sp = staffMap.get(staffId);
                return StaffPerformanceDto.builder()
                    .staffId(staffId)
                    .staffName(sp != null && sp.getUser() != null ? sp.getUser().getName() : "Unknown")
                    .department(sp != null && sp.getDepartment() != null ? sp.getDepartment().getName() : "Unknown")
                    .avgResolutionHours(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                    .slaCompliancePercent(row[2] != null ? ((Number) row[2]).doubleValue() : 100.0)
                    .openTickets(row[3] != null ? ((Number) row[3]).intValue() : 0)
                    .resolvedThisMonth(resolvedThisMonth.getOrDefault(staffId, 0))
                    .build();
            })
            .collect(Collectors.toList());
    }
}
