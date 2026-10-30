package com.fixloop.dto.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintCreatedDto {
    private Integer id;
    private String token;
    private Integer categoryId;
    private String categoryName;
    private String status;
    private BigDecimal priorityScore;
    private String slaDeadline;
    private String createdAt;
}
