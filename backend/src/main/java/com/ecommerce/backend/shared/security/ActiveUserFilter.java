package com.ecommerce.backend.shared.security;

import com.ecommerce.backend.identidad.model.EstadoUsuario;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveUserFilter extends OncePerRequestFilter {
    private final UsuarioPlataformaRepository usuarios;

    public ActiveUserFilter(UsuarioPlataformaRepository usuarios) { this.usuarios = usuarios; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID id) {
            var usuario = usuarios.findById(id).orElse(null);
            if (usuario == null || usuario.getEstado() != EstadoUsuario.ACTIVO) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
            } else {
                var roles = usuario.getRoles().stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name())).toList();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(id, null, roles));
            }
        }
        chain.doFilter(request, response);
    }
}
