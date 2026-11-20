# Plataforma y vitrinas en un dominio

El proyecto de Vercel utiliza la raíz del repositorio. Usar Node 22 y conservar los lockfiles del servidor raíz y de ambas aplicaciones. `npm ci` instala pnpm 10.28.2 como dependencia de desarrollo; el script raíz utiliza explícitamente esa versión para compilar ambas aplicaciones.

## Desarrollo local

1. Iniciar el backend y sus servicios siguiendo `render-backend.md` o la configuración local existente.
2. Configurar `VITE_API_URL` en `apps/cms/.env` y `apps/store/.env`, o exportarla antes de compilar. Debe apuntar al origen del backend; por ejemplo, `http://localhost:8080`.
3. En la raíz, ejecutar `npm ci`, `npm run build` y `npm start`. Abrir `http://localhost:3000/` y `/tienda/<slug>`.
4. Configurar el backend con `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000`, `SESSION_COOKIE_SECURE=false` y `SESSION_COOKIE_SAME_SITE=lax` para HTTP local. El navegador de desarrollo debe usar el mismo nombre de host para ambos servicios.

## Vercel y backend

Crear un proyecto Vercel con la raíz del repositorio como Root Directory y preset `Other`; no importar `cms` o `store` por separado para este despliegue conjunto. El `vercel.json` configura `pnpm run build`; la salida está en `public`. No seleccionar el preset `Services` sin implementar su configuración propia. La compilación raíz instala cada frontend con `pnpm install --frozen-lockfile` y publica sus archivos en `public/plataforma` y `public/tienda`.

En el entorno observado, configurar `VITE_API_URL=https://nubea.onrender.com` en Vercel. `VITE_MERCADOPAGO_OAUTH_ENABLED=true` muestra la conexión OAuth del CMS cuando el backend también está configurado; con `false` la oculta. Esta opción no habilita cobros. Las variables `VITE_*` son públicas y sus cambios requieren recompilar. Comprobar que los lockfiles de ambas aplicaciones estén incluidos en la revisión remota: si faltan o se usa una versión incompatible de pnpm falla la instalación congelada.

Configurar `APP_CORS_ALLOWED_ORIGINS=https://nubea.vercel.app` en el backend, sin ruta ni barra final. Incluir también los orígenes de vista previa autorizados cuando se necesiten. Si frontend y backend son sitios distintos, configurar `SESSION_COOKIE_SECURE=true` y `SESSION_COOKIE_SAME_SITE=none`; tanto la cookie de sesión como la CSRF usan esos ajustes. El cliente envía credenciales y obtiene el token CSRF del backend. Algunos navegadores bloquean cookies de terceros incluso con esta configuración; comprobar registro, sesión y cierre en los navegadores objetivo y considerar dominios que compartan sitio si fuera necesario.

Una vez desplegado, comprobar la raíz, recargas de ambos frontends, archivos inexistentes, una tienda activa y los flujos de sesión desde el dominio real. Actualizar el origen permitido en el backend antes de esas pruebas.

Para configurar la vinculación OAuth seguir [la guía de Mercado Pago](../technical/mercadopago-oauth.md). Se observó un retorno exitoso en el panel, pero los pagos y webhooks aún no están implementados. No almacenar `client_secret`, claves de cifrado o tokens en Vercel ni en variables `VITE_*`.
