# Módulo 3: flujos, extensión y límites actuales

Para cada historia, los criterios funcionales están en [HU-M3](../functional/uh-m3.md). Las rutas reales y los DTO del código prevalecen sobre los contratos orientativos de ese documento.

| Historia | Implementación y punto de entrada | Integración o límite |
| --- | --- | --- |
| 01–03 Plantillas | `PlantillaController`: POST y GET `/api/plantillas`, GET/PATCH `/{idPlantilla}`, PATCH `/{idPlantilla}/estado` | Escritura de ADMIN_GENERAL; listado/detalle para admin y dueño según rol. La aplicación cliente interpreta `codigo`; inactivas no se pueden elegir. |
| 04 Apariencia | GET/PATCH `/api/tiendas/{idTienda}/configuracion` | Solo propietario; M1 crea la fila y plantilla predeterminada. |
| 05 Banners | GET/POST `/api/tiendas/{idTienda}/banners`, PATCH/DELETE `/{idBanner}`, PUT `/{idBanner}/archivo`, PUT `/orden` | Multipart con campo `archivo`; identidad y estado de tienda se validan en backend. |
| 06 Inventario | GET `/api/tiendas/{idTienda}/inventario` | Solo lectura; [parámetros y paginación](inventario-consulta.md); M2/M4 mantienen el stock. |
| 07 Cuenta | GET `/api/tiendas/{idTienda}/cuenta-pago` | [Contrato](cuenta-pago-consulta.md); no devuelve tokens. |
| 08–09 OAuth | POST `/api/tiendas/{idTienda}/cuenta-pago/mercado-pago/oauth/inicios`; GET `/api/integraciones/mercado-pago/oauth/callback` | Condicionados a `mercadopago.oauth.enabled=true`; [configuración y flujo](mercadopago-oauth.md). |
| 10 Desconexión | POST `/api/tiendas/{idTienda}/cuenta-pago/desconexion` | `204`, idempotente; invalida intentos, borra referencia e identificador y registra transición. No revoca remotamente ni cancela pagos ya iniciados. |

## OAuth y transacciones

1. Inicio: comprobar titularidad y estado; con bloqueo de tienda, rechazar cuenta ACTIVA, crear cuenta PENDIENTE si falta e invalidar intentos anteriores. Generar state y PKCE; en la BD queda hash del state y verificador cifrado.
2. Callback público: consumir el intento válido una sola vez en transacción independiente; verificar tienda y propietario, descifrar verificador y canjear código por HTTP fuera de transacción. Si falla, no activar; el usuario debe iniciar otro intento.
3. Activación: volver a bloquear tienda y verificar el intento y la titularidad; guardar cuenta externa, sobre cifrado y estado ACTIVA en una transacción. Un nuevo inicio o desconexión invalida intentos consumidos en curso.
4. Desconexión: bloquear tienda y cuenta, invalidar intentos y borrar credencial/identificador dentro de la transacción; registrar historial. Al repetirse, no duplica historial.

La vinculación local no prueba que el proveedor conserve autorización en el futuro. Falta diseñar renovación, desautorización externa/webhooks, reconciliación y operaciones de pagos de M4. Ningún secreto, state, code, URL completa de autorización ni cabeceras de proveedor debe registrarse o enviarse a frontend.

## Banners: comportamiento y trabajo pendiente

`BannerStoragePort` tiene operaciones de subir/eliminar y `S3BannerStorageAdapter` usa SDK S3 compatible. Se valida tamaño y cabecera real de imágenes. El orden es único por tienda y la restricción SQL es diferible. Las eliminaciones del objeto anterior se publican como evento posterior al commit.

**Brechas actuales:** `BannerMapper` devuelve `storageKey`; no entrega URL. Las subidas y ciertas compensaciones de almacenamiento se realizan dentro de métodos `@Transactional`, pese a la regla funcional de no hacer llamadas externas dentro de transacciones. El listener posterior al commit no tiene `@Async` ni cola persistente; el adaptador captura fallos de borrado y solo registra una advertencia, sin reintento durable. Un fallo al confirmar después de subir puede dejar objetos huérfanos. Para producción, rediseñar el flujo con persistencia de tareas/outbox, reconciliación y pruebas de caída, y definir la exposición de imágenes.

## Puntos de modificación

- Cambios en tablas de catálogo o estado de tienda afectan consultas nativas; probar aislamiento multitienda y compatibilidad con M1/M2.
- Cambios de proveedores o vault requieren mantener un adaptador y formato versionado, migrar sobres existentes y probar rotación/lectura con claves anteriores.
- La historia 10 guarda historial solo de desconexión, no una auditoría completa de todas las transiciones. Diseñar auditoría integral si se necesita trazabilidad de creación, fallos, revinculación o cambios externos.
- Para endpoints y errores verificar siempre controladores, DTO y `GlobalExceptionHandler`; conversiones inválidas de parámetros pueden seguir el manejo estándar de Spring, no necesariamente `ErrorResponse`.
