package com.renda.merchantops.api.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface ImportFileStorageService {

    String store(Long tenantId, MultipartFile file);

    InputStream openStream(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
