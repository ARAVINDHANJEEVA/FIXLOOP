package com.fixloop.dto.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorUpdateRequest {
    @NotNull
    private Integer vendorId;
    @NotBlank
    private String contactStatus;
    private String notes;
}
