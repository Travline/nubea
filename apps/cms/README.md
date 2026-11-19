# CMS de Nubea

Aplicación React/Vite para acceder al espacio de Nubea. Usa una sesión del backend y solicita CSRF para cada operación de escritura. El dashboard muestra una bienvenida distinta para `ADMIN_GENERAL`; la gestión de tiendas se implementará en otra etapa.

El CMS y `apps/store` son aplicaciones independientes: cada una mantiene sus propios tipos y dependencias. No se usarán paquetes de tipos compartidos, Turborepo ni pnpm workspaces; cada frontend se instala, compila y despliega por separado.

El código propio usa nombres y comentarios en español, en consonancia con el backend. Se mantienen los nombres exigidos por React, shadcn, las bibliotecas y los contratos HTTP; los hooks conservan el prefijo `use` que requiere React.

## Desarrollo local

Instala las dependencias con `pnpm install`. Inicia el backend en `http://localhost:8080` y ejecuta `pnpm dev`. El CMS usa esa URL por defecto en desarrollo; para otra API define `VITE_API_URL`, por ejemplo en un archivo `.env.local`, y reinicia Vite. El navegador llama directamente al backend. Para HTTP local configura en el backend `SESSION_COOKIE_SECURE=false` y `SESSION_COOKIE_SAME_SITE=lax`, y añade el origen exacto de Vite, normalmente `http://localhost:5173`, a `APP_CORS_ALLOWED_ORIGINS`. Usa el mismo host (`localhost`) en ambas URL.

## Vercel y Render

Configura `apps/cms` como directorio raíz del proyecto en Vercel y define `VITE_API_URL` con el origen público HTTPS de Render, por ejemplo `https://tu-backend.onrender.com` (sin ruta ni barra final). Vite incorpora esta URL pública durante la compilación: vuelve a desplegar el CMS cuando cambie. En Render configura `APP_CORS_ALLOWED_ORIGINS` con el origen exacto de Vercel, `SESSION_COOKIE_SECURE=true` y `SESSION_COOKIE_SAME_SITE=none`. Estas dos últimas opciones también configuran la cookie CSRF. Añade otros orígenes explícitos separados por comas según sea necesario.

Comprueba en el navegador registro, inicio de sesión, recarga del panel y cierre de sesión. El CMS debe poder recibir cookies de Render; algunas políticas de navegador bloquean las cookies entre sitios. El registro otorga `CLIENTE`, no acceso administrativo. `APP_BOOTSTRAP_ADMIN_EMAIL` se evalúa cuando arranca el backend y requiere que la cuenta activa ya exista; la inicialización solo actúa cuando aún no hay otro administrador. La sesión actual se guarda en memoria y se pierde al reiniciar la instancia.
