package com.ecommerce.backend.banner.listener;

import com.ecommerce.backend.banner.event.BannerStorageCleanupEvent;
import com.ecommerce.backend.shared.storage.BannerStoragePort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BannerStorageCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(BannerStorageCleanupListener.class);

    private final BannerStoragePort bannerStoragePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void procesarLimpiezaPostCommit(BannerStorageCleanupEvent event) {
        log.info("Iniciando limpieza asíncrona post-commit de objeto en storage con key: {}", event.storageKey());
        bannerStoragePort.eliminar(event.storageKey());
    }
}
