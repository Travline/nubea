# Módulo 4: estado de implementación

La vitrina permite agregar presentaciones al carrito, iniciar sesión, ajustar cantidades, gestionar direcciones y consultar pedidos existentes. El panel del vendedor permite consultar pedidos existentes y el administrador puede programar una nueva vigencia de IGV. Los precios del carrito son actuales y agregar productos no reserva inventario.

El carrito muestra las direcciones guardadas y permite elegir una para preparar la compra. La selección es temporal: todavía no crea una reserva ni un pedido y debe elegirse nuevamente al recargar.

**Pendiente:** el checkout y la creación de pedidos no están disponibles mediante la API ni la vitrina. Existe un núcleo interno de reserva transaccional probado de forma inicial, que aún no está conectado al pago ni listo para exponerse. Los cobros con Mercado Pago, la renovación OAuth, los webhooks, la conciliación, el vencimiento y los reembolsos todavía no están implementados. No se debe habilitar un flujo de cobro hasta completarlos y probarlos. La cuenta de pago no se puede desconectar mientras haya pedidos o pagos sin resolver.

La reserva interna admite una clave de idempotencia por comprador y tienda y comprueba el total esperado antes de descontar stock. Su registro se crea en la misma transacción que el pedido. Esta capacidad todavía no constituye una API de checkout ni gestiona el vencimiento de las reservas.

Se han añadido componentes internos para verificar la firma de webhooks y enviar solicitudes de creación a Orders API, probados con un servidor simulado. La reserva idempotente registra un intento durable y una clave estable del proveedor en la misma transacción. Estos componentes no están conectados a una ruta pública ni procesan pagos; no se deben registrar webhooks ni habilitar cobros basándose en ellos. Queda por validar con Mercado Pago la compatibilidad de OAuth de vendedores con Orders en Perú, el vencimiento de 30 minutos y la exclusión completa de medios offline.

## Decisiones y trabajo restante de pagos

El comprador usa una cuenta de Nubea. Cada pedido corresponde a una tienda y se pretende cobrar con el token OAuth del vendedor de esa tienda; la aplicación de Nubea tiene sus propias credenciales OAuth. No se prevén comisión adicional de Nubea, pagos offline, cobro de envío ni compras como invitado. Mercado Pago puede cobrar sus propias tarifas al vendedor. El retorno del navegador deberá mostrar el estado consultado al backend y no confirmar el pago por sí solo.

La aplicación de Mercado Pago se configuró para Checkout Pro con Orders API. El cliente interno `MercadoPagoOrders` propone `PT30M`, excluye el tipo `ticket` y genera el retorno suministrado por el llamador, pero todavía no se ha comprobado con el proveedor que esos valores cubran el flujo deseado. El código de webhook comprueba la firma: no existe un endpoint público, cola durable, consulta de Orders, ni procesamiento de eventos. El estado ACTIVA de una cuenta indica la vinculación local, no que el pago esté disponible ni que la autorización siga vigente en el proveedor.

Antes de ofrecer checkout hay que completar y probar: operación autenticada y resumen; persistencia y recuperación de llamadas inciertas usando la misma clave; consulta de Orders con validación de vendedor, referencia, moneda e importe; webhook autenticado, durable e idempotente; conciliación y reintentos; expiración segura de reservas, cancelación y reembolso de aprobaciones tardías; renovación coordinada y revocación OAuth; aislamiento entre entornos de prueba y producción; y observabilidad sin secretos. No liberar stock mientras un pago pueda completarse o su estado sea incierto. Las llamadas al proveedor deben realizarse fuera de las transacciones de reserva.

Después de las pruebas simuladas se necesitan cuentas de prueba de vendedor y comprador, y validar el flujo OAuth de terceros y Orders en Perú. Configurar el webhook de prueba y su firma solo cuando exista el endpoint; la validación real y la activación de cobros son pasos posteriores. No cargar secretos ni identificadores de pago en la documentación.

## Verificación realizada

Se ejecutaron `CompraIntegracionTest`, `FirmaWebhookMercadoPagoTest` y `MercadoPagoOrdersTest` con Java 21 en Docker, PostgreSQL 18 temporal y proveedor simulado. La migración V7 se aplicó a una base vacía; se comprobó el doble envío y la conservación de la clave del proveedor. Esto no prueba llamadas a Mercado Pago, recuperación de pagos, concurrencia completa ni el despliegue actual. Los artefactos y cambios locales solo llegan a Render o Vercel después de publicarse en la rama desplegada.

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
