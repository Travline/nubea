# Integración del módulo 3 con los demás módulos

Estado observado en el código: 9 de noviembre de 2026. Este documento describe el backend existente y distingue las obligaciones de integración de funcionalidades que todavía no existen. Las reglas funcionales están en [modules-rules.md](../functional/modules-rules.md) y las historias en [uh-m3.md](../functional/uh-m3.md). Ante una discrepancia, comprobar código y esquema antes de integrar: la especificación expresa la intención, no demuestra implementación.

## M1: identidad, seguridad y tiendas

- `CurrentUserProvider` obtiene el identificador del `SecurityContext`; su implementación actual solo acepta un principal `UUID` o un `String` que pueda convertirse a UUID. Un principal `UserDetails` común no sirve sin adaptar el proveedor. Los roles se expresan mediante authorities Spring, por ejemplo `ROLE_ADMIN_GENERAL`.
- El `POST /api/auth/login` actual verifica una contraseña y devuelve datos de usuario, pero no emite cookie/JWT ni instala una sesión reutilizable. `SecurityConfig` es stateless, tiene CSRF deshabilitado y no configura un filtro de autenticación; por tanto, los endpoints protegidos no constituyen una autenticación completa. M1 debe aportar el mecanismo real, cargar el principal y los roles, y diseñar CSRF/CORS si utiliza cookies. No aceptar el id de usuario del cuerpo ni de cabeceras no autenticadas.
- **Incompatibilidad actual:** `UsuarioPlataforma` mapea `apellidos`, `correo`, `contrasena`, `rol` y `fecha_actualizacion`, mientras el SQL de `usuario_plataforma` usa `apellido_paterno`, `apellido_materno`, `email`, etc., y maneja roles/identidades por tablas separadas. Alinear M1 y el esquema antes de depender de registro o login; no presumir que los endpoints existentes funcionan contra el SQL actual.
- `TiendaPort.obtenerTiendaPorId` y `TiendaPortAdapter` son el adaptador temporal al modelo mínimo de la tabla `tienda`. Sustituir el adaptador al integrar M1 sin registrar dos implementaciones ambiguas. `TiendaAuthorizationService.esPropietario` compara UUID con `id_propietario`: tener rol no implica ser dueño. La consulta de una tienda inexistente también resulta en acceso denegado.
- Algunos flujos de cuenta de pago consultan/bloquean `tienda` directamente con SQL, además de usar el puerto en autorización. Una sustitución de M1 debe conservar tabla/columnas y semántica de bloqueo, o adaptar también `OAuthTransactionStore` y `CuentaPagoDesconexionService`; reemplazar solo `TiendaPort` no basta.
- La creación de tienda debe inicializar `configuracion_tienda` con plantilla activa por defecto y `cuenta_pago_tienda` PENDIENTE en la misma transacción. M3 no crea actualmente la configuración; su GET devuelve 404 si falta. El GET de cuenta devuelve 404 si falta; iniciar OAuth crea cuenta PENDIENTE si es necesario. El SQL no proporciona automáticamente estos registros ni una plantilla inicial.
- Las mutaciones de apariencia y banners usan `TiendaStatePolicy`: ACTIVA o BORRADOR; OAuth valida esos estados directamente en SQL. La desconexión permite también otras situaciones de la tienda. Coordinar la política definitiva y sus errores con M1.

## M2: catálogo e inventario

`GET /api/tiendas/{idTienda}/inventario` es solo lectura. La consulta usa directamente `producto`, `presentacion_producto` e `inventario`, con filtro por tienda y unión izquierda. Cualquier evolución de nombres, tipos o estados en M2 requiere actualizar la consulta y su prueba PostgreSQL. El filtro de alerta excluye presentaciones sin inventario; sin él, `sinRegistroInventario=true` señala la inconsistencia. Ver [contrato de inventario](../technical/inventario-consulta.md).

M2 debe crear el inventario junto con la presentación en una transacción, inicializar cantidades no negativas y preservar la PK/FK por presentación. No resolver ausencias creando stock durante un GET. M2 es dueño de las mutaciones de catálogo e inventario; M3 no tiene API de modificación.

## M4: checkout y pagos

Solo una cuenta `ACTIVA` puede servir para nuevos cobros; M4 aún debe implementar y hacer cumplir esa comprobación. `GET cuenta-pago` devuelve identificador enmascarado, no credenciales: no usarlo como fuente de credenciales de servidor ni descifrar en React. El vault actual es una abstracción genérica de cifrado, no existe todavía un puerto de M4 que devuelva credenciales tipadas, gestione refresh ni coordine pagos con desconexión.

Antes de integrar, acordar un servicio interno de credenciales que valide tienda y estado, use `PaymentCredentialVault` solo en servidor y no registre tokens. El sobre cifrado actual contiene tres componentes separados por `.`: access token y refresh token en Base64 URL sin padding y vencimiento absoluto ISO-8601 opcional; la finalidad autenticada es `mercadopago:{idTienda}`. Tratar ese formato como implementación interna versionable, no como API pública. Coordinar semántica de pagos ya iniciados y carreras entre comprobación, uso y desconexión; esta última no cancela pagos en curso.

Checkout debe realizar reserva transaccional mediante actualización condicional y control de concurrencia descritos en RN-M3-12/13 y RN-M4-05. La lectura paginada de M3 no reserva ni garantiza que el stock permanezca disponible.

## Frontend y vitrina

El CMS debe interpretar los códigos de plantilla disponibles; la API solo administra metadatos. El flujo OAuth devuelve `urlAutorizacion`, navega al proveedor y retorna a la URL fija de panel con `resultado=exito|error|conflicto`; después consultar estado mediante GET. No confiar en el resultado de la URL como prueba de activación. El callback consume el estado incluso si falla el canje: iniciar otro intento para reintentar.

Los endpoints de configuración y banners existentes son privados del propietario, no una API pública de vitrina. `BannerResponse` expone actualmente `storageKey`, no URL pública: definir explícitamente una estrategia de URL, CDN o firma y su autorización antes de usarlo en una tienda pública. No asumir que una clave interna es URL navegable.
