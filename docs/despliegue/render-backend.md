# Despliegue del backend en Render con Supabase

## Estado y alcance

**El backend todavía no está listo para producción.** Esta guía prepara un despliegue de prueba y enumera lo necesario antes de recibir usuarios reales. No se ha hecho un despliegue en Render ni se ha probado la conexión con el proyecto real de Supabase o Mercado Pago.

El repositorio incluye `backend/Dockerfile`, el plugin de empaquetado ejecutable y un endpoint `/health`. El repositorio contiene cambios locales sin commit. Render despliega la revisión presente en la rama remota, no los cambios locales. La etapa de imágenes se probó localmente con PostgreSQL y RustFS, pero requiere publicar esos cambios en la rama elegida antes de desplegarlos. No configurar Google OAuth en esta etapa.

Bloqueos y riesgos antes de producción:

- Completar y probar el acceso con cookies, CSRF y CORS desde el frontend y sus dominios reales. La sesión actual vive en memoria: se pierde al reiniciar y no se comparte entre instancias. Para varias instancias, implementar sesiones compartidas; con una sola, asumir la interrupción de sesiones durante los despliegues y reinicios.
- Asegurar el aprovisionamiento del primer administrador. Actualmente el registro es público, no verifica la titularidad del correo y la variable `APP_BOOTSTRAP_ADMIN_EMAIL` otorga el rol a la cuenta activa con ese correo cuando no existe otro administrador. **No confiar en este mecanismo en una aplicación abierta al público**: implementar una inicialización fuera de banda o impedir el acceso público hasta comprobar la identidad. Retirar la variable tras utilizarla.
- Provisionar almacenamiento S3 compatible y CDN, y verificar subida, lectura y limpieza desde Render. Las URLs son públicas incluso para imágenes de borradores. La validación actual comprueba tamaño y cabeceras, pero no decodifica el archivo completo.
- La vinculación con Mercado Pago está implementada, pero no se ha validado con una aplicación y cuentas reales. La renovación de credenciales y la revocación remota siguen pendientes; las operaciones de cobro son de M4. Mantener OAuth desactivado hasta completar esas verificaciones.
- La limpieza de imágenes depende del proceso programado del backend mientras esté activo; conservar los datos de PostgreSQL y los archivos del proveedor elegido.

## 1. Preparar el artefacto

El proyecto incluye `backend/Dockerfile`: usa Java 21, empaqueta el JAR ejecutable con el plugin Maven de Spring Boot y lo ejecuta con un usuario sin privilegios. Render construye la imagen a partir de ese archivo. Ejecutar la suite de pruebas por separado antes de publicar; el build de la imagen omite las pruebas.

```bash
docker build -t nubea-backend ./backend
```

Si cambia la versión del artefacto en el `pom.xml`, actualizar el nombre del JAR en el Dockerfile. Render solo podrá construir una vez que los archivos estén en la rama remota seleccionada.

## 2. Configurar Supabase: PostgreSQL y Storage

1. En el proyecto de Supabase, abrir **Connect → Session pooler** y copiar el host, puerto, nombre de base y usuario. Para una aplicación persistente como este backend, Supabase recomienda conexión directa cuando hay conectividad IPv6; si el entorno usa IPv4, usar el pooler en modo sesión (puerto 5432). No usar el modo transacción 6543 sin validar su compatibilidad con Flyway y sesiones JDBC. El usuario y host del pooler deben copiarse exactamente desde Supabase. [Conexiones PostgreSQL de Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres), [Supabase con Spring Boot](https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot).
2. Convertir los datos al formato JDBC, por ejemplo `jdbc:postgresql://HOST_POOLER:5432/postgres?sslmode=require`. Colocar usuario y contraseña por separado en Render; no pegar `postgresql://usuario:contraseña@...` en `SPRING_DATASOURCE_URL`. Usar una base vacía destinada al proyecto, o revisar las migraciones y datos existentes antes del primer arranque. La migración necesita permisos para `pgcrypto`, `btree_gist` y las tablas del esquema configurado.
3. En **Storage**, verificar que el bucket `nubea` sea público para las imágenes. Copiar de la configuración S3 el endpoint, región y credenciales S3. El endpoint de Supabase tiene forma `https://REFERENCIA.storage.supabase.co/storage/v1/s3`; copiar siempre el valor real del panel. La plantilla usa `us-east-1` como ejemplo, pero debe sustituirse si el panel indica otra región. Las credenciales S3 son solo para el backend y conceden acceso amplio al almacenamiento; no confundirlas con la contraseña de PostgreSQL ni exponerlas al navegador. [Autenticación S3 de Supabase](https://supabase.com/docs/guides/storage/s3/authentication).
4. La URL base pública de Supabase tiene forma `https://REFERENCIA.supabase.co/storage/v1/object/public/nubea`, sin barra ni clave al final. Todo archivo en un bucket público es accesible a quien conozca su URL, incluidos borradores. Probar subida, lectura, sustitución y eliminación con el proyecto real antes de presentar el sistema. [URLs públicas de Supabase](https://supabase.com/docs/guides/storage/serving/downloads).
5. Las tablas creadas por Flyway se alojan en el esquema `public` por defecto. Revisar la exposición mediante Data API de Supabase: las rutas directas no deben permitir a `anon` ni `authenticated` saltarse la seguridad del backend. Deshabilitar la exposición de ese esquema por Data API o configurar permisos y RLS explícitos; verificarlo con clientes no autenticados antes de usar datos reales.

RustFS sigue disponible para desarrollo local; no es el almacenamiento de Render. La compatibilidad S3 real de Supabase con el SDK debe comprobarse con credenciales de prueba, ya que no se ha probado con el proyecto del usuario.

## 3. Crear el Web Service

En Render, seleccionar **New → Web Service**, conectar el repositorio y escoger la rama remota que contenga la versión revisada. Configurar:

| Campo | Valor |
| --- | --- |
| Runtime | Docker |
| Root Directory | `backend` |
| Dockerfile Path | `Dockerfile`, relativo al Root Directory |
| Región | Preferiblemente cercana a la región del proyecto Supabase |
| Instancias | Una mientras las sesiones sean locales a memoria |
| Health check | `/health`; responde 200 si la aplicación puede consultar PostgreSQL y 503 si no puede. |

Render recomienda escuchar en `0.0.0.0` y utilizar su variable `PORT`. El backend usa `PORT` cuando está definida y `8080` en local. No fijar `SERVER_ADDRESS` a `127.0.0.1`. [Web Services y puertos](https://render.com/docs/web-services), [health checks](https://render.com/docs/health-checks), [rutas en monorepos](https://render.com/docs/monorepo-support).

## 4. Variables de entorno

Introducir valores reales en la sección **Environment** de Render, sin guardarlos en Git ni copiarlos a logs. Los valores `localhost` y credenciales del `.env.example` son solo para desarrollo. El archivo `.env.example` no se carga automáticamente por Render.

| Variable | Valor / propósito |
| --- | --- |
| `PORT` | Render la establece automáticamente; localmente se usa `8080` si no está definida. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST_POOLER_SUPABASE:5432/postgres?sslmode=require`, con los datos de Session pooler. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL con permisos de migración. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de PostgreSQL. |
| `STORAGE_S3_ENDPOINT` | Endpoint HTTPS del proveedor S3 compatible. |
| `STORAGE_S3_REGION` | Región que exige el proveedor. |
| `STORAGE_S3_BUCKET` | Nombre del bucket. |
| `STORAGE_S3_ACCESS_KEY` / `STORAGE_S3_SECRET_KEY` | Credenciales de acceso con permisos mínimos para subir y eliminar objetos. |
| `STORAGE_PUBLIC_BASE_URL` | Base HTTPS pública del CDN o bucket, sin clave de objeto. |
| `APP_CORS_ALLOWED_ORIGINS` | `https://nubea-clients.vercel.app`; añadir más adelante el origen exacto del frontend de tiendas, separado por coma. No usar `*`. |
| `SESSION_COOKIE_SECURE` | `true` en HTTPS. |
| `SESSION_COOKIE_SAME_SITE` | `none` para el frontend en Vercel y una API en `onrender.com`, con HTTPS y `Secure`. También se aplica a la cookie CSRF. Algunos navegadores bloquean cookies de terceros aun con `none`; probar y considerar dominios propios bajo el mismo sitio. |
| `MERCADOPAGO_OAUTH_ENABLED` | `false` hasta validar Mercado Pago con credenciales y URLs reales. |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Dejar vacío hasta disponer de un procedimiento de inicialización seguro; no es una contraseña ni verifica la propiedad del correo. |

Los formatos completos están en [`.env.render.example`](../../.env.render.example). Sustituir los marcadores con datos del panel, sin compartir contraseñas ni claves en chats o commits. El bucket y las credenciales aún no han sido verificados contra Supabase.

Si más adelante se habilita Mercado Pago, configurar además las variables `MERCADOPAGO_OAUTH_*` descritas en [la guía de OAuth](../technical/mercadopago-oauth.md), incluyendo claves de cifrado persistentes, URL exacta del callback y retorno fijo del panel. No exponer secretos ni registrar las consultas del callback. Google OAuth sigue fuera de alcance.

## Primer administrador para la prueba

El registro público de Swagger crea un usuario con rol `CLIENTE`, no administrador. Para una prueba controlada se puede registrar la cuenta propia mediante `POST /api/auth/registro`, obtener su `idUsuario` de la respuesta y confirmar su identidad antes de promoverla. Swagger también debe obtener un token desde `GET /api/auth/csrf` y enviarlo en la cabecera indicada para las operaciones POST, PUT y DELETE.

Después, una persona con acceso administrativo al SQL Editor de Supabase puede sustituir `UUID_DEL_USUARIO_VERIFICADO` por ese UUID y ejecutar esta asignación en la base correcta:

```sql
INSERT INTO usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM usuario_plataforma u CROSS JOIN rol r
WHERE u.id_usuario = 'UUID_DEL_USUARIO_VERIFICADO'::uuid
  AND u.estado = 'ACTIVO' AND r.nombre = 'ADMIN_GENERAL'
ON CONFLICT DO NOTHING;
```

Comprobar el usuario y el resultado antes de continuar; cero filas significa que no se ha asignado un rol nuevo. No publicar la contraseña ni los permisos de base de datos. Una vez iniciada sesión como administrador, `PUT /api/usuarios/{idUsuario}/roles/{rol}` ya exige `ADMIN_GENERAL`; actualmente permite asignar también ese mismo rol. Restringirlo a otros roles es un cambio separado si se desea. No hace falta configurar `APP_BOOTSTRAP_ADMIN_EMAIL` para este procedimiento.

## 5. Desplegar y comprobar

1. Ejecutar las pruebas de backend con Java 21, PostgreSQL y RustFS aislados; las pruebas de OAuth y almacenamiento requieren sus indicadores de entorno. No activar pruebas que crean o eliminan esquemas contra la base de producción.
2. Publicar el código revisado y el Dockerfile probado en la rama remota conectada a Render. Crear el servicio con las variables ya configuradas. Revisar que el build genere el JAR ejecutable y que el proceso arranque en el puerto esperado.
3. Sobre una base **vacía**, Flyway ejecuta sus migraciones al arrancar y Hibernate valida el esquema. Comprobar en los logs y en `flyway_schema_history` que se aplicaron las versiones esperadas. No ejecutar además los scripts SQL manuales antiguos. Para bases existentes, detenerse y evaluar su estado: esta guía no migra datos ni autoriza a borrar la base. [Base de datos y migraciones](../integracion/base-de-datos.md).
4. Con un frontend de prueba y una cuenta controlada, comprobar `GET /api/auth/csrf`, registro, login, petición autenticada, logout y rechazo sin CSRF. El cliente debe usar credenciales en las solicitudes CORS y enviar el token CSRF indicado por la respuesta. Comprobar desde el navegador real la política `SameSite` y obtener un token actualizado cuando corresponda.
5. Con una cuenta de prueba, crear tienda, categoría, producto e inventario; probar permisos entre dos usuarios. Subir, consultar, sustituir y eliminar una imagen, abrir la URL desde fuera de Render y verificar en `storage_tarea` que las tareas se procesan. Los archivos publicados pueden ser accesibles antes de que la tienda se publique.
6. Configurar `/health` como comprobación HTTP. La ruta comprueba acceso a PostgreSQL; no prueba el almacenamiento ni Mercado Pago.

## 6. Alcance de la prueba universitaria

Comprobar `/health`, el login, la creación de tienda y una imagen desde el navegador. La limpieza automática se ejecuta mientras el backend esté activo. Conservar las credenciales fuera del repositorio y, antes de reutilizar una base con datos, comprobar que no se ejecutarán instrucciones destructivas.

Render indica que las instancias gratuitas pueden detenerse por inactividad y que la base gratuita tiene limitaciones, incluida la falta de respaldos. Consultar las condiciones vigentes antes de elegir un plan. [Limitaciones de Render Free](https://render.com/docs/free).
