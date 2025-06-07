package com.AdityaCode.StudyVault.Entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_links")
public class ShareLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileId;
    private String shareLink;
    private String password;
    private LocalDateTime expiryDate;


    public ShareLinkEntity() {
        // Default constructor
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getShareLink() { return shareLink; }
    public void setShareLink(String shareLink) { this.shareLink = shareLink; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

}

