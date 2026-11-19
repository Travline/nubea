package com.ecommerce.backend.identidad.service;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class InicializacionAdministrador implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    @Value("${app.bootstrap-admin-email:}") private String email;

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) return;
        transacciones.executeWithoutResult(status -> {
            jdbc.execute("SELECT pg_advisory_xact_lock(491860713)");
            Integer total = jdbc.queryForObject("SELECT count(*) FROM usuario_rol ur JOIN rol r ON r.id_rol = ur.id_rol WHERE r.nombre = 'ADMIN_GENERAL'", Integer.class);
            if (total != null && total > 0) return;
            int inserted = jdbc.update("INSERT INTO usuario_rol (id_usuario, id_rol) SELECT u.id_usuario, r.id_rol FROM usuario_plataforma u CROSS JOIN rol r WHERE u.email = ? AND u.estado = 'ACTIVO' AND r.nombre = 'ADMIN_GENERAL' ON CONFLICT DO NOTHING", email.trim().toLowerCase(Locale.ROOT));
            if (inserted != 1) throw new IllegalStateException("La cuenta activa de administrador inicial no existe");
        });
    }
}
