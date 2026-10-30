package com.fixloop.controller;

import com.fixloop.domain.Incident;
import com.fixloop.dto.complaint.ClusterMemberDto;
import com.fixloop.dto.complaint.IncidentSummaryDto;
import com.fixloop.repository.ComplaintRepository;
import com.fixloop.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Incident (cluster) management endpoints.
 * Primarily used by the admin manual-cluster target picker dropdown.
 */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentRepository incidentRepo;
    private final ComplaintRepository complaintRepo;

    /**
     * GET /api/incidents?categoryId=3&status=OPEN
     * Returns open incidents for the manual-cluster target picker.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<IncidentSummaryDto>> getIncidents(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "OPEN") String status) {

        List<Incident> incidents = (categoryId != null)
                ? incidentRepo.findByCategoryIdAndStatus(categoryId, status)
                : incidentRepo.findByStatus(status);

        List<IncidentSummaryDto> dtos = incidents.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/incidents/{id}
     * Returns full incident detail including all member complaints.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IncidentSummaryDto> getById(@PathVariable Integer id) {
        Incident incident = incidentRepo.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Incident not found: " + id));
        return ResponseEntity.ok(toDto(incident));
    }

    private IncidentSummaryDto toDto(Incident incident) {
        List<ClusterMemberDto> members = complaintRepo.findByIncidentId(incident.getId()).stream()
                .map(c -> ClusterMemberDto.builder()
                        .id(c.getId())
                        .token(c.getToken())
                        .description(c.getDescription())
                        .status(c.getStatus())
                        .clusterScore(c.getClusterScore())
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return IncidentSummaryDto.builder()
                .id(incident.getId())
                .status(incident.getStatus())
                .complaintCount(incident.getComplaintCount())
                .summary(incident.getSummary())
                .members(members)
                .createdAt(incident.getCreatedAt() != null ? incident.getCreatedAt().toString() : null)
                .lastComplaintAt(incident.getLastComplaintAt() != null
                        ? incident.getLastComplaintAt().toString() : null)
                .build();
    }
}
