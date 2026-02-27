package com.example.pkms.service;

import com.example.pkms.dto.BlockRecommendationDTO;
import com.example.pkms.dto.GeneralEventRecommendationDTO;
import com.example.pkms.dto.SubjectRecommendationDTO;
import com.example.pkms.entity.*;
import com.example.pkms.repository.BlockRepository;
import com.example.pkms.repository.GeneralEventRepository;
import com.example.pkms.repository.StudyRecordRepository;
import com.example.pkms.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final BlockRepository blockRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final SubjectRepository subjectRepository;
    private final GeneralEventRepository generalEventRepository;

    public List<GeneralEventRecommendationDTO> getRecommendedEvents() {
        List<GeneralEvent> events = generalEventRepository.findByStatus(GeneralEvent.EventStatus.PENDING);
        return events.stream()
                .map(this::calculateEventScore)
                .sorted((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()))
                .collect(Collectors.toList());
    }

    private GeneralEventRecommendationDTO calculateEventScore(GeneralEvent event) {
        // Weights
        double wU = 0.5; // Urgency
        double wI = 0.3; // Importance
        double wD = 0.2; // Duration (Short job first)

        // 1. Urgency (U)
        double U = 0.0;
        if (event.getDeadline() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), event.getDeadline());
            int H = 30; // horizon
            U = clamp(((double) H - daysLeft) / H, 0.0, 1.0);
            if (daysLeft <= 0) U = 1.0; // Overdue is max urgency
        }

        // 2. Importance (I): 1-5 -> 0.2-1.0
        double I = event.getImportance() != null ? (event.getImportance() / 5.0) : 0.2;

        // 3. Duration (D): 1 / Duration. Normalize such that 30min = 0.5, 0min -> 1.
        // Formula: 1 / (1 + duration/30)
        double durationVal = event.getDuration() != null ? event.getDuration() : 60.0;
        double D = 1.0 / (1.0 + (durationVal / 30.0));

        double score = wU * U + wI * I + wD * D;
        String explanation = String.format("U: %.2f I: %.2f D: %.2f", U, I, D);

        return new GeneralEventRecommendationDTO(event, score, explanation);
    }

    public List<BlockRecommendationDTO> getRecommendedFiles() {
        // 1. Fetch all relevant data (N+1 optimization: fetch all at once)
        List<Block> allBlocks = blockRepository.findAll();
        Map<Long, StudyRecord> recordMap = studyRecordRepository.findAll().stream()
                .collect(Collectors.toMap(StudyRecord::getBlockId, r -> r));

        // 2. Filter files only (folders are not "reviewed" directly)
        return allBlocks.stream()
                .filter(b -> b.getType() != BlockType.FOLDER)
                .filter(b -> b.getSubjectId() != null) // hide Unassigned by default
                .map(block -> calculateFileScore(block, recordMap.get(block.getId())))
                .sorted((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore())) // Descending sort
                .limit(20) // Top 20
                .collect(Collectors.toList());
    }

    // Compute file-level score according to new spec
    private BlockRecommendationDTO calculateFileScore(Block block, StudyRecord record) {
        LocalDateTime now = LocalDateTime.now();

        // If mastered, short-circuit to 0
        if (record != null && Boolean.TRUE.equals(record.getIsMastered())) {
            return new BlockRecommendationDTO(block, 0.0,
                    record.getLastReviewTime(), "Mastered");
        }

        Subject subject = null;
        if (block.getSubjectId() != null) {
            subject = subjectRepository.findById(block.getSubjectId()).orElse(null);
        }

        // Normalization params (could be externalized to config)
        int H = 30; // DDL normalization days
        int Sh = 14; // Staleness days
        double wU = 0.4, wC = 0.2, wD = 0.2, wS = 0.2;

        // U: urgency by subject DDL
        double U = 0.0;
        if (subject != null && subject.getDdl() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), subject.getDdl());
            U = clamp(((double) H - daysLeft) / H, 0.0, 1.0);
            if (daysLeft <= 0)
                U = 1.0;
        }

        // C: credit weight (0-1)
        double C = subject != null && subject.getCreditWeight() != null ? subject.getCreditWeight() : 0.0;
        if (C > 1.0)
            C = C / 100.0;
        C = clamp(C, 0.0, 1.0);

        // D: difficulty 1-5 -> 0-1
        double Dnorm = 0.0;
        if (subject != null && subject.getDifficulty() != null) {
            Dnorm = clamp((subject.getDifficulty() - 1) / 4.0, 0.0, 1.0);
        }

        // S: staleness by last review
        double S;
        if (record == null || record.getLastReviewTime() == null) {
            S = 1.0;
        } else {
            long daysSince = ChronoUnit.DAYS.between(record.getLastReviewTime(), now);
            S = clamp(daysSince / (double) Sh, 0.0, 1.0);
        }

        double R_file = wU * U + wC * C + wD * Dnorm + wS * S;

        String explain = String.format("U: %.2f C: %.2f D: %.2f S: %.2f", U, C, Dnorm, S);

        return new BlockRecommendationDTO(
                block,
                R_file,
                record != null ? record.getLastReviewTime() : null,
                explain);
    }

    public List<SubjectRecommendationDTO> getRecommendedSubjects() {
        List<Block> allBlocks = blockRepository.findAll().stream()
                .filter(b -> b.getType() != BlockType.FOLDER)
                .collect(Collectors.toList());
        Map<Long, StudyRecord> recordMap = studyRecordRepository.findAll().stream()
                .collect(Collectors.toMap(StudyRecord::getBlockId, r -> r));
        Map<Long, Subject> subjectMap = subjectRepository.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        // group files by subjectId
        Map<Long, List<Block>> bySubject = allBlocks.stream()
                .filter(b -> b.getSubjectId() != null)
                .collect(Collectors.groupingBy(Block::getSubjectId));

        int Th = 600; // total time threshold (minutes)
        double k = 0.2; // workload factor

        List<SubjectRecommendationDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Block>> entry : bySubject.entrySet()) {
            Long subjectId = entry.getKey();
            Subject subject = subjectMap.get(subjectId);
            if (subject == null)
                continue;

            List<Block> files = entry.getValue();
            List<BlockRecommendationDTO> fileScores = files.stream()
                    .map(b -> calculateFileScore(b, recordMap.get(b.getId())))
                    .sorted((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()))
                    .collect(Collectors.toList());

            double num = 0.0;
            double den = 0.0;
            int totalMins = 0;
            for (Block b : files) {
                int t = b.getEstimatedReviewTimeMinutes() != null ? b.getEstimatedReviewTimeMinutes() : 30;
                totalMins += t;
                // find corresponding score
                BlockRecommendationDTO dto = fileScores.stream()
                        .filter(x -> Objects.equals(x.getBlock().getId(), b.getId()))
                        .findFirst().orElse(null);
                if (dto != null) {
                    num += t * dto.getPriorityScore();
                    den += t;
                }
            }
            double R_base = den > 0 ? (num / den) : 0.0;
            double workloadFactor = 1 + k * clamp(totalMins / (double) Th, 0.0, 1.0);
            double R_subject = R_base * workloadFactor;

            // take top 5 files for display
            List<BlockRecommendationDTO> topFiles = fileScores.stream().limit(5).collect(Collectors.toList());
            result.add(new SubjectRecommendationDTO(subject, R_subject, totalMins, topFiles));
        }

        // sort descending by subject score
        result.sort((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()));
        return result;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
