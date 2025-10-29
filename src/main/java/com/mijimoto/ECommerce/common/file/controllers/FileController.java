package com.mijimoto.ECommerce.common.file.controllers;

import com.mijimoto.ECommerce.common.file.entities.FileEntity;
import com.mijimoto.ECommerce.common.storage.services.StorageService;
import com.mijimoto.ECommerce.common.file.services.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final StorageService storageService;

    public FileController(FileService fileService, StorageService storageService) {
        this.fileService = fileService;
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileEntity> upload(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "ownerId", required = false) Long ownerId,
                                             @RequestParam(value = "public", required = false, defaultValue = "false") boolean isPublic) throws Exception {
        FileEntity saved = fileService.upload(file, ownerId, isPublic);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileEntity> metadata(@PathVariable Long id) {
        FileEntity f = fileService.get(id);
        return f == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(f);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        FileEntity f = fileService.get(id);
        if (f == null) return ResponseEntity.notFound().build();

        Resource resource = storageService.downloadAsResource(f);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(f.getFilename()).build());
        headers.setContentType(MediaType.parseMediaType(f.getContentType()));
        headers.setContentLength(f.getSize());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<FileEntity>> listByOwner(@RequestParam(value = "ownerId", required = false) Long ownerId) {
        List<FileEntity> list = fileService.listByOwner(ownerId);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws Exception {
        FileEntity f = fileService.get(id);
        if (f == null) return ResponseEntity.notFound().build();
        fileService.delete(f);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/presign")
    public ResponseEntity<String> presign(@PathVariable Long id,
                                          @RequestParam(value = "expiry", defaultValue = "3600") int expirySeconds) throws Exception {
        FileEntity f = fileService.get(id);
        if (f == null) return ResponseEntity.notFound().build();
        URL url = storageService.presignedUrl(f, expirySeconds);
        return ResponseEntity.ok(url.toString());
    }
}
