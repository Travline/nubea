package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.ImagenProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ImagenProductoResponse;
import com.ecommerce.backend.catalogo.mapper.ImagenProductoMapper;
import com.ecommerce.backend.catalogo.model.ImagenProducto;
import com.ecommerce.backend.catalogo.repository.ImagenProductoRepository;
import com.ecommerce.backend.catalogo.repository.ProductoRepository;
import com.ecommerce.backend.shared.storage.AlmacenamientoImagenes;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class ImagenProductoService {
    private final ImagenProductoRepository imagenes;
    private final ProductoRepository productos;
    private final AlmacenamientoImagenes almacenamiento;
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;

    public ImagenProductoResponse crearImagen(UUID producto, UUID tienda, MultipartFile archivo, Integer orden, Boolean principal) {
        validarOrden(orden);
        return almacenamiento.subirYGuardar("productos", tienda, archivo, key -> {
            bloquearProducto(producto, tienda);
            if (Boolean.TRUE.equals(principal)) quitarPrincipal(producto);
            ImagenProducto imagen = new ImagenProducto();
            imagen.setIdProducto(producto);
            imagen.setStorageKey(key);
            imagen.setOrden(orden);
            imagen.setEsPrincipal(Boolean.TRUE.equals(principal));
            return respuesta(imagenes.saveAndFlush(imagen));
        });
    }

    public ImagenProductoResponse sustituirImagen(UUID id, UUID producto, UUID tienda, MultipartFile archivo) {
        return almacenamiento.subirYGuardar("productos", tienda, archivo, key -> {
            bloquearProducto(producto, tienda);
            ImagenProducto imagen = buscar(id, producto);
            String anterior = imagen.getStorageKey();
            imagen.setStorageKey(key);
            ImagenProductoResponse resultado = respuesta(imagenes.saveAndFlush(imagen));
            almacenamiento.encolarEliminacion(anterior);
            return resultado;
        });
    }

    @Transactional(readOnly = true)
    public List<ImagenProductoResponse> listarImagenes(UUID producto, UUID tienda) {
        validarProducto(producto, tienda);
        return imagenes.findByIdProductoOrderByOrdenAsc(producto).stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public ImagenProductoResponse obtenerImagenPorId(UUID id, UUID producto, UUID tienda) {
        validarProducto(producto, tienda);
        return respuesta(buscar(id, producto));
    }

    @Transactional
    public ImagenProductoResponse actualizarImagen(UUID id, UUID producto, UUID tienda, ImagenProductoRequest request) {
        bloquearProducto(producto, tienda);
        if (request == null) throw new IllegalArgumentException("Solicitud inválida");
        validarOrden(request.getOrden());
        ImagenProducto imagen = buscar(id, producto);
        imagen.setOrden(request.getOrden());
        return respuesta(imagenes.save(imagen));
    }

    @Transactional
    public ImagenProductoResponse establecerImagenPrincipal(UUID id, UUID producto, UUID tienda) {
        bloquearProducto(producto, tienda);
        ImagenProducto imagen = buscar(id, producto);
        quitarPrincipal(producto);
        imagen.setEsPrincipal(true);
        return respuesta(imagenes.saveAndFlush(imagen));
    }

    @Transactional
    public void eliminarImagen(UUID id, UUID producto, UUID tienda) {
        bloquearProducto(producto, tienda);
        ImagenProducto imagen = buscar(id, producto);
        String key = imagen.getStorageKey();
        imagenes.delete(imagen);
        imagenes.flush();
        almacenamiento.encolarEliminacion(key);
    }

    private void validarProducto(UUID producto, UUID tienda) {
        productos.findByIdProductoAndIdTienda(producto, tienda)
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe en esta tienda"));
    }
    private void bloquearProducto(UUID producto, UUID tienda) {
        var propietarios = jdbc.queryForList("SELECT id_propietario FROM tienda WHERE id_tienda=? FOR UPDATE", tienda);
        if (propietarios.isEmpty() || (!currentUser.hasRole("ADMIN_GENERAL") && currentUser.getCurrentUserId().filter(propietarios.get(0).get("id_propietario")::equals).isEmpty()))
            throw new AccessDeniedException("Acceso denegado");
        var filas = jdbc.queryForList("SELECT id_producto FROM producto WHERE id_producto=? AND id_tienda=? FOR UPDATE", producto, tienda);
        if (filas.isEmpty()) throw new IllegalArgumentException("El producto no existe en esta tienda");
    }
    private ImagenProducto buscar(UUID id, UUID producto) {
        return imagenes.findByIdImagenAndIdProducto(id, producto)
                .orElseThrow(() -> new IllegalArgumentException("La imagen no existe en este producto"));
    }
    private void quitarPrincipal(UUID producto) {
        imagenes.findByIdProductoAndEsPrincipalTrue(producto).ifPresent(actual -> {
            actual.setEsPrincipal(false);
            imagenes.saveAndFlush(actual);
        });
    }
    private ImagenProductoResponse respuesta(ImagenProducto imagen) {
        return ImagenProductoMapper.toResponse(imagen, almacenamiento.url(imagen.getStorageKey()));
    }
    private void validarOrden(Integer orden) {
        if (orden == null || orden < 1) throw new IllegalArgumentException("El orden debe ser mayor o igual a 1");
    }
}
