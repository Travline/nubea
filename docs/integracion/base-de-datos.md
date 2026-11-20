# Migraciones de la base de datos

El backend ejecuta Flyway al arrancar y registra las versiones aplicadas en `flyway_schema_history`. Las migraciones están en `backend/src/main/resources/db/migration`; no aplicar adicionalmente los scripts manuales de `services/database/migrations` ni `services/database/init/db.sql` a la misma base.

En el árbol de trabajo actual existen las versiones V1–V7. V6 añade `checkout_idempotencia`; V7 añade `intento_order_mercadopago` y crea un intento para cada registro de idempotencia anterior. V7 y el servicio de checkout asociado aún pueden estar sin publicar: revisar la revisión desplegada y el historial antes de asumir que existen en Render. Estas tablas no habilitan checkout ni pagos por sí solas. No modificar una migración ya aplicada; añadir una versión nueva para cambios posteriores.

Para una instalación nueva, crear una base PostgreSQL vacía y configurar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` en el entorno del backend. El usuario necesita permisos para crear el esquema y las extensiones `pgcrypto` y `btree_gist`. Iniciar el backend una vez: Flyway aplicará las migraciones en orden y Hibernate validará los mapeos. Verificar el arranque y el historial de Flyway antes de habilitar tráfico.

En producción, respaldar y comprobar la instancia antes de cualquier operación. El procedimiento anterior presupone una base vacía; borrar una base existente es una operación separada y no la realiza la aplicación. No usar `baseline` ni modificar el historial para evitar errores de migración sin investigar la causa. Las credenciales y secretos deben entregarse mediante la configuración segura del entorno, no subirse al repositorio.
