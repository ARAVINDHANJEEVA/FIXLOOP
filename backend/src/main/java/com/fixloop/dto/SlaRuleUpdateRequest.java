package com.fixloop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaRuleUpdateRequest {
    @NotNull
    private Integer resolutionHours;

    @NotNull
    private Integer escalationHours;
}
