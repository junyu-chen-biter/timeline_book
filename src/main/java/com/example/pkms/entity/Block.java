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

    // Subject Attributes (Usually set on Folder blocks)
    // We will need logic to inherit these if not set on the file itself
    private Integer difficulty; // 1-10
    private Double credits;     // Credit weight
    private LocalDate ddl;      // Deadline

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