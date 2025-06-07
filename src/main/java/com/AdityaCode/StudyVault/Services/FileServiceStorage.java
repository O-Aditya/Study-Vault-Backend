package com.AdityaCode.StudyVault.Services;

import com.AdityaCode.StudyVault.Entity.FileEntity;
import com.AdityaCode.StudyVault.Entity.FolderEntity;
import com.AdityaCode.StudyVault.Repository.FileRepo;
import com.AdityaCode.StudyVault.Repository.FolderRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileServiceStorage {
    private static final Logger logger = LoggerFactory.getLogger(FileServiceStorage.class);
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "application/pdf", "image/jpeg", "image/png", "text/plain"
    );

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final FileRepo fileRepo;
    private final FolderRepo folderRepo;

    public FileServiceStorage(FileRepo fileRepo, FolderRepo folderRepo) {
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
    }

    public FolderEntity createFolder(String folderName, Long parentFolderId) throws IOException {
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name is required");
        }

        // Validate parent folder if provided
        Path parentPath = Paths.get(System.getProperty("user.dir"), uploadDir);
        if (parentFolderId != null) {
            FolderEntity parentFolder = folderRepo.findById(parentFolderId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
            parentPath = Paths.get(parentFolder.getFolderPath());
        }

        // Create folder path
        Path folderPath = parentPath.resolve(folderName);
        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        // Save folder metadata
        FolderEntity folderEntity = new FolderEntity(folderName, folderPath.toString(), parentFolderId);
        folderRepo.save(folderEntity);
        logger.info("Folder created: {}", folderName);
        return folderEntity;
    }



    public List<Map<String, Object>> getFolderPath(Long folderId) {
        List<Map<String, Object>> path = new ArrayList<>();
        FolderEntity currentFolder = folderRepo.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + folderId));

        // Add current folder
        Map<String, Object> currentFolderMap = new HashMap<>();
        currentFolderMap.put("id", currentFolder.getId());
        currentFolderMap.put("name", currentFolder.getFolderName());
        path.add(currentFolderMap);

        // Traverse up the folder hierarchy
        Long parentId = currentFolder.getParentFolderId();
        while (parentId != null) {
            Long finalParentId = parentId;
            FolderEntity parentFolder = folderRepo.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found with id: " + finalParentId));

            Map<String, Object> parentFolderMap = new HashMap<>();
            parentFolderMap.put("id", parentFolder.getId());
            parentFolderMap.put("name", parentFolder.getFolderName());
            path.add(0, parentFolderMap); // Add to the beginning of the list

            parentId = parentFolder.getParentFolderId();
        }

        return path;
    }


    @Transactional
    public void deleteFolder(Long folderId) {
        FolderEntity folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found with id: " + folderId));

        // Check if folder is empty
        List<FileEntity> files = fileRepo.findByParentFolderId(folderId);
        List<FolderEntity> subfolders = folderRepo.findByParentFolderId(folderId);

        if (!files.isEmpty() || !subfolders.isEmpty()) {
            throw new IllegalStateException("Cannot delete non-empty folder. Please delete all contents first.");
        }

        try {
            // Delete the folder from the database
            folderRepo.delete(folder);
            logger.info("Folder deleted successfully: {}", folderId);
        } catch (Exception e) {
            logger.error("Error deleting folder: {}", folderId, e);
            throw new RuntimeException("Failed to delete folder: " + e.getMessage());
        }
    }

    // Helper method to recursively delete folder contents (if needed in the future)
    @Transactional
    public void deleteFolderRecursive(Long folderId) {
        FolderEntity folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found with id: " + folderId));

        // Delete all files in the folder
        List<FileEntity> files = fileRepo.findByParentFolderId(folderId);
        for (FileEntity file : files) {
            try {
                // Delete physical file
                Path filePath = Paths.get(uploadDir, file.getFileName());
                Files.deleteIfExists(filePath);
                // Delete file record
                fileRepo.delete(file);
            } catch (IOException e) {
                logger.error("Error deleting file: {}", file.getFileName(), e);
            }
        }

        // Recursively delete subfolders
        List<FolderEntity> subfolders = folderRepo.findByParentFolderId(folderId);
        for (FolderEntity subfolder : subfolders) {
            deleteFolderRecursive(subfolder.getId());
        }

        // Delete the folder itself
        folderRepo.delete(folder);
        logger.info("Folder and its contents deleted successfully: {}", folderId);
    }




    public String saveFile(MultipartFile file, Long parentFolderId) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            String fileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("File name is invalid");
            }
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("File size exceeds 10MB limit");
            }

            // Determine upload path based on parent folder
            Path uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir);
            if (parentFolderId != null) {
                FolderEntity parentFolder = folderRepo.findById(parentFolderId)
                        .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
                uploadPath = Paths.get(parentFolder.getFolderPath());
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setFilePath(filePath.toString());
            fileEntity.setFileType(file.getContentType());
            fileEntity.setSize(file.getSize());
            fileEntity.setParentFolderId(parentFolderId);
            fileEntity.setCreatedAt(LocalDateTime.now());

            fileRepo.save(fileEntity);
            logger.info("File uploaded: {}", fileName);
            return "File uploaded successfully: " + fileName;
        } catch (IOException | IllegalArgumentException e) {
            logger.error("File upload failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    public List<FileEntity> getFilesByParentFolderId(Long parentFolderId) {
        return parentFolderId == null
                ? fileRepo.findByParentFolderIdIsNull()
                : fileRepo.findByParentFolderId(parentFolderId);
    }

    public List<FolderEntity> getFoldersByParentFolderId(Long parentFolderId) {
        return parentFolderId == null
                ? folderRepo.findByParentFolderIdIsNull()
                : folderRepo.findByParentFolderId(parentFolderId);
    }

    public FileEntity getFileById(Long fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
    }

    public void deleteFile(Long id) {
        FileEntity fileEntity = getFileById(id);
        Path filePath = Paths.get(fileEntity.getFilePath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Failed to delete file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Failed to delete file from disk", e);
        }
        fileRepo.deleteById(id);
        logger.info("File deleted: {}", fileEntity.getFileName());
    }
}