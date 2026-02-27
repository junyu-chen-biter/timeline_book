package com.example.pkms.controller;

import com.example.pkms.dto.SubjectRecommendationDTO;
import com.example.pkms.entity.Subject;
import com.example.pkms.repository.SubjectRepository;
import com.example.pkms.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final RecommendationService recommendationService;

    @GetMapping
    public List<Subject> list() {
        return subjectRepository.findAll();
    }

    @PostMapping
    public Subject create(@RequestBody Subject subject) {
        // Normalize and validate
        if (subject.getName() == null || subject.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("科目名不能为空");
        }
        if (subject.getDdl() == null || !subject.getDdl().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("DDL 必须是未来日期");
        }
        if (subject.getCreditWeight() == null) subject.setCreditWeight(0.0);
        // If client sends 0-100, convert to 0-1
        if (subject.getCreditWeight() > 1.0) {
            subject.setCreditWeight(subject.getCreditWeight() / 100.0);
        }
        if (subject.getDifficulty() == null || subject.getDifficulty() < 1 || subject.getDifficulty() > 5) {
            throw new IllegalArgumentException("难度必须在 1-5");
        }
        return subjectRepository.save(subject);
    }

    @PatchMapping("/{id}")
    public Subject update(@PathVariable Long id, @RequestBody Subject updates) {
        return subjectRepository.findById(id).map(s -> {
            if (updates.getName() != null) s.setName(updates.getName());
            if (updates.getDdl() != null) s.setDdl(updates.getDdl());
            if (updates.getCreditWeight() != null) {
                Double cw = updates.getCreditWeight();
                s.setCreditWeight(cw > 1.0 ? cw / 100.0 : cw);
            }
            if (updates.getDifficulty() != null) s.setDifficulty(updates.getDifficulty());
            return subjectRepository.save(s);
        }).orElseThrow(() -> new RuntimeException("Subject not found"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        subjectRepository.deleteById(id);
    }

    @GetMapping("/recommended")
    public List<SubjectRecommendationDTO> recommended() {
        return recommendationService.getRecommendedSubjects();
    }
}

