package com.ecommerce.backend.shared.storage;

import java.io.InputStream;

public interface StoragePort {
    void subir(String storageKey, InputStream contenido, long contentLength, String contentType);
    void eliminar(String storageKey);
}
