package com.example.pkms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "blocks")
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId; // Null for root blocks

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private BlockType type;

    private String filePath; // Relative path in uploads dir, null for FOLDER
    
    // New: Subject assignment for files
    private Long subjectId; // Required for files; null indicates Unassigned (to be migrated)
    // Estimated review time for a file (minutes)
    @Column(columnDefinition = "integer default 30")
    private Integer estimatedReviewTimeMinutes;
    
    // Legacy attributes kept for backward compatibility (mainly on Folder blocks)
    private Integer difficulty; // 1-10 (legacy)
    private Double credits;     // Credit weight (legacy)
    private LocalDate ddl;      // Deadline (legacy)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
