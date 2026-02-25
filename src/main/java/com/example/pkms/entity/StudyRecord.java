package com.example.pkms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "study_records")
public class StudyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long blockId;

    private LocalDateTime lastReviewTime;

    @Lob
    private String note; // Markdown or HTML notes

    private Integer reviewCount = 0;

    @Column(columnDefinition = "integer default 100")
    private Integer lastReviewProgress = 100;

    @Column(columnDefinition = "boolean default false")
    private Boolean isMastered = false;
}