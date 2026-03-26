package com.renda.merchantops.api.importjob;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface ImportFileStorageService {

    String store(Long tenantId, MultipartFile file);

    String store(Long tenantId, String filename, InputStream inputStream);

    InputStream openStream(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
