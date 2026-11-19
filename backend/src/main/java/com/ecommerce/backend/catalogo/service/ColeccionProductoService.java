
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.response.ColeccionProductoResponse;
import com.ecommerce.backend.catalogo.dto.response.ColeccionResponse;
import com.ecommerce.backend.catalogo.dto.response.ProductoResponse;

import com.ecommerce.backend.catalogo.mapper.ColeccionMapper;
import com.ecommerce.backend.catalogo.mapper.ColeccionProductoMapper;
import com.ecommerce.backend.catalogo.mapper.ProductoMapper;

import com.ecommerce.backend.catalogo.model.Coleccion;
import com.ecommerce.backend.catalogo.model.ColeccionProducto;
import com.ecommerce.backend.catalogo.model.ColeccionProductoId;
import com.ecommerce.backend.catalogo.model.Producto;

import com.ecommerce.backend.catalogo.repository.ColeccionProductoRepository;
import com.ecommerce.backend.catalogo.repository.ColeccionRepository;
import com.ecommerce.backend.catalogo.repository.ProductoRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColeccionProductoService {

    private final ColeccionProductoRepository coleccionProductoRepository;
    private final ColeccionRepository coleccionRepository;
    private final ProductoRepository productoRepository;

    public ColeccionProductoService(
            ColeccionProductoRepository coleccionProductoRepository,
            ColeccionRepository coleccionRepository,
            ProductoRepository productoRepository) {

        this.coleccionProductoRepository = coleccionProductoRepository;
        this.coleccionRepository = coleccionRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public ColeccionProductoResponse agregarProducto(
            UUID idColeccion,
            UUID idProducto,
            UUID idTienda) {

        // Verificar que la colección pertenezca a la tienda.
        buscarColeccion(idColeccion, idTienda);

        // Verificar que el producto pertenezca a la tienda.
        buscarProducto(idProducto, idTienda);

        // Evitar asociaciones duplicadas.
        if (coleccionProductoRepository
                .existsById_IdColeccionAndId_IdProducto(
                        idColeccion, idProducto)) {

            throw new IllegalArgumentException(
                    "El producto ya pertenece a esta colección");
        }

        ColeccionProducto relacion = new ColeccionProducto();

        relacion.setId(
                new ColeccionProductoId(
                        idColeccion,
                        idProducto));

        ColeccionProducto guardada =
                coleccionProductoRepository.save(relacion);

        return ColeccionProductoMapper.toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductosPorColeccion(
            UUID idColeccion,
            UUID idTienda) {

        buscarColeccion(idColeccion, idTienda);

        return coleccionProductoRepository
                .findById_IdColeccion(idColeccion)
                .stream()
                .map(relacion -> buscarProducto(
                        relacion.getId().getIdProducto(),
                        idTienda))
                .map(ProductoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ColeccionResponse> listarColeccionesPorProducto(
            UUID idProducto,
            UUID idTienda) {

        buscarProducto(idProducto, idTienda);

        return coleccionProductoRepository
                .findById_IdProducto(idProducto)
                .stream()
                .map(relacion -> buscarColeccion(
                        relacion.getId().getIdColeccion(),
                        idTienda))
                .map(ColeccionMapper::toResponse)
                .toList();
    }

    @Transactional
    public void quitarProducto(
            UUID idColeccion,
            UUID idProducto,
            UUID idTienda) {

        buscarColeccion(idColeccion, idTienda);
        buscarProducto(idProducto, idTienda);

        if (!coleccionProductoRepository
                .existsById_IdColeccionAndId_IdProducto(
                        idColeccion, idProducto)) {

            throw new IllegalArgumentException(
                    "El producto no pertenece a esta colección");
        }

        coleccionProductoRepository
                .deleteById_IdColeccionAndId_IdProducto(
                        idColeccion, idProducto);
    }

    // Buscar colección dentro de la tienda.
    private Coleccion buscarColeccion(
            UUID idColeccion,
            UUID idTienda) {

        return coleccionRepository
                .findByIdColeccionAndIdTienda(
                        idColeccion, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La colección no existe en esta tienda"));
    }

    // Buscar producto dentro de la tienda.
    private Producto buscarProducto(
            UUID idProducto,
            UUID idTienda) {

        return productoRepository
                .findByIdProductoAndIdTienda(
                        idProducto, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe en esta tienda"));
    }
}
