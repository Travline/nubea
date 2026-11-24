# Pagos de la tienda: activación y comprobación

El checkout redirige a Checkout Pro vía Orders API usando el token OAuth de la tienda. Las credenciales de la aplicación Nubea se usan para OAuth, no como sustituto del token del vendedor. No hay comisión adicional de Nubea. Los precios incluyen IGV; se permiten tarjetas de crédito y débito, sin cobro de envío. Las exclusiones de medios de pago se consultan al proveedor y se congelan al reservar.

## Configuración del despliegue

Conservar las variables OAuth actuales y, especialmente, las claves de cifrado. No desconectar la cuenta real para ejecutar pruebas. Las migraciones V9–V11 no clasifican ni reemplazan credenciales existentes. Las cuentas se verifican mediante `/users/me` (identidad, país MPE y etiqueta de usuario de prueba), sin deducir su ambiente a partir del prefijo del token.

Configurar en Render, nunca en variables `VITE_*`:

```dotenv
MERCADOPAGO_OAUTH_ENABLED=true
MERCADOPAGO_PAGOS_ENABLED=true
MERCADOPAGO_PAGOS_CHECKOUT_ENABLED=false
MERCADOPAGO_PAGOS_STORE_ORIGIN=https://nubea.vercel.app
MERCADOPAGO_PAGOS_TIENDAS=<UUID_TIENDA_PRUEBA>:PRUEBA
MERCADOPAGO_NOTIFICACIONES_ENABLED=true
MERCADOPAGO_NOTIFICACIONES_SECRET=<firma configurada en Mercado Pago>
```

`PAGOS_ENABLED` inicia API y procesadores y requiere OAuth configurado. `CHECKOUT_ENABLED` habilita solamente nuevas compras de las tiendas enumeradas en `TIENDAS`, separadas por comas. Desactivar checkout conserva conciliación y reembolsos. No apagar los procesadores mientras haya operaciones sin resolver.

Configurar las notificaciones de Orders en la aplicación de Mercado Pago con URL:

`https://nubea.onrender.com/api/publico/mercadopago/notificaciones`

El endpoint acepta el evento `type=order`, verifica `x-signature` y persiste antes de responder. El `user_id` del cuerpo es únicamente una pista para recuperar órdenes cuya respuesta se perdió: la asociación exige una consulta autenticada y coincidencia de vendedor, referencia, moneda e importe.

Las variables completas, agrupadas, están en [`.env.render.example`](../../.env.render.example). Publicar frontend y backend, verificar que Flyway llegue a **V14** (`SELECT max(version) FROM flyway_schema_history;`) y que la sesión y CSRF funcionen desde Vercel. El procesador de pagos y el vencimiento de reservas deben permanecer ejecutándose; si Render suspende la instancia, las tareas se recuperan al reiniciar, pero no hay procesamiento continuo durante la suspensión y las reservas vencidas retienen stock hasta entonces.

## Secuencia de prueba publicada

1. Crear una tienda Nubea separada y conectar un vendedor de prueba de Mercado Pago mediante OAuth. Usar un comprador de prueba distinto. Las credenciales de la aplicación ya configuradas no sustituyen estas cuentas.
2. Agregar un producto con stock y una dirección válida. Incluir únicamente el UUID de esta tienda con ambiente `PRUEBA` en la lista y activar `CHECKOUT_ENABLED=true`.
3. Comprar desde `/tienda/<slug>/carrito`, comprobar el aviso de prueba y la redirección. Verificar el retorno a `/tienda/<slug>/pedidos/<id>`, el pago aprobado y su aparición en el panel.
4. Comprobar rechazo, doble clic, recarga, abandono, vencimiento y notificaciones. Un fallo de red conserva la solicitud en la pestaña para «Recuperar compra». No debe aparecer un segundo pedido ni descontarse nuevamente stock.
5. Desde el panel, reembolsar el pedido completo. La opción de reponer stock está desmarcada por defecto; confirmar que solo se repone una vez si se selecciona. Un reembolso pendiente no equivale a dinero devuelto.
6. Después de validar el flujo, añadir la tienda real como `<UUID_TIENDA_REAL>:PRODUCCION`. Realizar una compra pequeña con un comprador distinto del vendedor, sin mezclar datos de prueba con datos reales.
7. Comparar la referencia del panel con la operación en la cuenta Mercado Pago del vendedor. Revisar importe bruto, comisiones, neto y disponibilidad del dinero allí. La aprobación del pedido no certifica por sí sola que el saldo esté disponible para retirar.
8. Reembolsar esa compra desde el panel y verificar la devolución en Mercado Pago. No automatizar compras reales ni introducir datos de tarjetas desde scripts de prueba.

Las pruebas locales con servidor simulado no certifican la habilitación de Orders/OAuth de esta aplicación en Perú. Antes del pago real debe completarse la prueba publicada y comprobar que Mercado Pago acepta la vigencia de 30 minutos y muestra únicamente los medios de pago previstos.

## Operación y recuperación

- Las tareas conservan claves de creación, cancelación y devolución. No cambiar claves para resolver timeouts.
- Cada pedido congela cuenta receptora, ambiente, retorno, importes y exclusiones. Una reconexión diferente no permite cobrar o consultar pedidos del vendedor anterior.
- Una orden acreditada consume la reserva. Una orden cancelada o fallida libera stock una vez; las órdenes se crean sin reintentos del comprador dentro de la misma orden. Si una consulta o cancelación de una orden **creada** es incierta, el procesador conserva la reserva y reintenta.
- **Vencimiento de reservas (RN-M4-19, Bloque 3):** `VencimientoReservasService` corre siempre, aunque `MERCADOPAGO_PAGOS_ENABLED=false`, cada `RESERVAS_VENCIMIENTO_DELAY_MS`. Cada pedido se procesa en su propia transacción; bloquea intento → pedido (como el procesador) con `SKIP LOCKED` y salta los pedidos con un trabajo de pago en curso.
  - Sin orden en Mercado Pago (intento `PREPARADO`): se cancela al vencer.
  - **Creación incierta (`CREACION_INCIERTA`): ahora sí libera stock.** Ya no se reenvía después de vencer, y pasada la gracia (`RESERVAS_VENCIMIENTO_GRACIA_MINUTOS`, 40 por defecto) el pedido se cancela localmente y el stock vuelve al inventario. Es seguro porque las órdenes se crean con `expiration_time` de 30 minutos y solo antes del vencimiento: pasada la gracia ya no se pueden pagar. Si aun así llega un pago (el aviso recupera la orden), la acreditación sobre un pedido cancelado dispara la devolución automática.
  - Orden creada: con el procesador activo la cancela y confirma él en Mercado Pago (si sigue pendiente 24 h después del vencimiento, queda una advertencia en el log); con el procesador apagado, se cancela pasada la gracia.
  - **Las cancelaciones por vencimiento devuelven los productos al carrito** activo del comprador (sumando cantidades, máximo 999). Un pago rechazado no los devuelve.
- Una acreditación tardía sobre un pedido cancelado solicita devolución automática. No vuelve a modificar el inventario.
- Un reembolso completo externo se refleja por webhook o conciliación sin reponer stock. Una devolución parcial externa queda para revisión; no se ofrecen reembolsos parciales desde Nubea.
- La renovación OAuth coordina procesos con una reserva temporal en PostgreSQL y guarda access/refresh token juntos, cifrados. `invalid_grant` marca la cuenta `ERROR`; otros fallos conservan la vinculación para reintentar. Reconectar la misma cuenta permite resolver operaciones previas.
- El panel muestra incidencias y referencias. `PROVEEDOR_NO_CONFIRMADO` conserva el trabajo con espera creciente; `AUTORIZACION_PENDIENTE` intenta renovar; `REQUIERE_REVISION` exige revisar identidad, ambiente, credenciales o importes. No registrar respuestas OAuth, tokens ni enlaces completos del checkout.
- Revisar `intento_order_mercadopago` para estado, intentos, próximo intento y error; `notificacion_order_mercadopago` para versiones recibidas/procesadas; `reembolso_order` para solicitudes y reposición. Las tareas vencidas se pueden recuperar por otra instancia. Los pagos resueltos se consultan cada seis horas para detectar cambios sin notificación.

## API

- `GET /api/compra/tiendas/{slug}/checkout`: disponibilidad y ambiente, con sesión.
- `POST` a la misma ruta: `{idDireccion,totalEsperado,clave}`. La clave identifica la misma compra incluso tras perderse la respuesta; cambiar dirección o total con esa clave se rechaza.
  - **Checkout parcial (RN-M4-05):** se compran solo las líneas disponibles; una línea con más unidades que el stock se aparta entera y las no disponibles pasan a un carrito activo nuevo. `totalEsperado` debe ser el `totalAPagar` del carrito (solo líneas disponibles). Si no hay ninguna disponible, 422 con `campos.items`.
  - La respuesta incluye `numero` (número corto del pedido en la tienda, #1001…, V14) y `productosApartados` (solo en esta respuesta; 0 si se repite la clave).
- `GET /api/compra/tiendas/{slug}/pedidos/{id}/pago`: estado del comprador, enlace si aún está vigente, referencia, vencimiento y `numero`.
- `GET /api/tiendas/{idTienda}/pedidos/{id}/pago`: estado para el propietario (el `ADMIN_GENERAL` no tiene acceso: contiene datos del comprador).
- `POST /api/tiendas/{idTienda}/pedidos/{id}/reembolsos`: `{clave,reponerStock}`, solo propietario. La clave UUID y la opción deben permanecer iguales en cada reintento.

Las mutaciones de usuario requieren CSRF. Ninguna ruta acepta tokens, identidad del vendedor o confirmación de pago desde el navegador.

## Validación reproducible

Ejecutar `mvn test` con Java 21 y PostgreSQL aislado mediante `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. Las pruebas no llaman a Mercado Pago real. Las pruebas opcionales existentes de PostgreSQL requieren sus banderas explícitas. Ejecutar también `npm run build` y `npm run lint` en `apps/store` y `apps/cms`.

Referencias: [creación de Orders](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/create-order), [notificaciones](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/notifications), [reembolsos](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/refunds-cancellations).
