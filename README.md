# NUBEA
La plataforma para hacer llegar tu tienda a todos lados.
Gestiona el contenido a través de un CMS fácil de usar configura toda tu tienda y los productos.
Comparte la vitrina pública de tu tienda. El checkout y los cobros todavía están en desarrollo.

## Levantar el proyecto
Para esto se cuenta con una estructura de monorepo con las apps y servicios que se necesitan para iniciar el desarrollo local.

- /apps contiene las aplicaciones en desarrollo
- /services para los contenedores docker de los servicios que necesitan las apps

Para PostgreSQL y pgAdmin, copia `docker/.env.example` a `docker/.env`, ajusta las credenciales locales y ejecuta `docker compose --env-file docker/.env -f docker/compose.yaml up -d`. El almacenamiento S3 compatible se configura por separado. No subas archivos `.env` con secretos.
## Dominio único

Consulta [el despliegue de plataforma y vitrinas](docs/despliegue/dominio-unico.md) para compilar y servir ambas aplicaciones desde la raíz.

El [estado del módulo 4](docs/technical/modulo-4.md) distingue lo disponible de lo pendiente para checkout, Mercado Pago y Google Places. La [guía de OAuth](docs/technical/mercadopago-oauth.md) explica la conexión de cada vendedor.
