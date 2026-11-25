package com.ecommerce.backend.compra;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;

@Component
public class PagosConfiguracion {
    private final boolean checkout;
    private final boolean produccionGlobal;
    private final String origen;
    private final Map<UUID,String> tiendas;
    public PagosConfiguracion(@Value("${mercadopago.pagos.checkout-enabled:false}") boolean checkout,
            @Value("${mercadopago.pagos.store-origin:https://nubea.vercel.app}") String origen,
            @Value("${mercadopago.pagos.tiendas:}") String lista,
            @Value("${mercadopago.pagos.produccion-global:false}") boolean produccionGlobal) {
        this.checkout=checkout;
        this.produccionGlobal=produccionGlobal;
        URI url=URI.create(origen);
        if (!"https".equals(url.getScheme()) || url.getHost()==null || url.getUserInfo()!=null || url.getQuery()!=null
                || url.getFragment()!=null || !(url.getPath().isEmpty() || url.getPath().equals("/")))
            throw new IllegalStateException("Origen de tienda inválido");
        this.origen=origen.replaceAll("/$","");
        this.tiendas=produccionGlobal ? Map.of() : leerTiendas(lista);
    }

    private static Map<UUID,String> leerTiendas(String lista) {
        var mapa=new java.util.HashMap<UUID,String>();
        for (String entrada:lista.split(",")) {
            if (entrada.isBlank()) continue;
            String[] partes=entrada.trim().split(":");
            if (partes.length!=2 || !java.util.Set.of("PRUEBA","PRODUCCION").contains(partes[1]))
                throw new IllegalStateException("Lista de tiendas de pago inválida");
            mapa.put(UUID.fromString(partes[0]),partes[1]);
        }
        return Map.copyOf(mapa);
    }

    /** Ambiente de cobro configurado para la tienda, si los cobros están habilitados para ella. */
    public Optional<String> ambienteHabilitado(UUID tienda) {
        if (!checkout) return Optional.empty();
        return Optional.ofNullable(produccionGlobal ? "PRODUCCION" : tiendas.get(tienda));
    }
    public String ambiente(UUID tienda) {
        return ambienteHabilitado(tienda)
                .orElseThrow(() -> new ReglaNegocioException("Los pagos aún no están habilitados en esta tienda"));
    }
    public String retorno(String slug, UUID pedido) {
        return org.springframework.web.util.UriComponentsBuilder.fromUriString(origen).pathSegment("tienda",slug,"pedidos",pedido.toString()).build().encode().toUriString();
    }
}
