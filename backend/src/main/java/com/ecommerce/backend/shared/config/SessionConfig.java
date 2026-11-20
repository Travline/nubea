package com.ecommerce.backend.shared.config;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 7200)
public class SessionConfig {
    @Bean
    CookieSerializer cookieSerializer(@Value("${server.servlet.session.cookie.secure:true}") boolean secure,
                                      @Value("${server.servlet.session.cookie.same-site:lax}") String sameSite) {
        var normal = cookie(secure, sameSite, -1);
        var persistente = cookie(secure, sameSite, 86400);
        return new CookieSerializer() {
            @Override public List<String> readCookieValues(HttpServletRequest request) { return normal.readCookieValues(request); }
            @Override public void writeCookieValue(CookieValue value) {
                var session = value.getRequest().getSession(false);
                if (session != null && Boolean.TRUE.equals(session.getAttribute("recordarme"))) persistente.writeCookieValue(value);
                else normal.writeCookieValue(value);
            }
        };
    }
    private static DefaultCookieSerializer cookie(boolean secure, String sameSite, int age) {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite(sameSite);
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge(age);
        return serializer;
    }
    public static class ExpiracionAbsoluta extends OncePerRequestFilter {
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            var session = request.getSession(false);
            if (session != null && session.getAttribute("venceAbsoluto") instanceof Long vence && System.currentTimeMillis() >= vence) session.invalidate();
            chain.doFilter(request, response);
        }
    }
}
