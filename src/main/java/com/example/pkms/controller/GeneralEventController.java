package com.example.pkms.controller;

import com.example.pkms.dto.GeneralEventRecommendationDTO;
import com.example.pkms.entity.GeneralEvent;
import com.example.pkms.repository.GeneralEventRepository;
import com.example.pkms.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GeneralEventController {

    private final GeneralEventRepository eventRepository;
    private final RecommendationService recommendationService;

    @GetMapping
    public List<GeneralEvent> list() {
        return eventRepository.findAll();
    }

    @PostMapping
    public GeneralEvent create(@RequestBody GeneralEvent event) {
        if (event.getStatus() == null) {
            event.setStatus(GeneralEvent.EventStatus.PENDING);
        }
        return eventRepository.save(event);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GeneralEvent> update(@PathVariable Long id, @RequestBody GeneralEvent updates) {
        return eventRepository.findById(id).map(existing -> {
            if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
            if (updates.getDeadline() != null) existing.setDeadline(updates.getDeadline());
            if (updates.getImportance() != null) existing.setImportance(updates.getImportance());
            if (updates.getDuration() != null) existing.setDuration(updates.getDuration());
            if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
            return ResponseEntity.ok(eventRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recommended")
    public List<GeneralEventRecommendationDTO> recommended() {
        return recommendationService.getRecommendedEvents();
    }
}
