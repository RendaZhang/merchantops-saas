package com.renda.merchantops.api.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImportFileStorageService {

    String store(Long tenantId, MultipartFile file);
}
