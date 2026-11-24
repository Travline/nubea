package com.ecommerce.backend.shared.exception;

/** Textos que ve el vendedor o el comprador. Sin jerga técnica (slug, SKU, presentación, variante...). */
public final class MensajesUsuario {
    private MensajesUsuario() {
    }

    public static final String TIENDA_DIRECCION_DUPLICADA = "Ya existe una tienda con esa dirección web";
    public static final String CATEGORIA_DIRECCION_DUPLICADA = "Ya existe una categoría con esa dirección web";
    public static final String COLECCION_DIRECCION_DUPLICADA = "Ya existe una colección con esa dirección web";
    public static final String DIRECCION_FORMATO =
            "La dirección web solo puede tener minúsculas, números y guiones (sin tildes ni espacios)";
    public static final String NOMBRE_VACIO = "El nombre no puede estar vacío";
    public static final String CODIGO_DUPLICADO = "Ya existe otro producto con ese código interno en tu tienda";
    public static final String CODIGO_VACIO = "El código interno no puede estar vacío";
    public static final String COMBINACION_REPETIDA = "Ya existe una versión de este producto con esas opciones";
    public static final String UN_VALOR_POR_OPCION = "Elige un solo valor por opción (por ejemplo, un solo color)";
    public static final String OPCION_DUPLICADA = "Ya tienes una opción con ese nombre (por ejemplo, Color)";
    public static final String VALOR_DUPLICADO = "Esa opción ya tiene ese valor";
    public static final String VALOR_VACIO = "El valor no puede estar vacío";
    public static final String PRECIO_NEGATIVO = "El precio no puede ser negativo";
    public static final String UNIDADES_NEGATIVAS = "Las unidades no pueden ser negativas";
    public static final String FOTO_PRINCIPAL_DUPLICADA = "Este producto ya tiene una foto principal";
    public static final String POSICION_BANNER_OCUPADA = "Ya hay una imagen destacada en esa posición";
    public static final String EMAIL_DUPLICADO = "Ya existe una cuenta con ese email";
    public static final String DOCUMENTO_DUPLICADO = "Ese documento ya está registrado";

    // Administración de tiendas
    public static final String DUENO_SIN_PERMISO_VENDEDOR =
            "El dueño de esta tienda ya no tiene permiso de vendedor. Devuélveselo antes de reactivarla";
    public static final String TIENDA_CERRADA_SIN_CAMBIOS = "Una tienda cerrada no se puede suspender ni reactivar";
    public static final String SOLO_SE_REACTIVA_SUSPENDIDA = "Solo se puede reactivar una tienda suspendida";
    public static final String ESTADO_ADMIN_INVALIDO = "El estado debe ser SUSPENDIDA o ACTIVA";

    // Vitrina
    public static final String TIENDA_NO_DISPONIBLE = "Esta tienda no está disponible en este momento";

    // Inventario
    public static final String AJUSTE_CERO = "Indica cuántas unidades sumar o quitar (distinto de cero)";
    public static final String STOCK_CAMBIO =
            "El stock cambió mientras lo editabas (alguien compró o lo ajustó). Revisa el valor actual";

    // Publicación de productos (RN-M2-13)
    public static final String PUBLICAR_TIENDA_INACTIVA = "Para publicar, tu tienda debe estar activa";
    public static final String PUBLICAR_CATEGORIA_INACTIVA = "Para publicar, la categoría del producto debe estar activa";
    public static final String PUBLICAR_SIN_VERSION_ACTIVA = "Para publicar, el producto necesita al menos una versión activa";
    public static final String PUBLICAR_SIN_UNIDADES = "Para publicar, al menos una versión necesita unidades disponibles";

    public static String soloQuedanUnidades(int unidades) {
        return unidades == 1 ? "Solo queda 1 unidad disponible" : "Solo quedan " + unidades + " unidades disponibles";
    }

    public static String unidadesApartadas(int apartadas) {
        return "Hay " + apartadas + (apartadas == 1 ? " unidad apartada" : " unidades apartadas")
                + " en pedidos pendientes de pago; el conteo no puede ser menor";
    }

    // Checkout
    public static final String NINGUN_PRODUCTO_DISPONIBLE = "Ninguno de los productos de tu carrito está disponible en este momento";
}
