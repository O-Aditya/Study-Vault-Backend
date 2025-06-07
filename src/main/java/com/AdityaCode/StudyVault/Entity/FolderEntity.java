package com.AdityaCode.StudyVault.Entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "folders")
public class FolderEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String folderName;
    private String folderPath; // Path to the folder in the file system
    private Long parentFolderId; // null for root folders
    private LocalDateTime createdAt;


    public FolderEntity() {
        // Default constructor
    }

    public FolderEntity( String folderName, String folderPath,  Long parentFolderId) {

        this.folderName = folderName;
        this.folderPath = folderPath;

        this.parentFolderId = parentFolderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(Long parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
}
