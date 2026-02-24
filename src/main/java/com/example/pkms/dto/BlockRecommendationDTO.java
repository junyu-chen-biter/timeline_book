package com.example.pkms.dto;

import com.example.pkms.entity.Block;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BlockRecommendationDTO {
    private Block block;
    private double priorityScore;
    private LocalDateTime lastReviewTime;
    // Helper to explain score for UI (optional)
    private String scoreExplanation; 
}