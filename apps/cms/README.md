# CMS de Nubea

Aplicación React/Vite para acceder al espacio de Nubea. Usa una sesión del backend y solicita CSRF para cada operación de escritura. El dashboard muestra una bienvenida distinta para `ADMIN_GENERAL`; la gestión de tiendas se implementará en otra etapa.

El CMS y `apps/store` son aplicaciones independientes: cada una mantiene sus propios tipos y dependencias. No se usarán paquetes de tipos compartidos, Turborepo ni pnpm workspaces; cada frontend se instala, compila y despliega por separado.

El código propio usa nombres y comentarios en español, en consonancia con el backend. Se mantienen los nombres exigidos por React, shadcn, las bibliotecas y los contratos HTTP; los hooks conservan el prefijo `use` que requiere React.

## Desarrollo local

Instala las dependencias con `pnpm install`. Inicia el backend en `http://localhost:8080` y ejecuta `pnpm dev`. Para otro servidor, usa `BACKEND_URL=https://tu-backend.onrender.com pnpm dev`. Vite reenvía `/api` y `/logout`, de modo que el navegador y las cookies usan el mismo origen. Para un backend local servido por HTTP, configura `SESSION_COOKIE_SECURE=false` en ese backend.

## Vercel y Render

Configura `apps/cms` como directorio raíz del proyecto en Vercel y define en Vercel la variable de servidor `BACKEND_URL` con el origen HTTPS de Render, por ejemplo `https://tu-backend.onrender.com` (sin ruta ni barra final). No lleva el prefijo `VITE_`. El proxy reenvía `/api` y `/logout`; las demás rutas abren la aplicación. Despliega también el backend actualizado antes de probar la sesión. Mantén `SESSION_COOKIE_SECURE=true` en Render.

Comprueba en el navegador registro, inicio de sesión, recarga del dashboard y cierre de sesión. El registro otorga `CLIENTE`, no acceso administrativo. `APP_BOOTSTRAP_ADMIN_EMAIL` se evalúa cuando arranca el backend y requiere que la cuenta activa ya exista; la inicialización solo actúa cuando aún no hay otro administrador. La sesión actual se guarda en memoria y se pierde al reiniciar la instancia.
