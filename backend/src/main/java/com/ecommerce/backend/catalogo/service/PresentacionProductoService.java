package com.ecommerce.backend.catalogo.service;

import org.springframework.stereotype.Service;



import com.ecommerce.backend.catalogo.repository.PresentacionProductoRepository;
import com.ecommerce.backend.catalogo.repository.ProductoRepository;
import com.ecommerce.backend.catalogo.repository.VarianteRepository;
import com.ecommerce.backend.catalogo.repository.ValorVarianteRepository;
import com.ecommerce.backend.catalogo.repository.PresentacionValorRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.ecommerce.backend.catalogo.dto.request.PresentacionProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.PresentacionProductoResponse;
import com.ecommerce.backend.catalogo.mapper.PresentacionProductoMapper;
import com.ecommerce.backend.catalogo.model.PresentacionProducto;
import com.ecommerce.backend.catalogo.model.PresentacionValor;
import com.ecommerce.backend.catalogo.model.PresentacionValorId;

import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.backend.catalogo.model.ValorVariante;

@Service
public class PresentacionProductoService {

    private final PresentacionProductoRepository presentacionProductoRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ValorVarianteRepository valorVarianteRepository;
    private final PresentacionValorRepository presentacionValorRepository;

   public PresentacionProductoService(
        PresentacionProductoRepository presentacionProductoRepository,
        ProductoRepository productoRepository,
        VarianteRepository varianteRepository,
        ValorVarianteRepository valorVarianteRepository,
        PresentacionValorRepository presentacionValorRepository
) {
    this.presentacionProductoRepository = presentacionProductoRepository;
    this.productoRepository = productoRepository;
    this.varianteRepository = varianteRepository;
    this.valorVarianteRepository = valorVarianteRepository;
    this.presentacionValorRepository = presentacionValorRepository;
}

private List<ValorVariante> validarValores(
        List<UUID> idsValores,
        UUID idTienda
) {
    // Si no se seleccionaron valores, devolvemos una lista vacía
    if (idsValores == null || idsValores.isEmpty()) {
        return new ArrayList<>();
    }

    // Evitar valores duplicados en la solicitud
    Set<UUID> idsUnicos = new HashSet<>(idsValores);

    if (idsUnicos.size() != idsValores.size()) {
        throw new IllegalArgumentException(
                "No se pueden enviar valores duplicados"
        );
    }

    // Buscar los valores en la base de datos
    List<ValorVariante> valores = valorVarianteRepository
            .findAllById(idsValores);

    // Verificar que todos los valores existan
    if (valores.size() != idsValores.size()) {
        throw new IllegalArgumentException(
                "Uno o más valores de variante no existen"
        );
    }

    // Verificar que las variantes pertenezcan a la tienda
    Set<UUID> idsVariantes = new HashSet<>();

    for (ValorVariante valor : valores) {

        UUID idVariante = valor.getIdVariante();

        varianteRepository.findByIdVarianteAndIdTienda(
                idVariante,
                idTienda
        ).orElseThrow(() -> new IllegalArgumentException(
                "Uno de los valores pertenece a una variante de otra tienda"
        ));

        // Evitar dos valores de una misma variante
        if (!idsVariantes.add(idVariante)) {
            throw new IllegalArgumentException(
                    "No se pueden seleccionar dos valores de una misma variante"
            );
        }
    }

    // NUBEA permite como máximo tres tipos de variantes
    if (idsVariantes.size() > 3) {
        throw new IllegalArgumentException(
                "Un producto puede tener como máximo tres variantes"
        );
    }

    return valores;
}


private String construirFirmaValores(List<ValorVariante> valores) {

    if (valores == null || valores.isEmpty()) {
        return "";
    }

    return valores.stream()
            .map(valor -> valor.getIdValor().toString())
            .sorted()
            .collect(Collectors.joining("|"));
}





@Transactional
public PresentacionProductoResponse crearPresentacion(
        UUID idProducto,
        UUID idTienda,
        PresentacionProductoRequest request
) {
    // Validar la solicitud
    if (request == null || request.getSku() == null
            || request.getSku().isBlank()) {
        throw new IllegalArgumentException(
                "El SKU de la presentación es obligatorio"
        );
    }

    String sku = request.getSku().trim();

    // Verificar que el producto pertenezca a la tienda
    productoRepository.findByIdProductoAndIdTienda(
            idProducto,
            idTienda
    ).orElseThrow(() -> new IllegalArgumentException(
            "El producto no existe en esta tienda"
    ));

    // Verificar que el SKU no esté registrado en la tienda
    if (presentacionProductoRepository.existsByIdTiendaAndSku(
            idTienda,
            sku
    )) {
        throw new IllegalArgumentException(
                "Ya existe una presentación con ese SKU en esta tienda"
        );
    }

    // Validar los valores seleccionados
    List<ValorVariante> valores = validarValores(
            request.getIdsValores(),
            idTienda
    );

    // Construir la firma de valores
    String firmaValores = construirFirmaValores(valores);

    // Verificar que no exista esa combinación para el producto
    if (presentacionProductoRepository
            .existsByIdProductoAndFirmaValores(idProducto, firmaValores)) {
        throw new IllegalArgumentException(
                "Ya existe una presentación con esa combinación de valores"
        );
    }

    // Crear la presentación
    PresentacionProducto presentacion = new PresentacionProducto();

    presentacion.setIdProducto(idProducto);
    presentacion.setIdTienda(idTienda);
    presentacion.setSku(sku);
    presentacion.setPrecioOverride(request.getPrecioOverride());
    presentacion.setEstado("ACTIVA");
    presentacion.setFirmaValores(firmaValores);

    // Guardar la presentación
    PresentacionProducto guardada =
            presentacionProductoRepository.save(presentacion);

    // Crear las relaciones con los valores seleccionados
    List<PresentacionValor> relaciones = valores.stream()
            .map(valor -> {
                PresentacionValor relacion = new PresentacionValor();

                relacion.setId(new PresentacionValorId(
                        guardada.getIdPresentacion(),
                        valor.getIdValor()
                ));

                relacion.setIdVariante(valor.getIdVariante());

                return relacion;
            })
            .toList();

    // Guardar las relaciones
    presentacionValorRepository.saveAll(relaciones);

    return PresentacionProductoMapper.toResponse(guardada);
}



}