package com.fixloop.scheduler;

import com.fixloop.service.SlaSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {
    private final SlaSchedulerService slaSchedulerService;
    
    @Scheduled(fixedRateString = "${fixloop.sla.scheduler-interval-ms:300000}")
    public void runSlaCheck() {
        log.info("[SlaScheduler] Running SLA check at {}", LocalDateTime.now());
        try {
            slaSchedulerService.runSlaCheck();
        } catch (Exception e) {
            log.error("[SlaScheduler] Error during SLA check", e);
        }
    }
}
