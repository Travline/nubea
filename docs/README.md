# Documentación de Nubea

Índice de documentación funcional y técnica. Los documentos describen tanto la intención de negocio como el código observado; para decisiones de integración, comprobar también las clases y el esquema referenciados.

## Funcional

- [Historias del módulo 3](functional/uh-m3.md): apariencia, banners, inventario y cuenta de pago.
- [Reglas de negocio](functional/modules-rules.md): reglas de los módulos 1 a 4.

## Integración y operación del módulo 3

- [Integración con M1, M2, M4 y frontend](integracion/modulo-3.md): contratos, adaptadores temporales y responsabilidades pendientes.
- [Flujos y puntos de extensión](technical/modulo-3-flujos.md): historias, endpoints, transacciones y brechas conocidas.
- [Preparación para producción](technical/modulo-3-produccion.md): configuración, migraciones, despliegue, seguridad y recuperación.
- [Inventario](technical/inventario-consulta.md), [consulta de cuenta](technical/cuenta-pago-consulta.md) y [OAuth y desconexión](technical/mercadopago-oauth.md): contratos específicos.
- [Visión general del backend](technical/backend-overview.md): panorama arquitectónico anterior. Contrastar sus detalles con las guías anteriores y el código actual: contiene descripciones de diseño que requieren actualización, especialmente sobre autenticación y banners.

- [Módulo 4: estado actual](technical/modulo-4.md): carrito, direcciones, pedidos existentes, verificaciones y trabajo pendiente de checkout, Orders y Google Places.
- [Migraciones de la base](integracion/base-de-datos.md): Flyway y versiones actuales. La [guía histórica del modelo](technical/database.md) usa nombres anteriores y no es el esquema vigente.
- [Dominio único](despliegue/dominio-unico.md) y [backend Render](despliegue/render-backend.md): configuración y límites del despliegue observado.
