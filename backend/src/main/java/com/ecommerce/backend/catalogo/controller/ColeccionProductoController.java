
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.response.ColeccionProductoResponse;
import com.ecommerce.backend.catalogo.dto.response.ColeccionResponse;
import com.ecommerce.backend.catalogo.dto.response.ProductoResponse;
import com.ecommerce.backend.catalogo.service.ColeccionProductoService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tiendas/{idTienda}")
public class ColeccionProductoController {

    private final ColeccionProductoService coleccionProductoService;

    public ColeccionProductoController(
            ColeccionProductoService coleccionProductoService) {
        this.coleccionProductoService = coleccionProductoService;
    }

    // Agregar un producto a una colección.
    @PostMapping("/colecciones/{idColeccion}/productos/{idProducto}")
    public ResponseEntity<ColeccionProductoResponse> agregarProducto(
            @PathVariable UUID idTienda,
            @PathVariable UUID idColeccion,
            @PathVariable UUID idProducto) {

        ColeccionProductoResponse response =
                coleccionProductoService.agregarProducto(
                        idColeccion, idProducto, idTienda);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar productos de una colección.
    @GetMapping("/colecciones/{idColeccion}/productos")
    public ResponseEntity<List<ProductoResponse>> listarProductosPorColeccion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idColeccion) {

        return ResponseEntity.ok(
                coleccionProductoService.listarProductosPorColeccion(
                        idColeccion, idTienda));
    }

    // Listar colecciones de un producto.
    @GetMapping("/productos/{idProducto}/colecciones")
    public ResponseEntity<List<ColeccionResponse>> listarColeccionesPorProducto(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto) {

        return ResponseEntity.ok(
                coleccionProductoService.listarColeccionesPorProducto(
                        idProducto, idTienda));
    }

    // Quitar un producto de una colección.
    @DeleteMapping("/colecciones/{idColeccion}/productos/{idProducto}")
    public ResponseEntity<Void> quitarProducto(
            @PathVariable UUID idTienda,
            @PathVariable UUID idColeccion,
            @PathVariable UUID idProducto) {

        coleccionProductoService.quitarProducto(
                idColeccion, idProducto, idTienda);

        return ResponseEntity.noContent().build();
    }
}
