package com.example.pkms.controller;

import com.example.pkms.entity.Block;
import com.example.pkms.entity.BlockType;
import com.example.pkms.repository.BlockRepository;
import com.example.pkms.service.RecommendationService;
import com.example.pkms.dto.BlockRecommendationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
public class BlockController {

    private final BlockRepository blockRepository;
    private final RecommendationService recommendationService;

    // --- 1. Resource Management ---

    // Get Directory Tree (Lazy Loading)
    // GET /api/blocks?parentId=null (Roots) or ?parentId=1 (Children)
    @GetMapping
    public List<Block> getBlocks(@RequestParam(required = false) Long parentId) {
        if (parentId == null) {
            return blockRepository.findByParentIdIsNull();
        }
        return blockRepository.findByParentId(parentId);
    }

    // Create a Folder
    @PostMapping("/folder")
    public Block createFolder(@RequestBody Block block) {
        try {
            System.out.println("Creating folder: " + block.getName() + ", Parent: " + block.getParentId());
            block.setType(BlockType.FOLDER);
            // Ensure defaults
            if (block.getDifficulty() == null) block.setDifficulty(1);
            if (block.getCredits() == null) block.setCredits(1.0);
            return blockRepository.save(block);
        } catch (Exception e) {
            e.printStackTrace(); // Print full stack trace to console
            throw new RuntimeException("Error creating folder: " + e.getMessage());
        }
    }

    // Upload a File
    @PostMapping("/upload")
    public Block uploadFile(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "parentId", required = false) Long parentId) throws IOException {
        
        try {
            System.out.println("Uploading file: " + file.getOriginalFilename() + ", Size: " + file.getSize());

            // 1. Save file to disk
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) throw new IllegalArgumentException("Filename cannot be null");
            
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex + 1).toLowerCase();
            }
            
            // Simple mapping for demo
            BlockType type = switch (extension) {
                case "pdf" -> BlockType.PDF;
                case "md", "markdown" -> BlockType.MARKDOWN;
                case "ppt", "pptx" -> BlockType.PPT;
                default -> BlockType.PDF; // Default fallback instead of throwing error for better UX
            };

            // Ensure upload directory exists - Use absolute path to be safe
            Path uploadPath = Paths.get("uploads").toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String storedFileName = System.currentTimeMillis() + "_" + originalFilename;
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 2. Save metadata to DB
            Block block = new Block();
            block.setName(originalFilename);
            block.setType(type);
            block.setParentId(parentId);
            block.setFilePath(targetLocation.toString());
            
            // Default attributes
            block.setDifficulty(1);
            block.setCredits(1.0);

            return blockRepository.save(block);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    // Update Attributes (Difficulty, Credits, DDL)
    @PatchMapping("/{id}")
    public Block updateBlock(@PathVariable Long id, @RequestBody Block updates) {
        return blockRepository.findById(id).map(block -> {
            if (updates.getName() != null) block.setName(updates.getName());
            if (updates.getDifficulty() != null) block.setDifficulty(updates.getDifficulty());
            if (updates.getCredits() != null) block.setCredits(updates.getCredits());
            if (updates.getDdl() != null) block.setDdl(updates.getDdl());
            return blockRepository.save(block);
        }).orElseThrow(() -> new RuntimeException("Block not found"));
    }

    // --- 2. File Download ---

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = Paths.get("uploads").resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                // Determine content type
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- 3. Recommendation ---

    @GetMapping("/recommended")
    public List<BlockRecommendationDTO> getRecommended() {
        return recommendationService.getRecommendedFiles();
    }
}

