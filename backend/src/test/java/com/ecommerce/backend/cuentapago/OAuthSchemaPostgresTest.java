package com.ecommerce.backend.cuentapago;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "OAUTH_PG_TEST", matches = "true")
class OAuthSchemaPostgresTest {
    @Test
    void flywayCreaTablasDeOAuthEHistorial() throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"));
             var s = c.createStatement();
             var rs = s.executeQuery("SELECT to_regclass('oauth_mercado_pago_intento'), to_regclass('cuenta_pago_historial')")) {
            assertTrue(rs.next());
            assertNotNull(rs.getString(1));
            assertNotNull(rs.getString(2));
        }
    }
}
