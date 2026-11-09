# Módulo 3: preparación para producción

Esta es una hoja de ruta, no una afirmación de que el sistema esté listo para desplegar. Las historias describen intención; consultar [flujos y brechas](modulo-3-flujos.md) e [integración M1/M2/M4](../integracion/modulo-3.md) antes de integrar.

## Requisitos previos

1. Completar M1: sesión autenticada real, authorities y principal compatibles, verificación de usuario activo, CSRF/CORS según mecanismo y alineación entre modelo de usuario y SQL. Probar accesos propietario/ajeno, administrador sin titularidad, anónimo, cuentas suspendidas y transferencia de tienda.
2. Integrar creación transaccional de tienda, configuración y cuenta; definir plantilla activa inicial. Integrar M2 para inventario inicial y M4 para reserva y uso seguro de cuenta ACTIVA; probar condiciones de carrera y rollback.
3. Revisar el flujo de banners: almacenamiento externo fuera de transacciones, compensación ante fallos de commit, cola durable/outbox para limpieza y reconciliación de huérfanos. Definir acceso público de imágenes y tratamiento de `storageKey` antes de exponer el contrato.
4. Para pagos, verificar con Mercado Pago y cuentas de prueba reales la aplicación, callback exacto, PKCE, vencimiento y refresh. Implementar gestión de expiración y credenciales, desautorización externa, monitoreo y política para pagos en curso. El código actual no implementa refresh ni revocación remota. Acordar con seguridad/operaciones protección de secretos y retención de respaldos.

## Configuración por ambiente

- PostgreSQL: suministrar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` desde configuración segura; no hay URL en `application.properties`. El Compose de desarrollo usa PostgreSQL 18 y monta `pgdata` en `/var/lib/postgresql`; credenciales y puerto allí son solo locales. No cambiar una imagen de versión mayor sobre datos existentes sin respaldar y ejecutar un procedimiento de actualización compatible.
- S3 compatible: `STORAGE_S3_ENDPOINT`, `STORAGE_S3_REGION`, `STORAGE_S3_ACCESS_KEY`, `STORAGE_S3_SECRET_KEY`, `STORAGE_S3_BUCKET`. Las propiedades y defaults actuales apuntan a RustFS local y no son credenciales de producción. Aprovisionar bucket, permisos mínimos, TLS, política de acceso y dominio/CDN en el proveedor escogido; verificar comportamiento S3 compatible.
- Banners: propiedades existentes `banner.max-file-size` y `banner.allowed-mime-types`; el campo Java `storagePrefix` corresponde a `banner.storage-prefix`, mientras el archivo actual usa `banner.storage.prefix`. Verificar el binding efectivo al configurar un prefijo distinto del valor por defecto; coordinar límites multipart de Spring y del proxy con el límite de aplicación. Ajustar dominio, MIME y límites según el producto.
- OAuth está desactivado por defecto. La lista completa de variables y reglas TLS está en [mercadopago-oauth.md](mercadopago-oauth.md). Configurar claves AES-256 versionadas fuera de BD y repositorio, `client_secret` en gestor de secretos, callback público registrado exactamente y retorno fijo al panel. Mantener claves anteriores hasta migrar sobres y respaldos que todavía se necesiten.
- No dejar logs de petición con query string del callback ni cuerpos del inicio o canje; revisar Spring, proxy, balanceador, APM y registros de errores. El log de S3 incluye bucket y clave de objeto; decidir su tratamiento y retención. Aplicar mínimo privilegio a secretos, datos y trazas.

## Base de datos, despliegue y recuperación

`services/database/init/db.sql` está diseñado para una base vacía y se ejecuta por el contenedor solo en inicialización. No se ejecuta al reiniciar un volumen ya inicializado. Para bases existentes, evaluar su estado real y aplicar las migraciones SQL `001_oauth_mercado_pago.sql` y `002_cuenta_pago_historial.sql` en orden solo si aún no fueron aplicadas. Son scripts manuales y no existe en el proyecto un historial automático de migraciones: establecer un mecanismo de versionado, respaldo previo y verificación antes de producción; no ejecutar dos veces a ciegas.

Ensayar restauración completa de PostgreSQL, objetos y claves de cifrado en un entorno aislado; un respaldo sin la clave correspondiente no permite recuperar los sobres. Definir retención y destrucción de backups conforme a política: borrar `referencia_credencial` de una fila no borra copias históricas. Asegurar integridad al restaurar BD y objetos de S3 en momentos compatibles.

Habilitar OAuth por entorno tras configurar secretos, callback y observabilidad. Verificar que el estado y PKCE expiren, que un callback repetido falle y que una falla de proveedor no active una cuenta. Mantener capacidad de deshabilitar nuevos inicios y planificar cómo procesar intentos ya iniciados antes de apagar la funcionalidad; desactivar la bandera también deshabilita el callback actual. Si hay un incidente de clave, conservar la capacidad de descifrar datos necesarios para recuperación y rotar según el procedimiento aprobado.

## Validación y operación

- Ejecutar `cd backend && ./mvnw test` con Java 21 y datasource accesible: la prueba de contexto existente requiere PostgreSQL. Las pruebas condicionales usan `OAUTH_PG_TEST`, `INVENTARIO_PG_TEST` y `CUENTA_PAGO_PG_TEST` con valor `true`; revisar el código antes de usarlas en una BD compartida. La prueba OAuth crea y elimina esquemas aislados, y las otras usan tablas temporales.
- Realizar pruebas con credenciales de proveedor de prueba y dueño real autenticado: inicio, autorización, retorno, estado, rechazo, expiración, repetición, dos inicios concurrentes, desconexión y caída del proveedor. Las pruebas con servidor simulado no verifican configuración real ni permisos de Mercado Pago.
- Definir alertas con datos no sensibles: tasas de errores de proveedor/callback, intentos inválidos y vencidos, objetos huérfanos, fallos de limpieza y cuentas inconsistentes. El servicio OAuth actual captura errores del callback y redirige `error` sin métricas ni diagnóstico específico; se necesita instrumentación segura. Evitar identificar secretos o almacenar payloads externos.
- Verificar migraciones y rollback en una copia antes del despliegue; establecer tiempos de recuperación, responsables, rotación de claves y pruebas periódicas de restauración. La reversión de código no debe eliminar tablas, historial ni credenciales sin evaluar compatibilidad.
