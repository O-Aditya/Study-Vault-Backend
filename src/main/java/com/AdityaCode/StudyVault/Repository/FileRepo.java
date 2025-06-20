package com.AdityaCode.StudyVault.Repository;

import com.AdityaCode.StudyVault.Entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepo extends JpaRepository<FileEntity,Long> {
    List<FileEntity> findByParentFolderId(Long parentFolderId);
    List<FileEntity> findByParentFolderIdIsNull();
}
