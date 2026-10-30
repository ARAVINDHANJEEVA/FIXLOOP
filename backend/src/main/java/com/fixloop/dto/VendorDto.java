package com.fixloop.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDto {
    private Integer id;
    private String name;
    private String phone;
    private String email;
    private String contactPerson;
    private List<String> skillTags;
}
