# Integración del módulo 3 con los demás módulos

La integración utiliza las entidades de M1 y M2 junto con los servicios de M3. Las reglas están en [modules-rules.md](../functional/modules-rules.md) y las historias en [uh-m3.md](../functional/uh-m3.md).

## Identidad, tiendas y catálogo

- El registro local e inicio de sesión usan Argon2 y sesión con cookie; `GET /api/auth/csrf` proporciona el token para las solicitudes de escritura. Configurar `APP_CORS_ALLOWED_ORIGINS` con orígenes explícitos y `SESSION_COOKIE_SECURE=true` con HTTPS. El cierre usa `POST /logout` con CSRF.
- El primer administrador se asigna a una cuenta activa ya registrada mediante `APP_BOOTSTRAP_ADMIN_EMAIL`, únicamente si no existe ningún administrador. Retirar la variable después de la inicialización. Los administradores pueden asignar roles y retirar el rol de dueño; esta última operación suspende las tiendas activas o en borrador.
- El propietario se obtiene de la sesión. Crear una tienda inicializa su configuración visual y cuenta de pago pendiente; crear una presentación inicializa el inventario. El administrador o propietario puede ajustar cantidades mediante `PUT /api/tiendas/{idTienda}/inventario/{idPresentacion}` con `stockActual` y `stockMinimo`.
- M3 consulta inventario sin modificarlo en su endpoint de lectura. La política de cambios de apariencia y banners admite tiendas ACTIVAS o en BORRADOR. El OAuth y la desconexión mantienen sus bloqueos SQL sobre la tienda.

## M4: checkout y pagos

Solo una cuenta `ACTIVA` puede servir para nuevos cobros; M4 aún debe implementar y hacer cumplir esa comprobación. `GET cuenta-pago` devuelve identificador enmascarado, no credenciales: no usarlo como fuente de credenciales de servidor ni descifrar en React. El vault actual es una abstracción genérica de cifrado, no existe todavía un puerto de M4 que devuelva credenciales tipadas, gestione refresh ni coordine pagos con desconexión.

Antes de integrar, acordar un servicio interno de credenciales que valide tienda y estado, use `PaymentCredentialVault` solo en servidor y no registre tokens. El sobre cifrado actual contiene tres componentes separados por `.`: access token y refresh token en Base64 URL sin padding y vencimiento absoluto ISO-8601 opcional; la finalidad autenticada es `mercadopago:{idTienda}`. Tratar ese formato como implementación interna versionable, no como API pública. Coordinar semántica de pagos ya iniciados y carreras entre comprobación, uso y desconexión; esta última no cancela pagos en curso.

Checkout debe realizar reserva transaccional mediante actualización condicional y control de concurrencia descritos en RN-M3-12/13 y RN-M4-05. La lectura paginada de M3 no reserva ni garantiza que el stock permanezca disponible.

## Frontend y vitrina

El CMS debe interpretar los códigos de plantilla disponibles; la API solo administra metadatos. El flujo OAuth devuelve `urlAutorizacion`, navega al proveedor y retorna a la URL fija de panel con `resultado=exito|error|conflicto`; después consultar estado mediante GET. No confiar en el resultado de la URL como prueba de activación. El callback consume el estado incluso si falla el canje: iniciar otro intento para reintentar.

Los endpoints de configuración y banners son privados del propietario. Las respuestas de imágenes incluyen una URL pública; su acceso depende de la configuración del bucket y CDN. Véase [imágenes y almacenamiento](../technical/imagenes-almacenamiento.md).
