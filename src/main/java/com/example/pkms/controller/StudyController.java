package com.example.pkms.controller;

import com.example.pkms.entity.StudyRecord;
import com.example.pkms.repository.StudyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudyController {

    private final StudyRecordRepository studyRecordRepository;

    // Get study record for a block (file)
    @GetMapping("/{blockId}")
    public StudyRecord getRecord(@PathVariable Long blockId) {
        return studyRecordRepository.findByBlockId(blockId)
                .orElseGet(() -> {
                    StudyRecord newRecord = new StudyRecord();
                    newRecord.setBlockId(blockId);
                    return newRecord;
                });
    }

    // Save Note
    @PostMapping("/{blockId}/note")
    public StudyRecord saveNote(@PathVariable Long blockId, @RequestBody String noteContent) {
        StudyRecord record = studyRecordRepository.findByBlockId(blockId)
                .orElseGet(() -> {
                    StudyRecord newRecord = new StudyRecord();
                    newRecord.setBlockId(blockId);
                    return newRecord;
                });
        
        record.setNote(noteContent);
        return studyRecordRepository.save(record);
    }

    // Mark as Reviewed (Check-in)
    @PostMapping("/{blockId}/review")
    public StudyRecord review(@PathVariable Long blockId) {
        StudyRecord record = studyRecordRepository.findByBlockId(blockId)
                .orElseGet(() -> {
                    StudyRecord newRecord = new StudyRecord();
                    newRecord.setBlockId(blockId);
                    return newRecord;
                });
        
        record.setLastReviewTime(LocalDateTime.now());
        record.setReviewCount(record.getReviewCount() == null ? 1 : record.getReviewCount() + 1);
        
        return studyRecordRepository.save(record);
    }
}