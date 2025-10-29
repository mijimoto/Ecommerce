package com.mijimoto.ECommerce.common.file.repositories;

import com.mijimoto.ECommerce.common.file.entities.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByStorageKey(String storageKey);
    List<FileEntity> findByOwnerId(Long ownerId);
}
