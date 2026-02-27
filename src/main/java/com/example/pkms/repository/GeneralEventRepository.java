package com.example.pkms.repository;

import com.example.pkms.entity.GeneralEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneralEventRepository extends JpaRepository<GeneralEvent, Long> {
    List<GeneralEvent> findByStatus(GeneralEvent.EventStatus status);
}
