package com.example.pkms.dto;

import com.example.pkms.entity.GeneralEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeneralEventRecommendationDTO {
    private GeneralEvent event;
    private double priorityScore;
    private String scoreExplanation;
}
