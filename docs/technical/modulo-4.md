# Módulo 4: estado de implementación

La vitrina permite agregar presentaciones al carrito, iniciar sesión, ajustar cantidades, gestionar direcciones y consultar pedidos existentes. El panel del vendedor permite consultar pedidos existentes y el administrador puede programar una nueva vigencia de IGV. Los precios del carrito son actuales y agregar productos no reserva inventario.

El checkout está conectado a la reserva idempotente, al token OAuth del vendedor y a Checkout Pro vía Orders. Incluye cola de notificaciones, conciliación, renovación OAuth, vencimientos y reembolsos completos desde el panel, con reposición opcional de stock. Las operaciones inciertas conservan sus claves y no liberan inventario sin resolución.

Los cobros permanecen deshabilitados por defecto. La validación con Mercado Pago y la activación publicada son pasos separados de la implementación local. Seguir [pagos: activación y operación](pagos-operacion.md) para configurar las tiendas autorizadas y realizar primero pruebas y después una compra real pequeña. Se conservan los precios con IGV, comprador registrado, una tienda por pedido, sin comisión Nubea ni cobro de envío.

## Google Places y direcciones

El autocompletado aún no está habilitado. Se acordó utilizarlo en el checkout para una transacción concreta y mantener manual la libreta reutilizable. La [política de Places](https://developers.google.com/maps/documentation/places/web-service/policies#autocomplete_for_end_user_addresses) contempla una excepción para la dirección postal seleccionada por el usuario únicamente para su transacción específica; no extiende esa excepción a sugerencias ni a otros datos de Google. Antes de integrarlo, revisar las condiciones aplicables a la cuenta. No guardar coordenadas, identificadores, sugerencias ni otros detalles de Places. El comprador debe confirmar el UBIGEO; las coincidencias ambiguas requieren selección manual.

Para una integración autorizada, Nubea deberá habilitar facturación, Maps JavaScript API y Places API (New), usar una clave de navegador restringida por API y por los dominios HTTPS de producción y vista previa, y restringir las claves de desarrollo a sus orígenes locales. Revisar los costes y cuotas de los campos solicitados. El ingreso manual y la selección de UBIGEO deben permanecer disponibles ante la ausencia de clave o fallos del servicio.

## API autenticada

Todas las rutas usan el usuario de la sesión y las mutaciones requieren CSRF:

- `GET /api/cliente/ubigeo?texto=...` busca distritos; `GET/POST /api/cliente/direcciones` y `PUT/DELETE /api/cliente/direcciones/{id}` gestionan únicamente direcciones del usuario.
- `GET /api/compra/tiendas/{slug}/carrito` consulta el carrito; `POST .../items/{idPresentacion}` suma unidades, `PUT` reemplaza la cantidad y `DELETE` elimina una línea. POST y PUT reciben `{ "cantidad": 1 }`; se admiten de 1 a 999 unidades.
- `GET /api/compra/tiendas/{slug}/pedidos` y `GET .../pedidos/{id}` consultan los pedidos del comprador. El propietario consulta `GET /api/tiendas/{idTienda}/pedidos` y `GET .../pedidos/{id}`.
- `GET /api/admin/impuestos` lista tasas y `POST` recibe `{ "tasa": 18.00, "inicio": "AAAA-MM-DD" }`. Solo un administrador puede programar una vigencia futura. La fecha final de la vigencia anterior es exclusiva.

## Carga de UBIGEO

La migración inicial crea la tabla sin datos; hay que cargarla antes de crear direcciones. Obtener la lista del [portal de datos abiertos](https://www.datosabiertos.gob.pe/dataset/datos-de-registros-de-nacidos-vivos/resource/573f433b-0d44-46e2-932e-14abb9757860), revisar su vigencia y normalizarla a CSV UTF-8 con cabecera exacta `ubigeo,departamento,provincia,distrito`. Mantener los ceros iniciales de los códigos. Generar y revisar el SQL sin conectarse a la base:

```sh
python3 scripts/preparar_ubigeo.py ubigeo.csv > ubigeo.sql
```

El script valida el archivo completo antes de producir una transacción de inserciones o actualizaciones; rechaza códigos inválidos y duplicados y no elimina ubicaciones que ya existan. Aplicar el SQL revisado con `psql` usando una conexión autorizada. No incluir credenciales en el archivo ni en el repositorio.
