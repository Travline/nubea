package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.security.CurrentUserProvider;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cliente")
public class DireccionesController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider usuarioActual;
    public record Ubigeo(String codigo, String departamento, String provincia, String distrito) {}
    public record Direccion(UUID id, String codigoUbigeo, String departamento, String provincia, String distrito,
                            String alias, String direccion, String referencia, String telefono, boolean principal) {}
    public record DatosDireccion(@NotBlank @Pattern(regexp="[0-9]{6}") String codigoUbigeo,
                                 @Size(max=60) String alias, @NotBlank @Size(max=250) String direccion,
                                 @Size(max=250) String referencia, @Size(max=20) String telefono, boolean principal) {}
    private UUID usuario() { return usuarioActual.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Acceso denegado")); }
    private static String opcional(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    @GetMapping("/ubigeo")
    public List<Ubigeo> ubigeo(@RequestParam(defaultValue="") @Size(max=100) String texto) {
        String patron = "%" + texto.trim().toLowerCase(java.util.Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
        return jdbc.query("SELECT codigo_ubigeo,departamento,provincia,distrito FROM ubigeo WHERE lower(concat(departamento,' ',provincia,' ',distrito,' ',codigo_ubigeo)) LIKE ? ESCAPE '\\' ORDER BY departamento,provincia,distrito LIMIT 100",
                (rs,n) -> new Ubigeo(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)), patron);
    }
    @GetMapping("/direcciones")
    public List<Direccion> listar() {
        return jdbc.query("""
            SELECT d.id_direccion,d.codigo_ubigeo,u.departamento,u.provincia,u.distrito,d.alias,d.direccion,d.referencia,d.telefono_contacto,d.es_principal
            FROM direccion_cliente d JOIN ubigeo u ON u.codigo_ubigeo=d.codigo_ubigeo WHERE d.id_usuario=? ORDER BY d.es_principal DESC,d.fecha_creacion,d.id_direccion
            """, (r,n) -> new Direccion(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9),r.getBoolean(10)), usuario());
    }
    @PostMapping("/direcciones")
    @Transactional
    public Direccion crear(@Valid @RequestBody DatosDireccion datos) {
        UUID usuario = usuario(), id = UUID.randomUUID();
        bloquearUsuario(usuario);
        validarUbigeo(datos.codigoUbigeo());
        if (datos.principal()) desmarcar(usuario);
        jdbc.update("INSERT INTO direccion_cliente(id_direccion,id_usuario,codigo_ubigeo,alias,direccion,referencia,telefono_contacto,es_principal) VALUES (?,?,?,?,?,?,?,?)",
                id,usuario,datos.codigoUbigeo(),opcional(datos.alias()),datos.direccion().trim(),opcional(datos.referencia()),opcional(datos.telefono()),datos.principal());
        return listar().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }
    @PutMapping("/direcciones/{id}")
    @Transactional
    public Direccion modificar(@PathVariable UUID id, @Valid @RequestBody DatosDireccion datos) {
        UUID usuario = usuario(); bloquearUsuario(usuario); validarUbigeo(datos.codigoUbigeo());
        if (jdbc.queryForObject("SELECT count(*) FROM direccion_cliente WHERE id_direccion=? AND id_usuario=?", Integer.class,id,usuario) == 0)
            throw new RecursoNoEncontradoException("Dirección no encontrada");
        if (datos.principal()) desmarcar(usuario);
        jdbc.update("UPDATE direccion_cliente SET codigo_ubigeo=?,alias=?,direccion=?,referencia=?,telefono_contacto=?,es_principal=? WHERE id_direccion=? AND id_usuario=?",
                datos.codigoUbigeo(),opcional(datos.alias()),datos.direccion().trim(),opcional(datos.referencia()),opcional(datos.telefono()),datos.principal(),id,usuario);
        return listar().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }
    @DeleteMapping("/direcciones/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Transactional
    public void eliminar(@PathVariable UUID id) {
        UUID usuario = usuario(); bloquearUsuario(usuario);
        if (jdbc.update("DELETE FROM direccion_cliente WHERE id_direccion=? AND id_usuario=?",id,usuario) == 0) throw new RecursoNoEncontradoException("Dirección no encontrada");
    }
    private void bloquearUsuario(UUID id) { jdbc.queryForObject("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario=? FOR UPDATE", UUID.class,id); }
    private void validarUbigeo(String codigo) { if (jdbc.queryForObject("SELECT count(*) FROM ubigeo WHERE codigo_ubigeo=?",Integer.class,codigo) == 0) throw new IllegalArgumentException("UBIGEO no encontrado"); }
    private void desmarcar(UUID id) { jdbc.update("UPDATE direccion_cliente SET es_principal=false WHERE id_usuario=? AND es_principal=true",id); }
}
