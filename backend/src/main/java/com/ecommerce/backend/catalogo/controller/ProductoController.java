
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.ProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ProductoResponse;
import com.ecommerce.backend.catalogo.service.ProductoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Crear producto.
    @PostMapping
    public ResponseEntity<ProductoResponse> crearProducto(
            @PathVariable UUID idTienda,
            @Valid @RequestBody ProductoRequest request) {

        ProductoResponse response =
                productoService.crearProducto(idTienda, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar productos de una tienda.
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos(
            @PathVariable UUID idTienda) {

        List<ProductoResponse> response =
                productoService.listarProductos(idTienda);

        return ResponseEntity.ok(response);
    }

    // Obtener producto por ID.
    @GetMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> obtenerProductoPorId(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto) {

        ProductoResponse response =
                productoService.obtenerProductoPorId(
                        idProducto, idTienda);

        return ResponseEntity.ok(response);
    }

    // Actualizar producto.
    @PutMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @Valid @RequestBody ProductoRequest request) {

        ProductoResponse response =
                productoService.actualizarProducto(
                        idProducto, idTienda, request);

        return ResponseEntity.ok(response);
    }

    // Cambiar estado del producto.
    @PatchMapping("/{idProducto}/estado")
    public ResponseEntity<ProductoResponse> cambiarEstadoProducto(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @RequestParam String estado) {

        ProductoResponse response =
                productoService.cambiarEstadoProducto(
                        idProducto, idTienda, estado);

        return ResponseEntity.ok(response);
    }
}
