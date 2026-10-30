package com.fixloop.service;

import com.fixloop.domain.AppUser;
import com.fixloop.domain.Complaint;
import com.fixloop.domain.EscalationLog;
import com.fixloop.domain.StaffProfile;
import com.fixloop.domain.StatusHistory;
import com.fixloop.dto.complaint.ComplaintSummaryDto;
import com.fixloop.repository.ComplaintRepository;
import com.fixloop.repository.EscalationLogRepository;
import com.fixloop.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SlaSchedulerService {
    private final ComplaintRepository complaintRepo;
    private final EscalationLogRepository escalationLogRepo;
    private final StatusHistoryRepository statusHistoryRepo;
    private final NotificationService notificationService;
    
    @Value("${fixloop.sla.at-risk-buffer-hours:2}")
    private int atRiskBufferHours;
    
    public void runSlaCheck() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime atRiskThreshold = now.plusHours(atRiskBufferHours);
        
        // 1. Flag at-risk (within buffer, not yet flagged)
        List<Complaint> atRiskCandidates = complaintRepo.findAtRiskCandidates(atRiskThreshold);
        for (Complaint c : atRiskCandidates) {
            c.setAtRisk(true);
            complaintRepo.save(c);
            notificationService.broadcastSlaAlert(Map.of(
                "type", "AT_RISK",
                "complaintId", c.getId(),
                "token", c.getToken(),
                "slaDeadline", c.getSlaDeadline().toString()
            ));
        }
        
        // 2. Flag breached (past deadline, not yet flagged)
        List<Complaint> breachedCandidates = complaintRepo.findBreachedCandidates(now);
        for (Complaint c : breachedCandidates) {
            c.setBreached(true);
            short newLevel = (short) (c.getEscalationLevel() + 1);
            StaffProfile fromStaff = c.getAssignedToStaff();
            c.setEscalationLevel(newLevel);
            
            // Log auto-escalation
            EscalationLog log = EscalationLog.builder()
                .complaint(c)
                .fromStaff(fromStaff)
                .toStaff(null) // auto-escalation, no specific target
                .previousLevel((short)(newLevel - 1))
                .newLevel(newLevel)
                .reason("SLA deadline breached — automatic escalation")
                .triggeredBy("AUTO")
                .build();
            escalationLogRepo.save(log);
            
            // Status history entry
            StatusHistory sh = StatusHistory.builder()
                .complaint(c)
                .status(c.getStatus())
                .changedByName("System")
                .changedByRole("SYSTEM")
                .note("SLA breached — escalated to level " + newLevel)
                .build();
            statusHistoryRepo.save(sh);
            
            complaintRepo.save(c);
            notificationService.broadcastSlaAlert(Map.of(
                "type", "BREACHED",
                "complaintId", c.getId(),
                "token", c.getToken(),
                "escalationLevel", newLevel
            ));
        }
    }
    
    /** Manual force-escalate by admin */
    public void forceEscalate(Integer complaintId, AppUser adminUser) {
        Complaint c = complaintRepo.findById(complaintId)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
        short newLevel = (short)(c.getEscalationLevel() + 1);
        StaffProfile fromStaff = c.getAssignedToStaff();
        c.setEscalationLevel(newLevel);
        
        EscalationLog log = EscalationLog.builder()
            .complaint(c)
            .fromStaff(fromStaff)
            .toStaff(null)
            .previousLevel((short)(newLevel - 1))
            .newLevel(newLevel)
            .reason("Manually escalated by admin: " + adminUser.getName())
            .triggeredBy("MANUAL")
            .build();
        escalationLogRepo.save(log);
        
        StatusHistory sh = StatusHistory.builder()
            .complaint(c)
            .status(c.getStatus())
            .changedById(adminUser.getId())
            .changedByName(adminUser.getName())
            .changedByRole("ADMIN")
            .note("Force-escalated to level " + newLevel)
            .build();
        statusHistoryRepo.save(sh);
        
        complaintRepo.save(c);
        notificationService.broadcastSlaAlert(Map.of(
            "type", "FORCE_ESCALATED",
            "complaintId", c.getId(),
            "token", c.getToken(),
            "escalationLevel", newLevel
        ));
    }

    @Transactional(readOnly = true)
    public List<ComplaintSummaryDto> getAtRisk(ComplaintService complaintService) {
        return complaintRepo.findAtRisk().stream().map(complaintService::toSummaryDto).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ComplaintSummaryDto> getBreached(ComplaintService complaintService) {
        return complaintRepo.findBreached().stream().map(complaintService::toSummaryDto).collect(Collectors.toList());
    }
}
