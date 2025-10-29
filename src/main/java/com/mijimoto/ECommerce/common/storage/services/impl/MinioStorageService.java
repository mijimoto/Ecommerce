package com.mijimoto.ECommerce.common.storage.services.impl;

import com.mijimoto.ECommerce.common.file.entities.FileEntity;
import com.mijimoto.ECommerce.common.file.repositories.FileRepository;
import com.mijimoto.ECommerce.common.storage.services.StorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final FileRepository fileRepository;
    private final String bucket;
    private final String minioUrl;

    public MinioStorageService(MinioClient minioClient,
                               FileRepository fileRepository,
                               @Value("${minio.bucket}") String bucket,
                               @Value("${minio.url}") String minioUrl) {
        this.minioClient = minioClient;
        this.fileRepository = fileRepository;
        this.bucket = bucket;
        this.minioUrl = minioUrl != null ? minioUrl.replaceAll("/+$", "") : null;
    }

    @Override
    public FileEntity store(MultipartFile file, Long ownerId, boolean isPublic) throws Exception {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx >= 0) ext = original.substring(idx);

        String storageKey = buildStorageKey(ownerId, ext);

        // compute checksum (sha-256) while streaming
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        String checksum = bytesToHex(md.digest());

        // Upload to MinIO (open new stream)
        try (InputStream uploadStream = file.getInputStream()) {
            PutObjectArgs putArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storageKey)
                    .stream(uploadStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(putArgs);
        }

        // Build file entity
        FileEntity entity = FileEntity.builder()
                .filename(original)
                .storageKey(storageKey)
                .bucket(bucket)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .checksum(checksum)
                .ownerId(ownerId)
                .uploadedAt(OffsetDateTime.now())
                .isPublic(isPublic)
                .build();

        // Build public URL from configured endpoint (do NOT call SDK internals)
        if (isPublic && minioUrl != null) {
            entity.setUrl(String.format("%s/%s/%s", minioUrl, bucket, storageKey));
        }

        return fileRepository.save(entity);
    }

    @Override
    public Optional<FileEntity> getFile(Long id) {
        return fileRepository.findById(id);
    }

    @Override
    public Resource downloadAsResource(FileEntity file) throws Exception {
        InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(file.getBucket())
                        .object(file.getStorageKey())
                        .build()
        );
        return new InputStreamResource(is);
    }

    @Override
    public URL presignedUrl(FileEntity file, int expirySeconds) throws Exception {
        // Use presigned GET url provided by the SDK
        String urlStr = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(file.getBucket())
                        .object(file.getStorageKey())
                        .expiry(expirySeconds)
                        .build()
        );
        return new URL(urlStr);
    }

    @Override
    public void delete(FileEntity file) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(file.getBucket())
                .object(file.getStorageKey())
                .build());
        fileRepository.delete(file);
    }

    @Override
    public List<FileEntity> listByOwner(Long ownerId) {
        return fileRepository.findByOwnerId(ownerId);
    }

    // helpers
    private String buildStorageKey(Long ownerId, String ext) {
        String uuid = UUID.randomUUID().toString();
        String prefix = ownerId != null ? "users/" + ownerId : "system";
        return String.format("%s/%s%s", prefix, uuid, ext);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
