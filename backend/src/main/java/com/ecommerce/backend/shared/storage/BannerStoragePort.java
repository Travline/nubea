package com.ecommerce.backend.shared.storage;

import java.io.InputStream;

public interface BannerStoragePort {
    void subir(String storageKey, InputStream contenido, long contentLength, String contentType);
    void eliminar(String storageKey);
}
