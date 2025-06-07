package com.AdityaCode.StudyVault.Repository;

import com.AdityaCode.StudyVault.Entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepo extends JpaRepository<FolderEntity, Long> {
    // Additional query methods can be defined here if needed
    List<FolderEntity> findByParentFolderId(Long parentFolderId);
    List<FolderEntity> findByParentFolderIdIsNull();
}
