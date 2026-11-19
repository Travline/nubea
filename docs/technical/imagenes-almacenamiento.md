# Imágenes de productos y banners

La API recibe los archivos directamente y genera las claves de almacenamiento. Configurar `STORAGE_S3_ENDPOINT`, `STORAGE_S3_REGION`, `STORAGE_S3_BUCKET`, `STORAGE_S3_ACCESS_KEY` y `STORAGE_S3_SECRET_KEY`; `STORAGE_PUBLIC_BASE_URL` es la base HTTP(S) pública desde la que se obtienen los objetos. La URL puede ser accedida por cualquiera que la conozca, incluso si el recurso todavía es un borrador. No guardar información privada en estos archivos.

Para desarrollo, iniciar `docker compose -f services/storage/docker-compose.yaml up -d`. El contenedor crea el bucket `nubea` y permite lectura pública únicamente de los prefijos `banners/` y `productos/`. La URL base local es `http://localhost:9000/nubea`. En producción, configurar un dominio HTTPS y las reglas de lectura del CDN/bucket por separado; no usar las credenciales locales.

## API

- `POST /api/tiendas/{tienda}/productos/{producto}/imagenes`: multipart con `archivo`, `orden` y `esPrincipal` opcional. Devuelve `url` y metadatos. El contrato anterior que aceptaba `storageKey` ha sido retirado.
- `PUT /api/tiendas/{tienda}/productos/{producto}/imagenes/{imagen}`: JSON con `orden`; `PATCH .../{imagen}/principal` selecciona la principal.
- `PUT /api/tiendas/{tienda}/productos/{producto}/imagenes/{imagen}/archivo`: multipart con `archivo`, sustituye el binario. `DELETE .../{imagen}` elimina la referencia y encola la limpieza.
- Los endpoints de banners conservan sus operaciones multipart y devuelven `url` en lugar de `storageKey`. Las mutaciones requieren propietario o los permisos previstos por cada controlador.

Los archivos admitidos y el límite se configuran con `banner.allowed-mime-types` y `banner.max-file-size`; también deben coordinarse los límites multipart y del proxy. El validador comprueba la cabecera del archivo y no confía solo en el MIME declarado.

## Recuperación

Flyway crea `storage_tarea`. Antes de subir se registra la clave con un plazo de 15 minutos, mayor al tiempo máximo de llamada S3 de 2 minutos. La aplicación guarda la referencia y confirma la subida en una transacción; si falla, programa la limpieza. Al sustituir o eliminar, la referencia antigua y la tarea de borrado se actualizan en la misma transacción. El procesador programado reclama tareas pendientes, comprueba que ninguna tabla de imágenes o banners siga referenciando la clave y reintenta los errores después de cinco minutos. Las subidas abandonadas se tratan como objetos huérfanos al vencer el plazo.

Los reintentos no borran objetos sin registro de tarea; por ello las claves antiguas anteriores a esta implementación no se barren automáticamente. Revisar `storage_tarea`, su campo `intentos` y los logs de almacenamiento para diagnosticar fallos. No modificar manualmente las tareas de cargas en curso. La política de acceso público y las pruebas con un CDN real son responsabilidades de la configuración de despliegue.
