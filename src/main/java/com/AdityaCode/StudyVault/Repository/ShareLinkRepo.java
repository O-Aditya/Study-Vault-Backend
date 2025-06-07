package com.AdityaCode.StudyVault.Repository;

import com.AdityaCode.StudyVault.Entity.ShareLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareLinkRepo extends JpaRepository<ShareLinkEntity, Long> {
    Optional<ShareLinkEntity> findByShareLinkContains(String linkId);
}
