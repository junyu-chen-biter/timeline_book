package com.example.pkms.repository;

import com.example.pkms.entity.Block;
import com.example.pkms.entity.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    
    // Find all blocks within a specific parent folder (for lazy loading the tree)
    List<Block> findByParentId(Long parentId);

    // Find all root blocks (where parentId is null)
    List<Block> findByParentIdIsNull();

    // Find all files (non-folders) - useful for global search or statistics
    List<Block> findByTypeNot(BlockType type);
}