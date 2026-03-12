package com.renda.merchantops.api.service;

import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalImportFileStorageService implements ImportFileStorageService {

    private final Path rootPath;

    public LocalImportFileStorageService(@Value("${merchantops.import.storage.local-dir:data/imports}") String localDir) {
        this.rootPath = Paths.get(localDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(Long tenantId, MultipartFile file) {
        if (tenantId == null || file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import file must not be empty");
        }
        try {
            String filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "upload.csv";
            String storageKey = tenantId + "/" + UUID.randomUUID() + "-" + filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = rootPath.resolve(storageKey).normalize();
            if (!target.startsWith(rootPath)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "invalid file path");
            }
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "failed to store import file");
        }
    }

    @Override
    public InputStream openStream(String storageKey) throws IOException {
        return Files.newInputStream(resolveStoredPath(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveStoredPath(storageKey));
    }

    private Path resolveStoredPath(String storageKey) throws IOException {
        if (!StringUtils.hasText(storageKey)) {
            throw new IOException("invalid storage key");
        }
        Path target = rootPath.resolve(storageKey).normalize();
        if (!target.startsWith(rootPath)) {
            throw new IOException("invalid storage key");
        }
        return target;
    }
}
