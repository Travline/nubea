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

    // Inicio de sesión con Google (RN-M1-05)
    public static final String GOOGLE_NO_DISPONIBLE = "El inicio de sesión con Google no está disponible en este momento";
    public static final String GOOGLE_TOKEN_INVALIDO = "No pudimos verificar tu cuenta de Google. Inténtalo de nuevo.";
    public static final String GOOGLE_CORREO_NO_VERIFICADO = "Tu correo de Google no está verificado";
    public static final String GOOGLE_CORREO_YA_REGISTRADO =
            "Ya tienes una cuenta con este correo. Inicia sesión con tu contraseña y vincula Google desde tu cuenta.";
    public static final String GOOGLE_DE_OTRO_USUARIO = "Esta cuenta de Google ya está vinculada a otro usuario de Nubea.";
    public static final String GOOGLE_YA_VINCULADO_OTRA = "Tu cuenta ya tiene vinculada otra cuenta de Google.";

    // Documento de identidad (registro y perfil)
    public static final String DOCUMENTO_TIPO_Y_NUMERO = "El tipo y el número de documento van juntos";
    public static final String DOCUMENTO_TIPO_INVALIDO = "Elige un tipo de documento válido (DNI, carné de extranjería o pasaporte)";
    public static final String DNI_INVALIDO = "El DNI debe tener 8 dígitos";
    public static final String CARNE_EXTRANJERIA_INVALIDO = "El carné de extranjería debe tener entre 9 y 12 letras o números";
    public static final String PASAPORTE_INVALIDO = "El pasaporte debe tener entre 6 y 12 letras o números";
    public static final String PAIS_EMISOR_REQUERIDO = "Para el pasaporte indica el país emisor (por ejemplo, CO)";

    // Perfil y contraseña
    public static final String CONTRASENA_ACTUAL_INCORRECTA = "La contraseña actual no es correcta";
    public static final String CONTRASENA_NUEVA_IGUAL = "La nueva contraseña debe ser distinta de la actual";
    public static final String CUENTA_SIN_CONTRASENA = "Tu cuenta entra con Google y todavía no tiene contraseña.";
}
