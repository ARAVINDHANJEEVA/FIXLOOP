package com.fixloop.service;

import com.fixloop.config.PriorityWeightsConfig;
import com.fixloop.domain.PriorityWeightConfig;
import com.fixloop.repository.PriorityWeightConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriorityScoreServiceTest {

    @Mock
    private PriorityWeightConfigRepository weightConfigRepo;

    @Mock
    private PriorityWeightsConfig defaultWeights;

    @InjectMocks
    private PriorityScoreService priorityScoreService;

    @BeforeEach
    void setUp() {
        when(weightConfigRepo.findTopByOrderByEffectiveFromDesc()).thenReturn(Optional.of(
                PriorityWeightConfig.builder()
                        .w1Severity(new BigDecimal("0.480"))
                        .w2Students(new BigDecimal("0.210"))
                        .w3Recurrence(new BigDecimal("0.080"))
                        .w4TimeDecay(new BigDecimal("0.230"))
                        .build()
        ));
    }

    @Test
    void testComputePriorityScore_criticalCase() {
        // severity = 5, studentsAffected = 500, recurrence = 10, hoursElapsed = 24, slaHours = 24
        // All norms should be 1.0 -> 100 * (0.48 + 0.21 + 0.08 + 0.23) = 100.00
        BigDecimal score = priorityScoreService.compute(5, 500, 10, 24, 24);
        assertNotNull(score);
        assertEquals(new BigDecimal("100.00"), score);
    }

    @Test
    void testComputePriorityScore_lowCase() {
        // severity = 1, studentsAffected = 1, recurrence = 0, hoursElapsed = 0, slaHours = 24
        BigDecimal score = priorityScoreService.compute(1, 1, 0, 0, 24);
        assertNotNull(score);
        assertTrue(score.doubleValue() > 0.0 && score.doubleValue() < 25.0);
    }
}
