package com.fixloop.service;

import com.fixloop.config.PriorityWeightsConfig;
import com.fixloop.domain.Complaint;
import com.fixloop.domain.PriorityWeightConfig;
import com.fixloop.repository.PriorityWeightConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PriorityScoreService {
    private final PriorityWeightConfigRepository weightConfigRepo;
    private final PriorityWeightsConfig defaultWeights;
    
    /**
     * Compute priority score using formula:
     * severity_norm     = severity / 5
     * scale_norm        = ln(1 + studentsAffected) / ln(1 + 500)  [cap at 500]
     * recurrence_norm   = min(recurrenceCount90d, 10) / 10
     * time_urgency_norm = min(hoursElapsed / slaHours, 1.0)
     * score = 100 * (w1*severity_norm + w2*scale_norm + w3*recurrence_norm + w4*time_urgency_norm)
     */
    public BigDecimal compute(int severity, int studentsAffected, int recurrenceCount90d,
                              long hoursElapsed, int slaHours) {
        
        PriorityWeightConfig dbConfig = weightConfigRepo.findTopByOrderByEffectiveFromDesc().orElse(null);
        
        double w1 = dbConfig != null && dbConfig.getW1Severity() != null ? dbConfig.getW1Severity().doubleValue() : defaultWeights.getW1();
        double w2 = dbConfig != null && dbConfig.getW2Students() != null ? dbConfig.getW2Students().doubleValue() : defaultWeights.getW2();
        double w3 = dbConfig != null && dbConfig.getW3Recurrence() != null ? dbConfig.getW3Recurrence().doubleValue() : defaultWeights.getW3();
        double w4 = dbConfig != null && dbConfig.getW4TimeDecay() != null ? dbConfig.getW4TimeDecay().doubleValue() : defaultWeights.getW4();
        
        double severityNorm = Math.min(severity / 4.0, 1.0);  // normalized to [0, 1], severity 1-4 (or 5)
        double scaleNorm = Math.log(1 + Math.min(studentsAffected, 500)) / Math.log(1 + 500);
        double recurrenceNorm = Math.min(recurrenceCount90d, 10) / 10.0;
        double timeUrgencyNorm = Math.min((double) hoursElapsed / Math.max(slaHours, 1), 1.0);
        
        double score = 100 * (w1 * severityNorm + w2 * scaleNorm + w3 * recurrenceNorm + w4 * timeUrgencyNorm);
        
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
    
    /** Recompute and update a complaint's priority score in place */
    public BigDecimal recompute(Complaint complaint) {
        long hoursElapsed = ChronoUnit.HOURS.between(complaint.getCreatedAt(), LocalDateTime.now());
        int slaHours = complaint.getCategory().getDefaultSlaHours();
        return compute(complaint.getSeverity(), complaint.getStudentsAffected(),
                       complaint.getRecurrenceCount90d(), hoursElapsed, slaHours);
    }
}
