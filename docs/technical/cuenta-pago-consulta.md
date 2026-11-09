# Consulta de cuenta de pago: HU-M3-07

`GET /api/tiendas/{idTienda}/cuenta-pago` requiere autenticación y titularidad de la tienda. Devuelve `proveedor`, `estado`, `idCuentaExterna`, `fechaVinculacion` y `fechaActualizacion`. El identificador externo es nulo si no existe; si tiene hasta cuatro caracteres se oculta por completo y, si tiene más, solo se muestran los últimos cuatro.

Si la tienda autorizada no tiene cuenta configurada, responde 404 con código `CUENTA_PAGO_NO_CONFIGURADA`. Las tiendas ajenas o inexistentes producen acceso denegado. La operación es de solo lectura, no consulta el campo de credenciales y no llama a Mercado Pago. La creación inicial de la cuenta queda a cargo de la integración con el registro de tienda.
