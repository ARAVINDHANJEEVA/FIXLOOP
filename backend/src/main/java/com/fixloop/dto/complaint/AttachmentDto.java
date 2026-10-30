package com.fixloop.dto.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private Integer id;
    private String fileUrl;
    private String fileName;
    private String mimeType;
}
