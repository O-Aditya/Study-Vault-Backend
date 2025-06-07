package com.AdityaCode.StudyVault.Controller;



import com.AdityaCode.StudyVault.Entity.FileEntity;
import com.AdityaCode.StudyVault.Entity.FolderEntity;
import com.AdityaCode.StudyVault.Entity.ShareLinkEntity;
import com.AdityaCode.StudyVault.Repository.ShareLinkRepo;
import com.AdityaCode.StudyVault.Services.FileServiceStorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {

    @Autowired
    private ShareLinkRepo shareLinkRepository;

    private final FileServiceStorage fileServiceStorage;

    public FileController(FileServiceStorage fileServiceStorage) {
        this.fileServiceStorage = fileServiceStorage;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "parentFolderId", required = false) Long parentFolderId) {
        try {
            String response = fileServiceStorage.saveFile(file, parentFolderId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed: " + e.getMessage());
        }
    }

@PostMapping("/folder")
public ResponseEntity<String> createFolder(@RequestBody FolderRequest request) {
    try {
        FolderEntity folder = fileServiceStorage.createFolder(request.getFolderName(), request.getParentFolderId());
        return ResponseEntity.ok("Folder created successfully: " + folder.getFolderName());
    } catch (IOException | IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Folder creation failed: " + e.getMessage());
    }
}

    @GetMapping("/path/{folderId}")
    public ResponseEntity<List<Map<String, Object>>> getFolderPath(@PathVariable Long folderId) {
        try {
            List<Map<String, Object>> path = fileServiceStorage.getFolderPath(folderId);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long folderId) {
        try {
            fileServiceStorage.deleteFolder(folderId);
            return ResponseEntity.ok().body(Map.of("message", "Folder deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete folder: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            FileEntity fileEntity = fileServiceStorage.getFileById(id);
            Path path = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFilesAndFolders(@RequestParam(value = "parentFolderId", required = false) Long parentFolderId) {
        List<FileEntity> files = fileServiceStorage.getFilesByParentFolderId(parentFolderId);
        List<FolderEntity> folders = fileServiceStorage.getFoldersByParentFolderId(parentFolderId);
        Map<String, Object> response = new HashMap<>();
        response.put("files", files);
        response.put("folders", folders);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        try {
            fileServiceStorage.deleteFile(id);
            return ResponseEntity.ok("File deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File deletion failed: " + e.getMessage());
        }
    }

    @PostMapping("/share")
    public ResponseEntity<ShareLinkEntity> createShareLink(@RequestBody ShareRequest request) {
        try {
            ShareLinkEntity link = new ShareLinkEntity();
            link.setFileId(request.getFileId());
            link.setPassword(request.getPassword());
            link.setShareLink("http://localhost:8080/files/" + UUID.randomUUID().toString());
            link.setExpiryDate(LocalDateTime.now().plusDays(request.getExpiryDays()));
            shareLinkRepository.save(link);
            return ResponseEntity.ok(link);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ShareLinkEntity()); // Simplified for error case
        }
    }

    @GetMapping("/share/{linkId}")
    public ResponseEntity<Resource> accessSharedFile(@PathVariable String linkId, @RequestParam String password) {
        try {
            ShareLinkEntity link = shareLinkRepository.findByShareLinkContains(linkId)
                    .orElseThrow(() -> new RuntimeException("Invalid link"));
            if (!link.getPassword().equals(password)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            if (link.getExpiryDate().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.GONE).build();
            }
            FileEntity fileEntity = fileServiceStorage.getFileById(Long.parseLong(link.getFileId()));
            Path path = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }





}

class FolderRequest {
    private String folderName;
    private Long parentFolderId;

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    public Long getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(Long parentFolderId) { this.parentFolderId = parentFolderId; }
}

class ShareRequest {
    private String fileId;
    private String password;
    private int expiryDays;

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getExpiryDays() { return expiryDays; }
    public void setExpiryDays(int expiryDays) { this.expiryDays = expiryDays; }
}
