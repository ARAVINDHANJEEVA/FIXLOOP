package com.fixloop.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Integer id;
    private Integer complaintId;
    private String complaintToken;
    private String type;
    private String title;
    private String body;
    private String statusAtSend;
    private boolean read;
    private String createdAt;
}
