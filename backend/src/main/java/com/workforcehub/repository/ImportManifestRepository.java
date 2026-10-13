package com.workforcehub.repository;

import com.workforcehub.entity.ImportManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImportManifestRepository extends JpaRepository<ImportManifest, UUID> {
    List<ImportManifest> findByJobId(UUID jobId);
    List<ImportManifest> findByEntityTypeAndTargetId(String entityType, UUID targetId);
}
