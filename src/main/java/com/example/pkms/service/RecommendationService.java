package com.example.pkms.service;

import com.example.pkms.dto.BlockRecommendationDTO;
import com.example.pkms.entity.Block;
import com.example.pkms.entity.BlockType;
import com.example.pkms.entity.StudyRecord;
import com.example.pkms.repository.BlockRepository;
import com.example.pkms.repository.StudyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final BlockRepository blockRepository;
    private final StudyRecordRepository studyRecordRepository;

    public List<BlockRecommendationDTO> getRecommendedFiles() {
        // 1. Fetch all relevant data (N+1 optimization: fetch all at once)
        List<Block> allBlocks = blockRepository.findAll();
        Map<Long, StudyRecord> recordMap = studyRecordRepository.findAll().stream()
                .collect(Collectors.toMap(StudyRecord::getBlockId, r -> r));

        // 2. Filter files only (folders are not "reviewed" directly)
        return allBlocks.stream()
                .filter(b -> b.getType() != BlockType.FOLDER)
                .map(block -> calculateScore(block, recordMap.get(block.getId())))
                .sorted((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore())) // Descending sort
                .limit(20) // Top 20
                .collect(Collectors.toList());
    }

    private BlockRecommendationDTO calculateScore(Block block, StudyRecord record) {
        LocalDateTime now = LocalDateTime.now();
        
        // --- 1. Time Factor (Ebbinghaus Proxy) ---
        double timeFactor;
        if (record == null || record.getLastReviewTime() == null) {
            // Never reviewed: Highest priority base
            timeFactor = 1000.0; 
        } else {
            long hoursSince = ChronoUnit.HOURS.between(record.getLastReviewTime(), now);
            // The longer since review, the higher the factor.
            // Using hours as base unit. Minimum 1.0 to avoid zero multiplication.
            timeFactor = Math.max(1.0, hoursSince);
        }

        // --- 2. Attribute Weighting ---
        // TODO: In a real app, we should traverse up the parent tree to find inherited attributes 
        // if they are null on the file itself. For now, we use defaults if null.
        int difficulty = block.getDifficulty() != null ? block.getDifficulty() : 1;
        double credits = block.getCredits() != null ? block.getCredits() : 1.0;

        double attrWeight = (1 + difficulty / 10.0) * (1 + credits / 10.0);

        double score = timeFactor * attrWeight;

        // --- 3. DDL Booster ---
        // If DDL is close (e.g., within 7 days), boost significantly
        if (block.getDdl() != null) {
            long daysToDDL = ChronoUnit.DAYS.between(LocalDate.now(), block.getDdl());
            if (daysToDDL >= 0 && daysToDDL <= 3) {
                score *= 3.0; // Very Urgent
            } else if (daysToDDL > 3 && daysToDDL <= 7) {
                score *= 2.0; // Urgent
            } else if (daysToDDL < 0) {
                score *= 4.0; // Overdue
            }
        }

        // --- 4. Mastery & Progress ---
        if (record != null) {
            if (Boolean.TRUE.equals(record.getIsMastered())) {
                return new BlockRecommendationDTO(
                    block, 
                    0.0, 
                    record.getLastReviewTime(), 
                    "Mastered"
                );
            }
            
            Integer progress = record.getLastReviewProgress() != null ? record.getLastReviewProgress() : 100;
            if (progress < 100) {
                // If not fully reviewed, increase priority
                // E.g. 10% progress -> multiplier 5.5
                // 50% progress -> multiplier 3.5
                double progressFactor = 1.0 + (100 - progress) / 20.0;
                score *= progressFactor;
            }
        }

        return new BlockRecommendationDTO(
            block, 
            score, 
            record != null ? record.getLastReviewTime() : null,
            String.format("TF: %.1f, W: %.2f", timeFactor, attrWeight)
        );
    }
}