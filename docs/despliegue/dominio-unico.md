# Plataforma y vitrinas en un dominio

El proyecto de Vercel utiliza la raíz del repositorio. Node 20.19 o superior y pnpm son necesarios para construir los dos proyectos Vite con sus propios lockfiles. El servidor raíz tiene su propio lockfile de npm.

## Desarrollo local

1. Iniciar el backend y sus servicios siguiendo `render-backend.md` o la configuración local existente.
2. Configurar `VITE_API_URL` en `apps/cms/.env` y `apps/store/.env`, o exportarla antes de compilar. Debe apuntar al origen del backend; por ejemplo, `http://localhost:8080`.
3. En la raíz, ejecutar `npm ci`, `npm run build` y `npm start`. Abrir `http://localhost:3000/` y `/tienda/<slug>`.
4. Configurar el backend con `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000`, `SESSION_COOKIE_SECURE=false` y `SESSION_COOKIE_SAME_SITE=lax` para HTTP local. El navegador de desarrollo debe usar el mismo nombre de host para ambos servicios.

## Vercel y backend

Crear un proyecto Vercel con la raíz del repositorio como Root Directory. Configurar `VITE_API_URL` con el origen HTTPS real del backend y mantener `VITE_MERCADOPAGO_OAUTH_ENABLED=false`. La compilación raíz instala cada frontend con `pnpm install --frozen-lockfile` y publica sus archivos en `public/plataforma` y `public/tienda`. Vercel sirve esos archivos desde su CDN; Express los sirve solamente en local. Ver [Express en Vercel](https://vercel.com/docs/frameworks/backend/express).

Configurar `APP_CORS_ALLOWED_ORIGINS` en el backend con el origen exacto de Vercel, sin ruta ni barra final. Incluir también los orígenes de vista previa autorizados cuando se necesiten. Si frontend y backend son sitios distintos, configurar `SESSION_COOKIE_SECURE=true` y `SESSION_COOKIE_SAME_SITE=none`; tanto la cookie de sesión como la CSRF usan esos ajustes. El cliente envía credenciales y obtiene el token CSRF del backend. Algunos navegadores bloquean cookies de terceros incluso con esta configuración; comprobar registro, sesión y cierre en los navegadores objetivo y considerar dominios que compartan sitio si fuera necesario.

Una vez desplegado, comprobar la raíz, recargas de ambos frontends, archivos inexistentes, una tienda activa y los flujos de sesión desde el dominio real. Actualizar el origen permitido en el backend antes de esas pruebas.

OAuth de Mercado Pago permanece desactivado. Para habilitarlo, seguir `../technical/mercadopago-oauth.md`, configurar `MERCADOPAGO_OAUTH_RETORNO_PANEL=https://<dominio>/plataforma/panel/retorno-pago` y activar también `VITE_MERCADOPAGO_OAUTH_ENABLED` antes de recompilar el CMS. El callback registrado en Mercado Pago sigue apuntando al backend.
