package com.fixloop.dto.complaint;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintCreateRequest {

    @NotNull
    private Integer categoryId;

    @NotNull
    private Integer locationId;

    @NotNull
    @Min(1) @Max(4)
    private Integer severity;

    private boolean isAnonymous;

    private Integer studentsAffected;

    /** Dynamic form field responses — validated server-side against category.form_schema */
    private Map<String, Object> extraData;
}
