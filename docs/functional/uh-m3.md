# Historias de usuario para el módulo 3

## Propósito y alcance

Este módulo entrega el backend de administración de la apariencia de una tienda,
sus banners, su cuenta de cobro de Mercado Pago y la consulta de inventario. No
administra el código o los archivos de una plantilla, no modifica existencias y
no crea pagos, pedidos ni preferencias de Mercado Pago; esas responsabilidades
pertenecen al frontend, al módulo 2 y al módulo 4, respectivamente.

En el modelo de datos el rol del vendedor se llama `DUENO_TIENDA`. En este
documento, **vendedor** significa un usuario con ese rol que además es
propietario de la tienda objetivo. `ADMIN_GENERAL` puede administrar el
catálogo global de plantillas, pero no adquiere por ello la titularidad de una
tienda ajena.

Las historias se diseñan para Spring Boot con autenticación por cookie pendiente
de implementación. Mientras no exista el mecanismo real de sesión/JWT, los
controladores y servicios deben depender de una abstracción de usuario
autenticado; no deben aceptar el identificador de usuario desde el request.

## Decisiones confirmadas y puntos de integración diferidos

Estas decisiones permiten implementar el módulo aislado y sustituir las
integraciones cuando los otros módulos estén listos.

| Tema | Decisión para este módulo | Placeholder o bandera de integración |
| --- | --- | --- |
| Sesión y roles | La seguridad se expresa con `@PreAuthorize` y un principal autenticado. | `CurrentUserProvider` y `TiendaAuthorizationService`; temporalmente puede usarse un principal de pruebas. |
| Propiedad de tienda | Todas las operaciones de vendedor verifican `tienda.id_propietario == usuario autenticado`. | `TiendaAuthorizationService.esPropietario(idTienda, idUsuario)`. No confiar solo en el rol global. |
| Estado de la tienda | Editar apariencia, banners y vincular pagos requiere tienda `ACTIVA` o `BORRADOR`; se rechaza `SUSPENDIDA` y `CERRADA`. | `TiendaStatePolicy`; confirmar la política definitiva al integrar el módulo 1. |
| Plantillas | El backend persiste únicamente `codigo`, `nombre`, `previewUrl` y `activa`; el frontend interpreta el código y contiene los recursos/código visuales. | Ninguna carga de archivos ni ejecución de código de plantilla en este módulo. |
| Archivos de banner | El módulo gestiona la subida, sustitución y eliminación de imágenes en un almacenamiento compatible con S3. PostgreSQL persiste solamente el `storage_key`, no el binario ni una URL pública permanente. | `BannerStoragePort` con primera implementación `S3BannerStorageAdapter`, basada en AWS SDK for Java v2 (`software.amazon.awssdk:s3`) configurado para un endpoint S3 compatible. Variables de entorno para endpoint, región, bucket y credenciales. |
| OAuth de Mercado Pago | Se propone OAuth Authorization Code, con `state` de un solo uso y callback de backend. | `MercadoPagoOAuthPort`, `OAuthStateStore` y configuración por ambiente; no usar llamadas reales en pruebas unitarias. |
| Token de Mercado Pago | La primera implementación cifra simétricamente la credencial de forma reversible antes de persistirla y jamás la expone en respuestas, logs o excepciones. | `PaymentCredentialVault` con `EncryptedDatabaseCredentialVault`. Puede cambiarse después a un gestor externo sin cambiar los casos de uso. La clave vive fuera de PostgreSQL (variable/secret del despliegue) y debe soportar rotación. `referencia_credencial` contiene el sobre cifrado, nunca el token en claro. |

## Recursos y visibilidad

| Recurso | Lectura | Escritura dentro del módulo |
| --- | --- | --- |
| Plantilla | `ADMIN_GENERAL` y vendedor autenticado | Solo `ADMIN_GENERAL` crea, edita, activa o desactiva. |
| Configuración y banners de una tienda | Su propietario; la lectura pública queda fuera del alcance | Solo su propietario. |
| Cuenta de pago de una tienda | Solo su propietario, sin credencial | Solo su propietario mediante OAuth/desconexión. |
| Inventario de una tienda | Solo su propietario | Nadie: es estrictamente de consulta en M3. |

## Historias de usuario

### HU-M3-01 — Crear una plantilla de tienda

**Como** `ADMIN_GENERAL`, **quiero** registrar la referencia de una nueva
plantilla, **para** que los vendedores puedan elegirla al configurar sus
tiendas.

**Criterios de aceptación**

1. El endpoint requiere `@PreAuthorize("hasRole('ADMIN_GENERAL')")`.
2. Recibe `codigo`, `nombre`, `previewUrl` opcional y `activa` opcional (por
   defecto `true`); no recibe ni guarda HTML, CSS, JavaScript ni binarios.
3. `codigo` y `nombre` se recortan; ambos son obligatorios y no pueden quedar
   vacíos. El `codigo` es único conforme a la restricción de BD.
4. Al duplicar el código responde `409 Conflict` con un código de error
   funcional, sin filtrar detalles SQL.
5. Devuelve `201 Created` con la representación de la plantilla, sin exponer
   información interna inexistente.

**Contrato orientativo:** `POST /api/admin/plantillas`.

### HU-M3-02 — Administrar el ciclo de vida de una plantilla

**Como** `ADMIN_GENERAL`, **quiero** editar los metadatos y activar o desactivar
una plantilla, **para** mantener disponible solo el catálogo visual aprobado.

**Criterios de aceptación**

1. Solo `ADMIN_GENERAL` puede actualizar `nombre`, `previewUrl` y `activa`.
2. El `codigo` es una referencia estable para el frontend y no se modifica una
   vez creada; si se requiere reemplazarlo se crea otra plantilla.
3. Desactivar una plantilla no altera configuraciones existentes que ya la
   referencian; estas siguen pudiendo leerse.
4. Una plantilla inactiva no puede ser elegida en una configuración nueva o al
   actualizar una existente.
5. Si la plantilla no existe se devuelve `404`; si la solicitud no es válida,
   `400`; si no tiene permiso, `403`.

**Contratos orientativos:** `PATCH /api/admin/plantillas/{idPlantilla}` y
`PATCH /api/admin/plantillas/{idPlantilla}/estado`.

### HU-M3-03 — Consultar las plantillas disponibles

**Como** `ADMIN_GENERAL` o vendedor, **quiero** ver el catálogo de plantillas,
**para** administrarlo o escoger una para mi tienda.

**Criterios de aceptación**

1. Ambos roles autenticados pueden listar plantillas activas.
2. `ADMIN_GENERAL` puede solicitar también las inactivas para administrarlas;
   el vendedor no puede seleccionarlas aunque consiga su identificador.
3. La respuesta contiene `idPlantilla`, `codigo`, `nombre`, `previewUrl` y
   `activa`; no depende de recursos del frontend.
4. Se puede obtener el detalle de una plantilla por identificador con la misma
   regla de visibilidad.

**Contratos orientativos:** `GET /api/plantillas?incluirInactivas=false` y
`GET /api/plantillas/{idPlantilla}`.

### HU-M3-04 — Consultar y configurar la apariencia de mi tienda

**Como** vendedor propietario, **quiero** consultar y actualizar la
configuración visual de mi tienda, **para** definir la plantilla y el contenido
base de mi página inicial.

**Criterios de aceptación**

1. El vendedor debe ser propietario de `{idTienda}`; un `DUENO_TIENDA` no
   puede leer ni modificar una tienda ajena. El `idUsuario` proviene del
   principal autenticado.
2. La respuesta de lectura contiene `idTienda`, plantilla seleccionada,
   `tituloInicio`, `descripcionInicio`, `mostrarCarrusel` y
   `fechaActualizacion`.
3. La actualización acepta explícitamente los campos modificables y rechaza
   campos desconocidos o el cambio de `idTienda`.
4. Cuando cambia `idPlantilla`, el servicio verifica que exista y esté activa
   en la misma transacción antes de persistirla.
5. `tituloInicio`, si se envía, no puede ser solo espacios ni superar 160
   caracteres. `descripcionInicio` puede ser nula. Se actualiza
   `fecha_actualizacion` en cada modificación efectiva.
6. Si falta la configuración por una integración incompleta del módulo 1, se
   devuelve un error de dependencia controlado; no se crea silenciosamente con
   una plantilla arbitraria. La creación inicial corresponde a RN-M3-02.
7. Se rechaza la edición de tiendas suspendidas o cerradas según la política
   indicada en la tabla de integraciones.

**Contratos orientativos:** `GET /api/tiendas/{idTienda}/configuracion` y
`PATCH /api/tiendas/{idTienda}/configuracion`.

### HU-M3-05 — Gestionar banners de mi tienda

**Como** vendedor propietario, **quiero** listar, crear, editar, activar,
desactivar, eliminar y reordenar los banners de mi tienda, **para** controlar
el carrusel o las promociones visibles en ella.

**Criterios de aceptación**

1. Cada operación verifica propiedad y estado administrable de la tienda.
2. Al crear un banner el backend recibe el archivo mediante `multipart/form-data`,
   valida que sea una imagen permitida y genera una `storageKey` propia, no
   predecible y única. Después sube el binario al bucket S3 compatible y
   persiste la clave junto con `orden` positivo, `enlaceDestino` opcional y
   `activo=true`.
3. Los límites de tamaño, tipos MIME permitidos, bucket, endpoint S3, región y
   prefijo de claves se externalizan en propiedades de aplicación. La validación
   no confía únicamente en el `Content-Type` enviado por el cliente.
4. Si se indica `enlaceDestino`, debe ser una URL absoluta `https`; se rechazan
   otros esquemas, URLs relativas y texto inválido.
5. La lista se entrega ordenada ascendentemente por `orden` y expone solo
   metadatos del banner, no credenciales ni contenido de almacenamiento.
6. La edición permite cambiar enlace y estado. La sustitución de imagen recibe
   un nuevo archivo, crea una nueva `storageKey` y actualiza el registro sin
   mover el banner a otra tienda. El objeto anterior se elimina solo después
   del commit; si esa limpieza falla, queda una tarea idempotente de reintento.
7. La operación de reordenar recibe el conjunto completo de pares
   `idBanner`/`orden`, sin repetidos, compuesto exactamente por los banners de
   la tienda. Se ejecuta en una transacción; la restricción diferible
   `uq_banner_tienda_orden` valida el resultado al confirmar.
8. Eliminar un banner borra el registro y solicita al `BannerStoragePort` la
   limpieza del objeto solo después de confirmar la transacción. Si la limpieza
   externa falla, queda un evento/reintento y nunca se revierte la BD.
9. Si la subida al storage falla, no se crea el registro en la BD. Si falla la
   persistencia después de una subida exitosa, el backend intenta borrar el
   objeto y registra una limpieza pendiente si no puede hacerlo.

**Contratos orientativos:**

- `GET /api/tiendas/{idTienda}/banners`
- `POST /api/tiendas/{idTienda}/banners` (`multipart/form-data`, campo `archivo`)
- `PATCH /api/tiendas/{idTienda}/banners/{idBanner}`
- `PUT /api/tiendas/{idTienda}/banners/{idBanner}/archivo` (`multipart/form-data`)
- `PUT /api/tiendas/{idTienda}/banners/orden`
- `DELETE /api/tiendas/{idTienda}/banners/{idBanner}`

### HU-M3-06 — Consultar el stock del catálogo de mi tienda

**Como** vendedor propietario, **quiero** consultar el stock de las
presentaciones de mis productos, **para** conocer su disponibilidad sin
modificar inventario desde este módulo.

**Criterios de aceptación**

1. Solo el propietario de la tienda puede consultar sus existencias; no basta
   con tener el rol `DUENO_TIENDA`.
2. La consulta une presentaciones, productos e inventario y se filtra por
   `presentacion_producto.id_tienda = {idTienda}`. Nunca acepta una lista de
   identificadores de presentación sin validar que pertenece a la tienda.
3. Cada fila expone los identificadores y datos de presentación necesarios para
   el panel, `stockActual`, `stockMinimo` y `fechaActualizacion`.
4. Permite filtros seguros y paginación: texto de producto/SKU, estado de
   producto o presentación y alerta de stock (`stockActual <= stockMinimo`).
5. No existe en M3 ningún endpoint, método de servicio ni repositorio público
   para crear, actualizar, reservar o descontar inventario. Las mutaciones
   quedan para catálogo/checkout (RN-M3-10 a RN-M3-13).
6. Una presentación sin inventario indica una inconsistencia de la integración
   con M2; se puede reportar como `sinRegistroInventario` para diagnóstico,
   pero el endpoint no crea ni repara registros de forma implícita.

**Contrato orientativo:**
`GET /api/tiendas/{idTienda}/inventario?texto=&alertaStock=&page=&size=`.

### HU-M3-07 — Consultar el estado de facturación de mi tienda

**Como** vendedor propietario, **quiero** conocer el estado de la cuenta de
Mercado Pago de mi tienda, **para** saber si está habilitada para cobrar.

**Criterios de aceptación**

1. Solo el propietario puede leer la cuenta de pago de su tienda.
2. Devuelve únicamente `proveedor`, `estado`, `idCuentaExterna` (si la política
   de privacidad permite mostrarlo, preferiblemente enmascarado),
   `fechaVinculacion` y `fechaActualizacion`.
3. Nunca devuelve `referenciaCredencial`, el access token, refresh token,
   `client_secret`, códigos OAuth ni cabeceras de terceros.
4. Si aún no existe el registro, la respuesta representa estado `PENDIENTE` o
   devuelve `404` de recurso no configurado; se debe escoger una convención
   única para toda la API. Se recomienda crear la fila junto con la tienda y
   responder `PENDIENTE`, conforme a RN-M3-01.

**Contrato orientativo:** `GET /api/tiendas/{idTienda}/cuenta-pago`.

### HU-M3-08 — Iniciar la afiliación OAuth de Mercado Pago

**Como** vendedor propietario, **quiero** iniciar la vinculación OAuth de
Mercado Pago para mi tienda, **para** autorizar que la plataforma cobre en mi
nombre.

**Criterios de aceptación**

1. Solo el propietario de una tienda administrable puede iniciar el flujo.
2. El backend genera un `state` criptográficamente aleatorio, de un solo uso,
   ligado a `idTienda`, usuario autenticado, URI de retorno permitida y fecha
   de expiración corta. No se coloca el identificador de tienda sin protección
   en parámetros confiados al cliente.
3. Se registra el intento como `PENDIENTE` sin activar la cuenta ni persistir
   tokens antes del callback exitoso.
4. La respuesta devuelve la URL de autorización generada por
   `MercadoPagoOAuthPort`; los secretos de cliente permanecen solo en
   configuración del servidor.
5. Iniciar una nueva afiliación invalida los estados OAuth pendientes previos
   para la misma tienda, o los hace inelegibles para consumo.
6. El flujo requiere HTTPS fuera de desarrollo local y no registra en logs
   `state`, `code` ni tokens completos.

**Contrato orientativo:**
`POST /api/tiendas/{idTienda}/cuenta-pago/mercado-pago/oauth/inicios`.

### HU-M3-09 — Completar una afiliación OAuth de Mercado Pago

**Como** vendedor que autorizó en Mercado Pago, **quiero** que el callback
valide y guarde de forma segura mi vinculación, **para** habilitar los cobros
de mi tienda.

**Criterios de aceptación**

1. El callback valida el `state`, su expiración, uso único y la correspondencia
   con tienda y usuario antes de intercambiar el `code`.
2. El backend intercambia el código exclusivamente servidor a servidor y valida
   la respuesta del proveedor. Un callback cancelado, denegado, inválido o con
   fallo de red no activa la cuenta.
3. Tras obtener una respuesta válida, identifica la cuenta externa y almacena
   las credenciales mediante `PaymentCredentialVault`; solo persiste su
   referencia opaca/cifrada en `referencia_credencial`.
4. En una única transacción actualiza o crea `cuenta_pago_tienda` con proveedor
   `MERCADO_PAGO`, `id_cuenta_externa`, referencia, `estado=ACTIVA`,
   `fecha_vinculacion` y `fecha_actualizacion`. Esto satisface la restricción
   `chk_cuenta_pago_activa`.
5. Si la cuenta externa ya está vinculada a otra tienda, la restricción única se
   traduce a un conflicto funcional y la nueva tienda permanece sin activar.
6. La respuesta/redirección de éxito no contiene tokens. En error se marca
   `ERROR` únicamente cuando hay una falla persistente y explicable; un error
   temporal se conserva como `PENDIENTE` para permitir reintento.
7. El `state` se consume incluso si falla el canje, para impedir su repetición.

**Contrato orientativo:**
`GET /api/integraciones/mercado-pago/oauth/callback?code=&state=`. Esta ruta
debe estar permitida en Security para recibir el callback, pero su autorización
se basa en el `state` validado, no en un id de tienda recibido por query string.

### HU-M3-10 — Desconectar la cuenta de Mercado Pago

**Como** vendedor propietario, **quiero** desconectar mi cuenta de Mercado
Pago, **para** impedir nuevos cobros con esa autorización.

**Criterios de aceptación**

1. Solo el propietario de la tienda puede solicitar la desconexión.
2. La operación deja el registro en estado `DESCONECTADA`, actualiza su fecha
   y revoca/elimina la credencial mediante el vault. Si Mercado Pago ofrece
   revocación, se intenta después del commit y se registra una tarea de
   reintento si falla.
3. No elimina el historial de estado de forma silenciosa ni expone secretos.
4. El módulo 4 debe interpretar exclusivamente `ACTIVA` como cuenta apta para
   cobrar; una cuenta `DESCONECTADA`, `ERROR` o `PENDIENTE` bloquea nuevos
   cobros.

**Contrato orientativo:**
`POST /api/tiendas/{idTienda}/cuenta-pago/desconexion`.

## Reglas transversales de implementación

### Autorización

* Los endpoints administrativos de plantilla usan
  `@PreAuthorize("hasRole('ADMIN_GENERAL')")`.
* Los endpoints por tienda usan una expresión equivalente a
  `@PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")`.
  La implementación consulta la tienda; nunca acepta el propietario desde el
  cuerpo, cabecera o query string.
* Toda ruta no pública requiere autenticación. La futura cookie debe tener
  `HttpOnly`, `Secure` en producción y una política `SameSite` compatible con
  el frontend. Al introducir cookies se debe habilitar una estrategia CSRF;
  no conservar indefinidamente el `csrf().disable()` actual.

### Validación y errores

* Los DTO usan Bean Validation; los servicios repiten las reglas de negocio y
  las operaciones que cambian varias filas son `@Transactional`.
* Errores de restricciones de PostgreSQL se traducen a una respuesta uniforme:
  `400` para formato/validación, `401` no autenticado, `403` no autorizado,
  `404` inexistente, `409` conflicto y `502/503` para fallos de servicios
  externos. No se exponen SQL, claves, tokens ni stack traces.
* Las operaciones externas —storage S3 compatible, Mercado Pago y vault— no se invocan dentro de
  una transacción abierta. Cuando dependen de una escritura confirmada, se usan
  eventos post-commit y reintentos idempotentes.

### Datos y contratos que pertenecen a otros módulos

* Módulo 1 crea `configuracion_tienda` con una plantilla por defecto y
  `cuenta_pago_tienda` en estado `PENDIENTE` al crear la tienda. Debe existir al
  menos una plantilla activa inicial antes de habilitar la creación de tiendas.
* Módulo 2 crea el registro `inventario` junto con cada presentación y es dueño
  de toda modificación manual de stock. Módulo 4 realiza reservas y descuentos
  transaccionales.
* Las plantillas se referencian por `codigo`; eliminar físicamente una plantilla
  no es un caso de uso porque la FK podría afectar configuraciones históricas.
  Se desactiva.
* La exposición pública de configuración, banners y catálogo para la vitrina de
  una tienda no pertenece a esta primera versión administrativa; se agregará
  como contrato público separado si el módulo responsable lo requiere.

## Matriz de trazabilidad con las reglas del proyecto

| Regla | Historias que la satisfacen |
| --- | --- |
| RN-M3-01 y RN-M3-02 | HU-M3-04, HU-M3-07; creación inicial delegada al módulo 1. |
| RN-M3-03 y RN-M3-04 | HU-M3-01 a HU-M3-04. |
| RN-M3-05 y RN-M3-06 | HU-M3-05. |
| RN-M3-07 a RN-M3-09 | HU-M3-07 a HU-M3-10. |
| RN-M3-10 a RN-M3-13 | HU-M3-06 delimita consulta; creación y mutaciones se delegan a M2/M4. |
