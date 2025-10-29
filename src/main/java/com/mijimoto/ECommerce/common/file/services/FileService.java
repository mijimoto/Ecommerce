package com.mijimoto.ECommerce.common.file.services;

import com.mijimoto.ECommerce.common.file.entities.FileEntity;
import com.mijimoto.ECommerce.common.file.repositories.FileRepository;
import com.mijimoto.ECommerce.common.storage.services.StorageService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class FileService {
    private final StorageService storageService;
    private final FileRepository fileRepository;

    public FileService(StorageService storageService, FileRepository fileRepository) {
        this.storageService = storageService;
        this.fileRepository = fileRepository;
    }

    public FileEntity upload(MultipartFile file, Long ownerId, boolean isPublic) throws Exception {
        return storageService.store(file, ownerId, isPublic);
    }

    public FileEntity get(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    public List<FileEntity> listByOwner(Long ownerId) {
        return storageService.listByOwner(ownerId);
    }

    public void delete(FileEntity file) throws Exception {
        storageService.delete(file);
    }
}
