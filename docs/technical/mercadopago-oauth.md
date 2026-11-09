# Vinculación OAuth de Mercado Pago

El flujo está deshabilitado por defecto. Antes de habilitarlo, aplicar una sola vez `services/database/migrations/001_oauth_mercado_pago.sql` a bases existentes. Las bases nuevas reciben la tabla desde `services/database/init/db.sql`. M1 debe proporcionar autenticación cuyo principal sea el UUID del usuario.

Configurar propiedades Spring por entorno, sin guardar secretos en el repositorio:

- `MERCADOPAGO_OAUTH_ENABLED=true`
- `MERCADOPAGO_OAUTH_CLIENT_ID` y `MERCADOPAGO_OAUTH_CLIENT_SECRET`: credenciales de la aplicación.
- `MERCADOPAGO_OAUTH_AUTHORIZATION_URL=https://auth.mercadopago.com.pe/authorization`
- `MERCADOPAGO_OAUTH_TOKEN_URL=https://api.mercadopago.com/oauth/token`
- `MERCADOPAGO_OAUTH_CALLBACK_URL`: URL pública exacta del backend terminada en `/api/integraciones/mercado-pago/oauth/callback`; registrarla en la aplicación de Mercado Pago.
- `MERCADOPAGO_OAUTH_RETORNO_PANEL`: URL fija del panel que recibe `resultado=exito`, `resultado=error` o `resultado=conflicto` (cuenta externa ya vinculada).
- `MERCADOPAGO_OAUTH_EXPIRY_MINUTES=10` y `MERCADOPAGO_OAUTH_ACTIVE_KEY=k1`.
- `MERCADOPAGO_OAUTH_KEYS_K1`: clave aleatoria de 32 bytes codificada en Base64, gestionada como secreto externo.

Todas las URL deben usar HTTPS. Para pruebas locales únicamente, el perfil `local` permite `MERCADOPAGO_OAUTH_ALLOW_INSECURE_LOCALHOST=true` para HTTP en localhost. Para rotar claves, conservar las claves antiguas en la configuración, añadir la nueva y cambiar el identificador activo; no retirar una clave antigua mientras existan sobres cifrados con ella. Los sobres de credenciales contienen access token, refresh token y vencimiento absoluto, protegidos por AES-GCM y asociados al ID de tienda. El verificador PKCE se cifra con contexto del intento.

`POST /api/tiendas/{idTienda}/cuenta-pago/mercado-pago/oauth/inicios` requiere al propietario y devuelve una URL de autorización. El callback público consume el estado antes del canje y redirige al panel. La aplicación y los proxies deben excluir los query strings del callback y el cuerpo de la respuesta de inicio de los registros de acceso: contienen datos de un solo uso. Nunca registrar secretos ni cuerpos del canje.

Para una comprobación manual, registrar el callback exacto en una aplicación de Mercado Pago, configurar sus credenciales y dos usuarios de prueba, iniciar la afiliación como propietario y autorizarla en el navegador. Verificar el retorno y consultar `GET /api/tiendas/{idTienda}/cuenta-pago`; repetir cancelando y comprobando que no se activa. La prueba automatizada de PostgreSQL requiere `OAUTH_PG_TEST=true` y `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`; crea y elimina un esquema aislado.

## Desconexión

`POST /api/tiendas/{idTienda}/cuenta-pago/desconexion` requiere autenticación y titularidad. Devuelve 204 también si la cuenta ya estaba desconectada y 404 `CUENTA_PAGO_NO_CONFIGURADA` si no existe. En bases existentes debe aplicarse previamente `services/database/migrations/002_cuenta_pago_historial.sql`, después de la migración OAuth.

La operación invalida los intentos OAuth pendientes o consumidos, elimina la referencia cifrada y libera el identificador externo; la transición queda registrada sin credenciales en `cuenta_pago_historial`. Los nuevos cobros solo deben usar cuentas `ACTIVA`. No cancela pagos existentes. La desconexión es local y no revoca automáticamente la autorización de Mercado Pago: el vendedor puede revocarla en su cuenta del proveedor. Los respaldos conservan datos según su política de retención.
