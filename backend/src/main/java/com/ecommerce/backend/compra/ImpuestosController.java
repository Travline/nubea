package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.security.CurrentUserProvider;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/impuestos")
@PreAuthorize("hasAuthority('ROLE_ADMIN_GENERAL')")
public class ImpuestosController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider usuarioActual;
    public record Impuesto(short id, BigDecimal tasa, LocalDate inicio, LocalDate fin) {}
    public record NuevaVigencia(@NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal tasa,
                                @NotNull LocalDate inicio) {}
    @GetMapping
    public List<Impuesto> listar() {
        return jdbc.query("SELECT id_impuesto,tasa_porcentaje,fecha_inicio,fecha_fin FROM configuracion_impuesto WHERE codigo='IGV_GENERAL' ORDER BY fecha_inicio DESC",
                (r,n)->new Impuesto(r.getShort(1),r.getBigDecimal(2),r.getObject(3,LocalDate.class),r.getObject(4,LocalDate.class)));
    }
    @PostMapping
    @Transactional
    public Impuesto crear(@Valid @RequestBody NuevaVigencia vigencia) {
        UUID usuario=usuarioActual.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Acceso denegado"));
        LocalDate hoy=jdbc.queryForObject("SELECT CURRENT_DATE",LocalDate.class);
        if (!vigencia.inicio().isAfter(hoy)) throw new ReglaNegocioException("La nueva vigencia debe comenzar en una fecha futura");
        jdbc.queryForList("SELECT id_impuesto FROM configuracion_impuesto WHERE codigo='IGV_GENERAL' ORDER BY id_impuesto FOR UPDATE");
        if (jdbc.queryForObject("SELECT count(*) FROM configuracion_impuesto WHERE codigo='IGV_GENERAL' AND fecha_inicio>=?",Integer.class,vigencia.inicio())>0)
            throw new ReglaNegocioException("Ya existe una vigencia en esa fecha o posterior");
        int modificadas=jdbc.update("UPDATE configuracion_impuesto SET fecha_fin=? WHERE codigo='IGV_GENERAL' AND fecha_inicio<? AND (fecha_fin IS NULL OR fecha_fin>?)",vigencia.inicio(),vigencia.inicio(),vigencia.inicio());
        if (modificadas != 1) throw new ReglaNegocioException("No existe una vigencia anterior aplicable");
        Short id=jdbc.queryForObject("INSERT INTO configuracion_impuesto(codigo,nombre,tasa_porcentaje,fecha_inicio,creado_por) VALUES ('IGV_GENERAL','IGV + IPM (tasa conjunta)',?,?,?) RETURNING id_impuesto",Short.class,vigencia.tasa(),vigencia.inicio(),usuario);
        return new Impuesto(id,vigencia.tasa(),vigencia.inicio(),null);
    }
}
