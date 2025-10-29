package com.mijimoto.ECommerce.common.storage.services;

import com.mijimoto.ECommerce.common.file.entities.FileEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.io.InputStream;
import java.util.List;
import java.net.URL;

public interface StorageService {

    FileEntity store(MultipartFile file, Long ownerId, boolean isPublic) throws Exception;

    Optional<FileEntity> getFile(Long id);

    Resource downloadAsResource(FileEntity file) throws Exception;

    URL presignedUrl(FileEntity file, int expirySeconds) throws Exception;

    void delete(FileEntity file) throws Exception;

    List<FileEntity> listByOwner(Long ownerId);
}
