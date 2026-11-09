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
