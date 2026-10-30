package com.fixloop.controller;

import com.fixloop.domain.AppUser;
import com.fixloop.domain.SlaRule;
import com.fixloop.dto.SlaRuleDto;
import com.fixloop.dto.SlaRuleUpdateRequest;
import com.fixloop.dto.complaint.ComplaintSummaryDto;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.repository.SlaRuleRepository;
import com.fixloop.service.ComplaintService;
import com.fixloop.service.SlaSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SlaController {

    private final SlaSchedulerService slaSchedulerService;
    private final SlaRuleRepository slaRuleRepo;
    private final ComplaintService complaintService;
    private final AppUserRepository userRepo;

    private AppUser getCurrentUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping("/sla/at-risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintSummaryDto>> getAtRisk() {
        return ResponseEntity.ok(slaSchedulerService.getAtRisk(complaintService));
    }

    @GetMapping("/sla/breached")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintSummaryDto>> getBreached() {
        return ResponseEntity.ok(slaSchedulerService.getBreached(complaintService));
    }

    @PostMapping("/sla/{id}/force-escalate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceEscalate(@PathVariable Integer id, Authentication auth) {
        slaSchedulerService.forceEscalate(id, getCurrentUser(auth));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sla-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SlaRuleDto>> getSlaRules() {
        List<SlaRuleDto> rules = slaRuleRepo.findAll().stream().map(rule -> SlaRuleDto.builder()
            .id(rule.getId())
            .categoryId(rule.getCategory() != null ? rule.getCategory().getId() : null)
            .categoryName(rule.getCategory() != null ? rule.getCategory().getName() : null)
            .severityTier(rule.getSeverityTier())
            .resolutionHours(rule.getResolutionHours())
            .escalationHours(rule.getEscalationHours())
            .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }

    @PatchMapping("/sla-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlaRuleDto> updateSlaRule(@PathVariable Integer id, @RequestBody SlaRuleUpdateRequest request) {
        SlaRule rule = slaRuleRepo.findById(id).orElseThrow();
        if (request.getResolutionHours() != null) rule.setResolutionHours(request.getResolutionHours());
        if (request.getEscalationHours() != null) rule.setEscalationHours(request.getEscalationHours());
        slaRuleRepo.save(rule);
        
        SlaRuleDto dto = SlaRuleDto.builder()
            .id(rule.getId())
            .categoryId(rule.getCategory() != null ? rule.getCategory().getId() : null)
            .categoryName(rule.getCategory() != null ? rule.getCategory().getName() : null)
            .severityTier(rule.getSeverityTier())
            .resolutionHours(rule.getResolutionHours())
            .escalationHours(rule.getEscalationHours())
            .build();
        return ResponseEntity.ok(dto);
    }
}
