# CMS de Nubea

Aplicación React/Vite para que cada comercio administre sus tiendas. El registro inicia sesión automáticamente; si el acceso falla, la cuenta queda creada y se puede iniciar sesión manualmente. El panel permite crear varias tiendas y entrar únicamente a las propias. Desde el resumen, el propietario puede activar una tienda en borrador sin exigir productos ni una cuenta de pago.

## Funciones

- Catálogo: categorías, productos, variantes y valores, presentaciones, imágenes, colecciones y asociaciones. Se crea primero una categoría, después un producto y al menos una presentación con SKU. El inventario de la presentación se ajusta desde Inventario. Las operaciones se guardan por separado: si una falla, se puede volver al producto y continuar.
- Inventario: búsqueda, filtros por estado y alerta, paginación y ajuste de stock actual y mínimo. Una presentación sin registro de inventario se muestra como incidencia y no se puede ajustar desde el CMS.
- Apariencia: selección de plantilla activa, contenido de inicio, carrusel y banners con carga, reemplazo, orden, enlace y estado. Si la plantilla asignada se desactiva, se conserva y se puede seguir editando la configuración; solo las plantillas activas se pueden elegir. La plantilla configura la futura vitrina; el CMS no la publica.
- Pagos: consulta y desconexión de la cuenta de Mercado Pago. La vinculación OAuth está desactivada por defecto.

El código propio usa nombres y textos en español; se conservan los nombres exigidos por React, las bibliotecas y los contratos HTTP. El CMS y `apps/store` son aplicaciones independientes y mantienen sus tipos y dependencias por separado.

## Desarrollo local

Instala las dependencias con `pnpm install` y ejecuta `pnpm dev`. El CMS usa `http://localhost:8080` por defecto en desarrollo; para otra API define `VITE_API_URL` con el origen, sin ruta ni barra final, y reinicia Vite. Hay un ejemplo en `.env.example`.

Inicia el backend y sus servicios de PostgreSQL y almacenamiento. Para HTTP local configura `SESSION_COOKIE_SECURE=false`, `SESSION_COOKIE_SAME_SITE=lax` y añade el origen exacto de Vite (normalmente `http://localhost:5173`) a `APP_CORS_ALLOWED_ORIGINS`. Usa el mismo host (`localhost`) en ambas URL. Las imágenes requieren almacenamiento configurado y accesible.

El cliente envía cookies y obtiene un token CSRF antes de cada escritura, incluidos archivos. Una sesión vencida devuelve al inicio de sesión conservando la ruta de trabajo. Las consultas y operaciones de catálogo aplican los permisos por tienda y las validaciones del backend; la creación de una tienda asigna el rol de dueño y el CMS actualiza la sesión.

## Mercado Pago

Mantén `MERCADOPAGO_OAUTH_ENABLED=false` en el backend y `VITE_MERCADOPAGO_OAUTH_ENABLED=false` en el CMS hasta disponer de credenciales y URLs válidas. El CMS informa que la vinculación no está disponible, pero sigue mostrando el estado y permite desconectar una cuenta activa.

Para habilitarla, configura el backend según [la guía OAuth](../../docs/technical/mercadopago-oauth.md), establece `MERCADOPAGO_OAUTH_RETORNO_PANEL` a la URL pública del CMS terminada en `/panel/retorno-pago` y activa ambas variables. Vite incorpora su variable durante la compilación: recompila y despliega el CMS tras cambiarla. El retorno usa la tienda guardada en la sesión del navegador y consulta el estado real en el backend; debe completarse en la misma pestaña. No se guardan credenciales del proveedor en el frontend.

## Despliegue y comprobaciones

En Vercel configura `apps/cms` como directorio raíz y `VITE_API_URL` con el origen HTTPS del backend. En el backend configura el origen exacto de Vercel en `APP_CORS_ALLOWED_ORIGINS`, `SESSION_COOKIE_SECURE=true` y `SESSION_COOKIE_SAME_SITE=none`. Algunas políticas del navegador pueden bloquear cookies entre sitios. La sesión actual del backend se guarda en memoria y se pierde al reiniciar la instancia.

Ejecuta `pnpm build` y `pnpm lint`. Con el backend y almacenamiento activos, verifica registro, recarga y cierre de sesión; crea dos tiendas y cambia entre ellas; crea categoría, producto y presentación, ajusta stock, prueba filtros y publicación; carga imágenes, colecciones y banners; cambia la configuración visual. Comprueba validaciones, acceso a una tienda ajena y vencimiento de sesión. La prueba completa de OAuth requiere habilitar la integración y usar cuentas de prueba de Mercado Pago.

## Pendiente

La vitrina pública, el carrito y el checkout quedan fuera de esta etapa. Las tiendas nuevas nacen en BORRADOR; para publicar productos deben activarse y cada producto debe cumplir los requisitos del backend. La activación no abre todavía una vitrina pública. Las presentaciones muestran su combinación actual de variantes y permiten modificarla desde el producto. La vitrina pública se desarrollará más adelante en `apps/store`. El checkout depende de capacidades pendientes del backend.

En la revisión del panel se ejecutaron pruebas de integración de sesión, permisos entre tiendas, catálogo, inventario, apariencia, imágenes y banners contra PostgreSQL y almacenamiento temporales. La comprobación visual en navegador y el flujo OAuth completo requieren una verificación adicional en un entorno configurado.

## Administración general

La cuenta con `ADMIN_GENERAL` accede desde `/panel` a `/panel/administracion`, con secciones de usuarios, tiendas y plantillas. Se puede buscar y paginar usuarios y tiendas; las tiendas se consultan sin modificar su contenido. En usuarios se pueden asignar roles o retirar `DUENO_TIENDA`: esta última acción suspende sus tiendas activas y en borrador. El panel no ofrece cambiar los roles de la cuenta administradora actual ni crea una tienda para ella.

En plantillas se administran su código, nombre, URL de vista previa y estado. El código es inmutable y representa una plantilla que deberá existir en la futura vitrina; agregar metadatos no implementa un diseño. Desactivar la plantilla predeterminada impide crear tiendas hasta volver a activarla.

Las consultas `/api/admin/usuarios` y `/api/admin/tiendas` requieren `ADMIN_GENERAL` y admiten `texto`, `page` (desde 0) y `size` (1 a 100). Las acciones de modificación mantienen sesión y CSRF. La asignación del primer administrador se gestiona fuera del panel; este no modifica roles existentes automáticamente.
