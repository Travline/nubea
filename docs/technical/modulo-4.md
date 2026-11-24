# Módulo 4: estado de implementación

La vitrina permite agregar presentaciones al carrito, iniciar sesión, ajustar cantidades, gestionar direcciones y consultar pedidos existentes. El panel del vendedor permite consultar pedidos existentes y el administrador puede programar una nueva vigencia de IGV. Los precios del carrito son actuales y agregar productos no reserva inventario.

El checkout está conectado a la reserva idempotente, al token OAuth del vendedor y a Checkout Pro vía Orders. Incluye cola de notificaciones, conciliación, renovación OAuth, vencimientos y reembolsos completos desde el panel, con reposición opcional de stock.

Cambios del Bloque 3 (ver [pagos: operación](pagos-operacion.md) y `docs/functional/modules-rules.md`):

- **Checkout parcial (RN-M4-05):** se compran solo las líneas disponibles; las demás pasan a un carrito activo nuevo. Una línea con más unidades que el stock se aparta entera.
- **Vencimiento siempre activo (RN-M4-19):** las reservas vencidas se cancelan aunque los pagos estén deshabilitados. Una creación de orden incierta libera el stock pasada una gracia de 40 minutos (la orden de Mercado Pago expira a los 30); un pago tardío se reembolsa automáticamente.
- **Vuelta al carrito (RN-M4-20):** al vencer una reserva, sus productos vuelven al carrito activo del comprador.
- **Número corto de pedido (RN-M4-24, V14):** #1001, #1002… por tienda, en todas las respuestas de pedidos.

Los cobros permanecen deshabilitados por defecto. La validación con Mercado Pago y la activación publicada son pasos separados de la implementación local. Seguir [pagos: activación y operación](pagos-operacion.md) para configurar las tiendas autorizadas y realizar primero pruebas y después una compra real pequeña. Se conservan los precios con IGV, comprador registrado, una tienda por pedido, sin comisión Nubea ni cobro de envío.

## Direcciones con Google Places (RN-M4-01 v1.2)

Decisión del equipo (opción A): la dirección se registra con **Google Places** o como **texto libre**, y `codigo_ubigeo` es **opcional**. Cada usuario tiene como máximo una dirección principal (`uq_direccion_principal_usuario` + `DireccionesController`, que desmarca la anterior).

- El formulario de dirección usa el autocompletado de Places (`PlaceAutocompleteElement`) cuando hay clave de Maps; se guarda solo el `place_id` (V12, `direccion_cliente.place_id`), que Google permite almacenar. Departamento, provincia y distrito se guardan como texto (V12) y el pedido copia la dirección y el `place_id` en su snapshot (`place_id_entrega`).
- Sin clave de Maps o ante un fallo del servicio, el usuario escribe la dirección a mano; el flujo no depende de Google.
- Si se envía `codigoUbigeo`, el backend comprueba que exista en la tabla `ubigeo`; si no se envía, la dirección es válida igualmente.
- No se guardan coordenadas, sugerencias ni otros datos de Places.

Configuración: habilitar Maps JavaScript API y Places API (New) con facturación y usar una clave de navegador restringida por API y por los dominios HTTPS de producción y vista previa (`VITE_GOOGLE_MAPS_API_KEY` en cms y store; ver [google-maps.md](google-maps.md)). Revisar costes y cuotas de los campos solicitados.

**Trabajo futuro:** el UBIGEO pasará a ser obligatorio para las direcciones de entrega cuando exista el módulo de envíos (`modules-rules.md`, trabajo futuro).

## API autenticada

Todas las rutas usan el usuario de la sesión y las mutaciones requieren CSRF:

- `GET /api/cliente/ubigeo?texto=...` busca distritos; `GET/POST /api/cliente/direcciones` y `PUT/DELETE /api/cliente/direcciones/{id}` gestionan únicamente direcciones del usuario.
- `GET /api/compra/tiendas/{slug}/carrito` consulta el carrito; `POST .../items/{idPresentacion}` suma unidades, `PUT` reemplaza la cantidad y `DELETE` elimina una línea. POST y PUT reciben `{ "cantidad": 1 }`; se admiten de 1 a 999 unidades. Cada línea indica `disponible` (hay stock suficiente) y `valida` (versión activa de un producto publicado). Además de `total` (todas las líneas), la respuesta trae `subtotal`, `igv`, `tasaIgv` y `totalAPagar`, calculados solo con las líneas que se van a cobrar y con el mismo cálculo que el checkout.
- Si la tienda no está activa, la vitrina, el carrito y el checkout responden 404 `TIENDA_NO_DISPONIBLE`; los pedidos del comprador siguen visibles.
- `GET /api/compra/tiendas/{slug}/pedidos` y `GET .../pedidos/{id}` consultan los pedidos del comprador. El propietario consulta `GET /api/tiendas/{idTienda}/pedidos` y `GET .../pedidos/{id}` (el administrador no: contienen datos del comprador). Cada pedido incluye `numero`.
- `GET /api/admin/impuestos` lista tasas y `POST` recibe `{ "tasa": 18.00, "inicio": "AAAA-MM-DD" }`. Solo un administrador puede programar una vigencia futura. La fecha final de la vigencia anterior es exclusiva.

## Carga de UBIGEO (opcional)

Con RN-M4-01 v1.2 la tabla `ubigeo` es **opcional**: solo se usa si el frontend envía `codigoUbigeo` (`GET /api/cliente/ubigeo?texto=...`), y será necesaria cuando se implementen los envíos. Si se quiere cargar, obtener la lista del [portal de datos abiertos](https://www.datosabiertos.gob.pe/dataset/datos-de-registros-de-nacidos-vivos/resource/573f433b-0d44-46e2-932e-14abb9757860), revisar su vigencia y normalizarla a CSV UTF-8 con cabecera exacta `ubigeo,departamento,provincia,distrito`. Mantener los ceros iniciales de los códigos. Generar y revisar el SQL sin conectarse a la base:

```sh
python3 scripts/preparar_ubigeo.py ubigeo.csv > ubigeo.sql
```

El script valida el archivo completo antes de producir una transacción de inserciones o actualizaciones; rechaza códigos inválidos y duplicados y no elimina ubicaciones que ya existan. Aplicar el SQL revisado con `psql` usando una conexión autorizada. No incluir credenciales en el archivo ni en el repositorio.
