package com.ecommerce.backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.HttpMethod;
import com.ecommerce.backend.shared.security.ActiveUserFilter;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;

@Configuration 
@EnableMethodSecurity
public class SecurityConfig {

    //Cifrado de Contraseña con Argon es mas seguro que Bycript.
    @Bean 
    public PasswordEncoder passwordEncoder(){
               return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(); 
    }

    @Bean 
    public SecurityFilterChain filterChain(HttpSecurity http, UsuarioPlataformaRepository usuarios,
            @Value("${server.servlet.session.cookie.secure:true}") boolean cookieSecure,
            @Value("${server.servlet.session.cookie.same-site:lax}") String cookieSameSite) throws Exception{
        var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite(cookieSameSite));
        http
            .addFilterAfter(new ActiveUserFilter(usuarios), SecurityContextHolderFilter.class)
            .cors(cors -> {})
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
            .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
                .requestMatchers("/error", "/health").permitAll()
                .requestMatchers("/api/auth/**", "/api/integraciones/mercado-pago/oauth/callback").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tiendas/{slug}").permitAll()
                .anyRequest().authenticated()
            );
           return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:}") String origins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
