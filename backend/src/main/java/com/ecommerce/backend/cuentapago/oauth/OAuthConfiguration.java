package com.ecommerce.backend.cuentapago.oauth;

import com.ecommerce.backend.cuentapago.vault.AesGcmCredentialVault;
import com.ecommerce.backend.cuentapago.vault.PaymentCredentialVault;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import java.util.Arrays;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "mercadopago.oauth", name = "enabled", havingValue = "true")
    PaymentCredentialVault paymentCredentialVault(OAuthProperties properties, Environment environment) {
        properties.validar();
        if (properties.allowInsecureLocalhost() && !Arrays.asList(environment.getActiveProfiles()).contains("local"))
            throw new IllegalStateException("HTTP local solo está permitido con el perfil local");
        return new AesGcmCredentialVault(properties.activeKey(), properties.keys());
    }
}
