package com.example.pkms.repository;

import com.example.pkms.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
    
    // Find the study record for a specific block (file)
    Optional<StudyRecord> findByBlockId(Long blockId);
}