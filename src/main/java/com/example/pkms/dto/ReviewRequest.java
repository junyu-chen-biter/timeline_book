package com.example.pkms.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Integer progress; // 10 - 100
    private Boolean isMastered;
}
