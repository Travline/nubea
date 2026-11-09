package com.ecommerce.backend.cuentapago;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "OAUTH_PG_TEST", matches = "true")
class OAuthSchemaPostgresTest {
    @Test
    void inicializacionYMigrationDefinenMismoEsquemaOAuth() throws Exception {
        String nuevo = "oauth_init_" + UUID.randomUUID().toString().replace("-", "");
        String anterior = "oauth_mig_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD")); Statement s = c.createStatement()) {
            try {
                try (ResultSet extensiones = s.executeQuery("SELECT count(*) FROM pg_extension WHERE extname IN ('pgcrypto','btree_gist')")) {
                    extensiones.next();
                    org.junit.jupiter.api.Assumptions.assumeTrue(extensiones.getInt(1) == 2, "Se requieren extensiones PostgreSQL instaladas");
                }
                s.execute("CREATE SCHEMA " + nuevo);
                s.execute("CREATE SCHEMA " + anterior);
                s.execute("SET search_path TO " + nuevo + ", public");
                s.execute(Files.readString(Path.of("../services/database/init/db.sql")).replace("CREATE EXTENSION IF NOT EXISTS pgcrypto;", "").replace("CREATE EXTENSION IF NOT EXISTS btree_gist;", ""));
                s.execute("SET search_path TO " + anterior + ", public");
                s.execute("CREATE TABLE usuario_plataforma(id_usuario uuid PRIMARY KEY)");
                s.execute("CREATE TABLE tienda(id_tienda uuid PRIMARY KEY)");
                s.execute(Files.readString(Path.of("../services/database/migrations/001_oauth_mercado_pago.sql")));
                try (PreparedStatement p = c.prepareStatement("""
                        SELECT column_name, data_type, is_nullable
                        FROM information_schema.columns WHERE table_schema=? AND table_name='oauth_mercado_pago_intento'
                        ORDER BY ordinal_position
                        """)) {
                    p.setString(1, nuevo); String a = columns(p);
                    p.setString(1, anterior); String b = columns(p);
                    assertFalse(a.isEmpty()); assertEquals(a, b);
                }
                try (PreparedStatement p = c.prepareStatement("SELECT count(*) FROM pg_indexes WHERE schemaname=? AND tablename='oauth_mercado_pago_intento'")) {
                    p.setString(1, nuevo); int a = count(p);
                    p.setString(1, anterior); int b = count(p);
                    assertEquals(4, a); assertEquals(a, b);
                }
            } finally {
                s.execute("SET search_path TO public");
                s.execute("DROP SCHEMA IF EXISTS " + nuevo + " CASCADE");
                s.execute("DROP SCHEMA IF EXISTS " + anterior + " CASCADE");
            }
        }
    }
    private static String columns(PreparedStatement p) throws SQLException {
        StringBuilder result = new StringBuilder();
        try (ResultSet r = p.executeQuery()) { while (r.next()) result.append(r.getString(1)).append('/').append(r.getString(2)).append('/').append(r.getString(3)).append(';'); }
        return result.toString();
    }
    private static int count(PreparedStatement p) throws SQLException { try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1); } }
}
