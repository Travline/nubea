# Vitrina de Nubea

Aplicación React y Vite para consultar tiendas y productos públicos, gestionar un carrito y direcciones con una cuenta de Nubea y consultar pedidos existentes. Se publica bajo `/tienda/<slug>`; las tiendas y productos deben estar activos y publicados para aparecer.

El carrito muestra precios actuales, disponibilidad y direcciones guardadas. La elección de dirección es temporal y no reserva stock. **El checkout y los cobros todavía no están disponibles**; Google Places tampoco está integrado. Consulta el [estado del módulo 4](../../docs/technical/modulo-4.md).

## Desarrollo

En este directorio ejecuta `pnpm install --frozen-lockfile` y `pnpm dev` con pnpm 10. Para una API distinta de `http://localhost:8080`, configura `VITE_API_URL` con el origen, sin `/api` ni barra final, y reinicia Vite. El backend debe permitir el origen exacto del frontend por CORS y configurar las cookies para el entorno; las solicitudes autenticadas usan sesión y CSRF. Ejecuta `pnpm build` y `pnpm lint` para verificar la aplicación.

Para compilar CMS y vitrina juntos desde la raíz del repositorio y publicarlos en un único dominio, sigue la [guía de despliegue](../../docs/despliegue/dominio-unico.md).
