package com.mijimoto.ECommerce.common.file.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "files", indexes = {
        @Index(name = "ix_files_storage_key", columnList = "storageKey"),
        @Index(name = "ix_files_owner_id", columnList = "ownerId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // original filename uploaded by user
    @Column(nullable = false)
    private String filename;

    // internal object key in MinIO (e.g. product/2025/10/uuid.ext)
    @Column(nullable = false, unique = true)
    private String storageKey;

    // optional bucket (useful if supporting multi-bucket)
    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    // public URL (if publicly accessible) or signed URL can be generated dynamically
    @Column(length = 2000)
    private String url;

    // owner user id (nullable for system files)
    private Long ownerId;

    // JSON metadata (optional)
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String metadata;

    // checksum (sha256)
    @Column(length = 128)
    private String checksum;

    // whether the object is meant to be public
    @Column(nullable = false)
    private boolean isPublic = false;

    // created/updated
    @Column(nullable = false)
    private OffsetDateTime uploadedAt;
}
