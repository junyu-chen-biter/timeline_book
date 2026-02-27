package com.example.pkms.dto;

import com.example.pkms.entity.Subject;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SubjectRecommendationDTO {
    private Subject subject;
    private double priorityScore;
    private int totalReviewTimeMinutes;
    private List<BlockRecommendationDTO> files; // optional: top files under this subject
}

