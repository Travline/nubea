# Consulta de inventario: HU-M3-06

`GET /api/tiendas/{idTienda}/inventario` requiere autenticación y titularidad de la tienda. No modifica inventario.

Parámetros opcionales: `texto` busca literalmente, sin distinguir mayúsculas, en nombre de producto o SKU; `estadoProducto` admite `BORRADOR`, `PUBLICADO` o `INACTIVO`; `estadoPresentacion` admite `ACTIVA` o `INACTIVA`; `alertaStock` admite `true` o `false`. La alerta compara stock actual y mínimo y excluye presentaciones sin registro de inventario. Sin filtro de alerta estas se incluyen con `sinRegistroInventario: true` y campos de inventario nulos.

`page` comienza en 0 y por defecto vale 0; `size` por defecto vale 20 y admite entre 1 y 100. El resultado contiene `contenido`, `page`, `size`, `totalElementos` y `totalPaginas`. Cada fila incluye identificadores, nombre, SKU y estados del producto y presentación, además de `stockActual`, `stockMinimo`, `fechaActualizacion` y `sinRegistroInventario`. El orden es nombre, SKU e identificador de presentación.

Los parámetros inválidos producen 400 y el acceso a una tienda ajena produce 403. La consulta no crea registros de inventario faltantes.
