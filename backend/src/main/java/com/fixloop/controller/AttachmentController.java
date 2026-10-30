package com.fixloop.controller;

import com.fixloop.domain.Attachment;
import com.fixloop.domain.Complaint;
import com.fixloop.dto.complaint.AttachmentDto;
import com.fixloop.repository.AttachmentRepository;
import com.fixloop.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentRepository attachmentRepo;
    private final ComplaintRepository complaintRepo;

    @Value("${fixloop.uploads.base-path:uploads/attachments}")
    private String uploadBasePath;

    @PostMapping("/api/complaints/{id}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','STUDENT')")
    @Transactional
    public ResponseEntity<AttachmentDto> uploadAttachment(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found: " + id));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot upload empty file");
        }

        // 1. Hard cap file size: 10MB
        final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10MB limit");
        }

        // 2. Validate Extension allowlist
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        String cleanFilename = Paths.get(originalFilename).getFileName().toString();
        int dotIndex = cleanFilename.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have a valid extension");
        }
        String ext = cleanFilename.substring(dotIndex + 1).toLowerCase();
        Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "webp", "pdf", "txt", "docx");
        if (!allowedExtensions.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File extension '." + ext + "' is not permitted. Allowed: " + allowedExtensions);
        }

        // 3. Validate MIME type allowlist
        String contentType = file.getContentType();
        Set<String> allowedMimes = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "application/pdf", "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        if (contentType == null || !allowedMimes.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MIME type '" + contentType + "' is not permitted. Allowed: images (JPG, PNG, WEBP), PDF, TXT, DOCX");
        }

        try {
            // Ensure target directory exists
            Path uploadDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            // Generate collision-resistant unique stored name
            String storedFilename = UUID.randomUUID() + "_" + cleanFilename;

            Path targetPath = uploadDir.resolve(storedFilename).normalize();
            if (!targetPath.startsWith(uploadDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path traversal");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String mimeType = contentType;

            Attachment attachment = Attachment.builder()
                    .complaint(complaint)
                    .fileName(cleanFilename)
                    .filePath(targetPath.toString())
                    .mimeType(mimeType)
                    .fileSizeBytes(file.getSize())
                    .build();

            Attachment saved = attachmentRepo.save(attachment);

            AttachmentDto dto = AttachmentDto.builder()
                    .id(saved.getId())
                    .fileName(saved.getFileName())
                    .mimeType(saved.getMimeType())
                    .fileUrl("/api/attachments/" + saved.getId())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IOException e) {
            log.error("Failed to store uploaded attachment for complaint {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save uploaded file");
        }
    }

    @GetMapping("/api/attachments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Integer id) {
        Attachment attachment = attachmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found: " + id));

        File file = new File(attachment.getFilePath());
        if (!file.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored file missing from disk");
        }

        Resource resource = new FileSystemResource(file);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (attachment.getMimeType() != null) {
                mediaType = MediaType.parseMediaType(attachment.getMimeType());
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }
}
