package com.example.pkms.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "subjects", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name" })
})
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate ddl;

    // 学分占比，0-1 范围（若前端传入 0-100 会在接口层转换）
    @Column(nullable = false)
    private Double creditWeight;

    // 难度 1-5
    @Column(nullable = false)
    private Integer difficulty;

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
