-- =====================================================================
-- NUBEA / MAESTRO V1.2 / USAR SOLO EN BASE VACIA
-- PostgreSQL 16+ (probado en 16; recomendado 17 en Docker).
-- Orden modulo 1 -> 2 -> 3 -> 4. Ejecutar con ON_ERROR_STOP=1.
-- Cambios respecto a v1.1:
--  * producto y presentacion_producto guardan id_tienda (redundancia controlada
--    con FK compuestas) -> SKU unico por tienda garantizado por la BD.
--  * presentacion_producto.firma_valores -> combinacion unica por producto.
--  * presentacion_valor guarda id_variante -> un valor por variante con UNIQUE
--    (corrige el bug que impedia editar un valor con UPDATE y resiste concurrencia).
--  * banner_tienda: id con DEFAULT y orden UNIQUE diferible (reordenar).
--  * pedido: tasa coherente con su vigencia (FK compuesta), maquina de estados
--    y devolucion de stock unica por trigger; pago valida monto y estados de reembolso.
--  * Ajustes v1.1 (carrito origen, creado_por) integrados en las tablas.
-- =====================================================================
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- #####################################################################
-- MODULO 1 | Usuarios, identidades, roles y tiendas
-- #####################################################################

CREATE TABLE tipo_documento (
 id_tipo_documento SMALLINT PRIMARY KEY,
 codigo VARCHAR(20) NOT NULL UNIQUE CHECK (length(btrim(codigo))>0),
 nombre VARCHAR(100) NOT NULL CHECK (length(btrim(nombre))>0)
);
INSERT INTO tipo_documento VALUES
 (1,'DNI','Documento Nacional de Identidad'),
 (2,'CE','Carné de Extranjería'),
 (3,'PASAPORTE','Pasaporte');

CREATE TABLE usuario_plataforma (
 id_usuario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 nombres VARCHAR(100) NOT NULL CHECK (length(btrim(nombres))>0),
 apellido_paterno VARCHAR(80),
 apellido_materno VARCHAR(80),
 email VARCHAR(254) NOT NULL UNIQUE,
 telefono VARCHAR(20),
 id_tipo_documento SMALLINT REFERENCES tipo_documento(id_tipo_documento) ON DELETE RESTRICT,
 numero_documento VARCHAR(30),
 pais_emisor_documento CHAR(2), -- ISO 3166-1 alfa-2; necesario especialmente en pasaportes
 estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
   CHECK (estado IN ('ACTIVO','SUSPENDIDO','CIERRE_PENDIENTE','CERRADO')),
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 fecha_cierre TIMESTAMPTZ,
 CONSTRAINT chk_usuario_email_formato CHECK (
   email=lower(btrim(email)) AND length(email)>3 AND position('@' in email)>1
 ),
 CONSTRAINT chk_usuario_apellidos CHECK (
  (apellido_paterno IS NULL OR length(btrim(apellido_paterno))>0) AND
  (apellido_materno IS NULL OR length(btrim(apellido_materno))>0)
 ),
 CONSTRAINT chk_usuario_telefono CHECK (telefono IS NULL OR length(btrim(telefono))>0),
 CONSTRAINT chk_documento_conjunto CHECK (
  (id_tipo_documento IS NULL AND numero_documento IS NULL AND pais_emisor_documento IS NULL)
  OR (id_tipo_documento IS NOT NULL AND numero_documento IS NOT NULL
      AND length(btrim(numero_documento))>0 AND pais_emisor_documento IS NOT NULL
      AND pais_emisor_documento ~ '^[A-Z]{2}$')
 ),
 CONSTRAINT chk_cierre_usuario CHECK (
  (estado='CERRADO' AND fecha_cierre IS NOT NULL) OR
  (estado<>'CERRADO' AND fecha_cierre IS NULL)
 ),
 CONSTRAINT chk_usuario_fechas CHECK (fecha_cierre IS NULL OR fecha_cierre>=fecha_creacion)
);
-- No repetir el mismo documento + país emisor + tipo entre usuarios.
CREATE UNIQUE INDEX uq_usuario_documento
 ON usuario_plataforma(id_tipo_documento,pais_emisor_documento,upper(btrim(numero_documento)))
 WHERE numero_documento IS NOT NULL;

CREATE TABLE rol (
  id_rol SMALLINT PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE,
  CONSTRAINT chk_rol_nombre CHECK (nombre IN ('ADMIN_GENERAL','DUENO_TIENDA','CLIENTE'))
);
INSERT INTO rol (id_rol,nombre) VALUES
  (1,'ADMIN_GENERAL'),(2,'DUENO_TIENDA'),(3,'CLIENTE');

CREATE TABLE usuario_identidad (
  id_identidad UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
  proveedor VARCHAR(30) NOT NULL,
  issuer VARCHAR(255),
  subject VARCHAR(255),
  password_hash VARCHAR(255),
  fecha_vinculacion TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_usuario_identidad_proveedor UNIQUE (id_usuario,proveedor),
  CONSTRAINT chk_identidad_modalidad CHECK (
    (proveedor='LOCAL' AND issuer IS NULL AND subject IS NULL AND password_hash IS NOT NULL AND length(btrim(password_hash)) > 0)
    OR
    (proveedor='GOOGLE' AND issuer IS NOT NULL AND length(btrim(issuer)) > 0
      AND subject IS NOT NULL AND length(btrim(subject)) > 0 AND password_hash IS NULL)
  ),
  CONSTRAINT uq_usuario_identidad_oidc UNIQUE (issuer,subject)
);

CREATE TABLE usuario_rol (
  id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
  id_rol SMALLINT NOT NULL REFERENCES rol(id_rol) ON DELETE RESTRICT,
  PRIMARY KEY (id_usuario,id_rol)
);

CREATE TABLE tienda (
  id_tienda UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  id_propietario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
  nombre VARCHAR(150) NOT NULL CHECK (length(btrim(nombre)) > 0),
  slug VARCHAR(150) NOT NULL UNIQUE,
  estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
    CHECK (estado IN ('BORRADOR','ACTIVA','SUSPENDIDA','CERRADA')),
  fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
  fecha_cierre TIMESTAMPTZ,
  CONSTRAINT chk_tienda_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  CONSTRAINT chk_tienda_fecha_cierre CHECK (
    (estado='CERRADA' AND fecha_cierre IS NOT NULL) OR
    (estado<>'CERRADA' AND fecha_cierre IS NULL)
  ),
  CONSTRAINT chk_tienda_fechas CHECK (fecha_cierre IS NULL OR fecha_cierre >= fecha_creacion)
);
-- SIN UNIQUE(id_propietario): un propietario puede poseer varias tiendas.

CREATE TABLE tienda_cliente (
  id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
  id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
  fecha_registro TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id_usuario,id_tienda)
);

CREATE INDEX idx_identidad_usuario ON usuario_identidad(id_usuario);
CREATE INDEX idx_usuario_rol_rol ON usuario_rol(id_rol);
CREATE INDEX idx_tienda_propietario ON tienda(id_propietario);
CREATE INDEX idx_tienda_cliente_tienda ON tienda_cliente(id_tienda);

-- REGLAS RESUELTAS EN SPRING BOOT (M1):
-- 1) Solo usuario con rol DUENO_TIENDA puede ser propietario; alta con transaccion.
-- 2) Solo propietario o ADMIN_GENERAL autorizado administra tienda.
-- 3) Vincular identidades OIDC requiere verificar el usuario autenticado.
-- 4) TIENDA_CLIENTE: crear al asociarse expresamente o al PRIMER "agregar al carrito"
--    (carrito tiene FK a tienda_cliente).
-- 5) Cierre: validar pedidos/pagos y concurrencia; registrar trazabilidad.
-- 6) Supresion/anonimizacion de datos: flujo posterior, no cascadas indiscriminadas.
-- 7) Validar formato de documento segun tipo (DNI = 8 digitos, etc.).

-- #####################################################################
-- MODULO 2 | Catalogo, variantes de tienda y presentaciones
-- #####################################################################

CREATE TABLE categoria (
 id_categoria UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
 nombre VARCHAR(100) NOT NULL CHECK (length(btrim(nombre)) > 0),
 slug VARCHAR(120) NOT NULL CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
 activa BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE (id_tienda, slug),
 CONSTRAINT uq_categoria_tienda UNIQUE (id_categoria, id_tienda) -- destino de FK compuesta
);

-- id_tienda: redundancia controlada. La FK compuesta obliga a que sea la tienda
-- de su categoria; permite filtrar por tienda sin JOIN y SKU unico por tienda.
CREATE TABLE producto (
 id_producto UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_categoria UUID NOT NULL,
 id_tienda UUID NOT NULL,
 nombre VARCHAR(150) NOT NULL CHECK (length(btrim(nombre)) > 0),
 descripcion TEXT,
 precio_base NUMERIC(12,2) NOT NULL CHECK (precio_base >= 0),
 estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
   CHECK (estado IN ('BORRADOR','PUBLICADO','INACTIVO')),
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_producto_categoria_tienda FOREIGN KEY (id_categoria, id_tienda)
   REFERENCES categoria(id_categoria, id_tienda) ON DELETE RESTRICT,
 CONSTRAINT uq_producto_tienda UNIQUE (id_producto, id_tienda)
);

-- Caracteristicas que la tienda configura una sola vez: Color, Talla de calzado, etc.
CREATE TABLE variante (
 id_variante UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
 nombre VARCHAR(80) NOT NULL CHECK (length(btrim(nombre)) > 0),
 CONSTRAINT uq_variante_tienda_nombre UNIQUE (id_tienda, nombre)
);
CREATE UNIQUE INDEX uq_variante_nombre_ci ON variante(id_tienda, lower(btrim(nombre)));

CREATE TABLE valor_variante (
 id_valor UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_variante UUID NOT NULL REFERENCES variante(id_variante) ON DELETE RESTRICT,
 valor VARCHAR(100) NOT NULL CHECK (length(btrim(valor)) > 0),
 CONSTRAINT uq_valor_variante UNIQUE (id_variante, valor),
 CONSTRAINT uq_valor_con_variante UNIQUE (id_valor, id_variante) -- destino de FK compuesta
);
CREATE UNIQUE INDEX uq_valor_variante_ci ON valor_variante(id_variante, lower(btrim(valor)));

-- Articulo realmente vendible: SKU, precio e inventario especificos.
-- firma_valores: ids de valor_variante ordenados y unidos con '|', calculada por
-- el backend en la misma transaccion; '' = producto sin opciones.
CREATE TABLE presentacion_producto (
 id_presentacion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_producto UUID NOT NULL,
 id_tienda UUID NOT NULL,
 sku VARCHAR(80) NOT NULL CHECK (length(btrim(sku)) > 0),
 precio_override NUMERIC(12,2) CHECK (precio_override >= 0),
 estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
   CHECK (estado IN ('ACTIVA','INACTIVA')),
 firma_valores TEXT NOT NULL DEFAULT '',
 CONSTRAINT fk_presentacion_producto_tienda FOREIGN KEY (id_producto, id_tienda)
   REFERENCES producto(id_producto, id_tienda) ON DELETE RESTRICT,
 CONSTRAINT uq_sku_tienda UNIQUE (id_tienda, sku),                         -- RN-M2-06
 CONSTRAINT uq_presentacion_combinacion UNIQUE (id_producto, firma_valores) -- RN-M2-05
);

-- id_variante: FK compuesta garantiza que es la variante real del valor;
-- UNIQUE(id_presentacion,id_variante) = un solo valor por variante (RN-M2-04).
CREATE TABLE presentacion_valor (
 id_presentacion UUID NOT NULL REFERENCES presentacion_producto(id_presentacion) ON DELETE RESTRICT,
 id_valor UUID NOT NULL,
 id_variante UUID NOT NULL,
 PRIMARY KEY (id_presentacion, id_valor),
 CONSTRAINT fk_pv_valor_variante FOREIGN KEY (id_valor, id_variante)
   REFERENCES valor_variante(id_valor, id_variante) ON DELETE RESTRICT,
 CONSTRAINT uq_pv_una_por_variante UNIQUE (id_presentacion, id_variante)
);

CREATE TABLE imagen_producto (
 id_imagen UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_producto UUID NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
 storage_key TEXT NOT NULL UNIQUE CHECK (length(btrim(storage_key)) > 0),
 orden INTEGER NOT NULL DEFAULT 1 CHECK (orden >= 1),
 es_principal BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uq_imagen_principal_producto ON imagen_producto(id_producto)
 WHERE es_principal = TRUE;

CREATE TABLE presentacion_imagen (
 id_presentacion UUID NOT NULL REFERENCES presentacion_producto(id_presentacion) ON DELETE RESTRICT,
 id_imagen UUID NOT NULL REFERENCES imagen_producto(id_imagen) ON DELETE RESTRICT,
 PRIMARY KEY (id_presentacion, id_imagen)
);

CREATE TABLE coleccion (
 id_coleccion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
 nombre VARCHAR(150) NOT NULL CHECK (length(btrim(nombre)) > 0),
 slug VARCHAR(150) NOT NULL CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
 activa BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE(id_tienda, slug)
);

CREATE TABLE coleccion_producto (
 id_coleccion UUID NOT NULL REFERENCES coleccion(id_coleccion) ON DELETE RESTRICT,
 id_producto UUID NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
 PRIMARY KEY (id_coleccion, id_producto)
);

CREATE INDEX idx_categoria_tienda ON categoria(id_tienda);
CREATE INDEX idx_producto_categoria ON producto(id_categoria);
CREATE INDEX idx_producto_tienda_estado ON producto(id_tienda, estado);
CREATE INDEX idx_variante_tienda ON variante(id_tienda);
CREATE INDEX idx_valor_variante ON valor_variante(id_variante);
CREATE INDEX idx_presentacion_valor_valor ON presentacion_valor(id_valor);
CREATE INDEX idx_imagen_producto ON imagen_producto(id_producto);
CREATE INDEX idx_presentacion_imagen_imagen ON presentacion_imagen(id_imagen);
CREATE INDEX idx_coleccion_tienda ON coleccion(id_tienda);
CREATE INDEX idx_coleccion_producto_producto ON coleccion_producto(id_producto);

-- ---------- Triggers de integridad entre tiendas (M2) ----------
CREATE OR REPLACE FUNCTION nubea_validar_coleccion_producto() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT id_tienda FROM coleccion WHERE id_coleccion=NEW.id_coleccion)
    IS DISTINCT FROM (SELECT id_tienda FROM producto WHERE id_producto=NEW.id_producto) THEN
  RAISE EXCEPTION 'La colección y el producto deben pertenecer a la misma tienda';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_coleccion_producto_tienda BEFORE INSERT OR UPDATE
 ON coleccion_producto FOR EACH ROW EXECUTE FUNCTION nubea_validar_coleccion_producto();

-- "Un valor por variante" ya lo garantiza uq_pv_una_por_variante; aqui solo la tienda.
CREATE OR REPLACE FUNCTION nubea_validar_presentacion_valor() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT id_tienda FROM presentacion_producto WHERE id_presentacion=NEW.id_presentacion)
    IS DISTINCT FROM (SELECT id_tienda FROM variante WHERE id_variante=NEW.id_variante) THEN
  RAISE EXCEPTION 'El valor y la presentación deben pertenecer a la misma tienda';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_presentacion_valor_validar BEFORE INSERT OR UPDATE
 ON presentacion_valor FOR EACH ROW EXECUTE FUNCTION nubea_validar_presentacion_valor();

CREATE OR REPLACE FUNCTION nubea_validar_presentacion_imagen() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE p_presentacion UUID; p_imagen UUID;
BEGIN
 SELECT id_producto INTO p_presentacion FROM presentacion_producto
  WHERE id_presentacion=NEW.id_presentacion;
 SELECT id_producto INTO p_imagen FROM imagen_producto WHERE id_imagen=NEW.id_imagen;
 IF p_presentacion IS DISTINCT FROM p_imagen THEN
  RAISE EXCEPTION 'La imagen y la presentación deben pertenecer al mismo producto';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_presentacion_imagen_producto BEFORE INSERT OR UPDATE
 ON presentacion_imagen FOR EACH ROW EXECUTE FUNCTION nubea_validar_presentacion_imagen();

-- ---------- Triggers de inmutabilidad de pertenencia (M2) ----------
CREATE OR REPLACE FUNCTION nubea_proteger_cambio_tienda() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id_tienda IS DISTINCT FROM OLD.id_tienda THEN
  RAISE EXCEPTION 'No se permite cambiar la tienda de esta entidad';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_categoria_no_mover BEFORE UPDATE OF id_tienda ON categoria
 FOR EACH ROW EXECUTE FUNCTION nubea_proteger_cambio_tienda();
CREATE TRIGGER trg_producto_no_mover BEFORE UPDATE OF id_tienda ON producto
 FOR EACH ROW EXECUTE FUNCTION nubea_proteger_cambio_tienda();
CREATE TRIGGER trg_variante_no_mover BEFORE UPDATE OF id_tienda ON variante
 FOR EACH ROW EXECUTE FUNCTION nubea_proteger_cambio_tienda();
CREATE TRIGGER trg_coleccion_no_mover BEFORE UPDATE OF id_tienda ON coleccion
 FOR EACH ROW EXECUTE FUNCTION nubea_proteger_cambio_tienda();

CREATE OR REPLACE FUNCTION nubea_proteger_cambio_tipo_valor() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id_variante IS DISTINCT FROM OLD.id_variante THEN
  RAISE EXCEPTION 'No se permite mover un valor a otro tipo de variante';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_valor_no_mover BEFORE UPDATE OF id_variante ON valor_variante
 FOR EACH ROW EXECUTE FUNCTION nubea_proteger_cambio_tipo_valor();

CREATE OR REPLACE FUNCTION nubea_proteger_cambio_producto() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id_producto IS DISTINCT FROM OLD.id_producto THEN
  RAISE EXCEPTION 'No se permite cambiar el producto asociado a esta entidad';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_presentacion_no_mover BEFORE UPDATE OF id_producto ON presentacion_producto
 FOR EACH ROW EXECUTE FUNCTION nubea_proteger_cambio_producto();
CREATE TRIGGER trg_imagen_no_mover BEFORE UPDATE OF id_producto ON imagen_producto
 FOR EACH ROW EXECUTE FUNCTION nubea_proteger_cambio_producto();

-- REGLAS RESUELTAS EN SPRING BOOT (M2):
-- 1. Calcular firma_valores (ids ordenados) al crear/editar valores de una presentacion.
-- 2. Todas las presentaciones publicables de un producto usan el mismo conjunto de
--    tipos de variante (maximo 3).
-- 3. Publicar: tienda ACTIVA, categoria activa, presentacion ACTIVA, inventario creado.
-- 4. Autorizacion de propietario / admin, validacion de carga R2 y limpieza de archivos.

-- #####################################################################
-- MODULO 3 | Apariencia, banners, cuenta de pago e inventario
-- Integracion de pagos: solo metadata; no almacenar tokens en texto plano.
-- #####################################################################

CREATE TABLE plantilla (
    id_plantilla SMALLINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    preview_url TEXT,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_plantilla_codigo CHECK (length(btrim(codigo)) > 0),
    CONSTRAINT chk_plantilla_nombre CHECK (length(btrim(nombre)) > 0)
);

CREATE TABLE configuracion_tienda (
    id_tienda UUID PRIMARY KEY,
    id_plantilla SMALLINT NOT NULL,
    titulo_inicio VARCHAR(160),
    descripcion_inicio TEXT,
    mostrar_carrusel BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_config_tienda FOREIGN KEY (id_tienda)
        REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
    CONSTRAINT fk_config_plantilla FOREIGN KEY (id_plantilla)
        REFERENCES plantilla(id_plantilla) ON DELETE RESTRICT,
    CONSTRAINT chk_config_titulo CHECK (titulo_inicio IS NULL OR length(btrim(titulo_inicio)) > 0)
);

CREATE TABLE banner_tienda (
    id_banner UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_tienda UUID NOT NULL,
    storage_key TEXT NOT NULL UNIQUE,
    enlace_destino TEXT,
    orden SMALLINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_banner_tienda FOREIGN KEY (id_tienda)
        REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
    -- DEFERRABLE: se verifica al COMMIT, permite intercambiar el orden de banners.
    CONSTRAINT uq_banner_tienda_orden UNIQUE (id_tienda, orden) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_banner_storage_key CHECK (length(btrim(storage_key)) > 0),
    CONSTRAINT chk_banner_orden CHECK (orden > 0),
    CONSTRAINT chk_banner_enlace CHECK (enlace_destino IS NULL OR length(btrim(enlace_destino)) > 0)
);

CREATE TABLE cuenta_pago_tienda (
    id_tienda UUID PRIMARY KEY,
    proveedor VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO',
    id_cuenta_externa VARCHAR(100) UNIQUE,
    referencia_credencial TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_vinculacion TIMESTAMPTZ,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cuenta_pago_tienda FOREIGN KEY (id_tienda)
        REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
    CONSTRAINT chk_cuenta_pago_proveedor CHECK (proveedor = 'MERCADO_PAGO'),
    CONSTRAINT chk_cuenta_pago_estado CHECK (estado IN ('PENDIENTE','ACTIVA','DESCONECTADA','ERROR')),
    CONSTRAINT chk_cuenta_externa_no_vacia CHECK (id_cuenta_externa IS NULL OR length(btrim(id_cuenta_externa)) > 0),
    CONSTRAINT chk_referencia_credencial_no_vacia CHECK (referencia_credencial IS NULL OR length(btrim(referencia_credencial)) > 0),
    CONSTRAINT chk_cuenta_pago_activa CHECK (
        estado <> 'ACTIVA' OR (
            id_cuenta_externa IS NOT NULL
            AND referencia_credencial IS NOT NULL
            AND fecha_vinculacion IS NOT NULL
        )
    )
);

CREATE TABLE cuenta_pago_historial (
    id_historial BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
    id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
    estado_anterior VARCHAR(20) NOT NULL CHECK (estado_anterior IN ('PENDIENTE','ACTIVA','DESCONECTADA','ERROR')),
    estado_nuevo VARCHAR(20) NOT NULL CHECK (estado_nuevo = 'DESCONECTADA'),
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cuenta_pago_historial_tienda_fecha ON cuenta_pago_historial(id_tienda, fecha_cambio);

CREATE TABLE oauth_mercado_pago_intento (
    id_intento UUID PRIMARY KEY,
    id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
    id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
    state_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(state_hash) = 32),
    verifier_cifrado TEXT NOT NULL,
    retorno_panel TEXT NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'CONSUMIDO', 'INVALIDADO')),
    CHECK (fecha_expiracion > fecha_creacion)
);
CREATE INDEX idx_oauth_mp_intento_tienda ON oauth_mercado_pago_intento(id_tienda, fecha_creacion DESC);
CREATE UNIQUE INDEX uq_oauth_mp_pendiente_tienda ON oauth_mercado_pago_intento(id_tienda) WHERE estado = 'PENDIENTE';

CREATE TABLE inventario (
    id_presentacion UUID PRIMARY KEY,
    stock_actual INTEGER NOT NULL DEFAULT 0,
    stock_minimo INTEGER NOT NULL DEFAULT 0,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventario_presentacion FOREIGN KEY (id_presentacion)
        REFERENCES presentacion_producto(id_presentacion) ON DELETE RESTRICT,
    CONSTRAINT chk_inventario_stock CHECK (stock_actual >= 0),
    CONSTRAINT chk_inventario_minimo CHECK (stock_minimo >= 0)
);

CREATE INDEX idx_banner_tienda ON banner_tienda (id_tienda);
CREATE INDEX idx_config_plantilla ON configuracion_tienda (id_plantilla);

-- Sembrado de ejemplo opcional: ejecutar solo cuando existan los componentes React.
-- INSERT INTO plantilla(id_plantilla, codigo, nombre) VALUES (1,'minimal','Minimalista');

-- Descuento seguro dentro de la transaccion de pedido:
-- UPDATE inventario
-- SET stock_actual = stock_actual - :cantidad, fecha_actualizacion = NOW()
-- WHERE id_presentacion = :id_presentacion AND stock_actual >= :cantidad;
-- Comprobar que se actualizo exactamente 1 fila; :cantidad debe ser entero positivo.

-- REGLAS RESUELTAS EN SPRING BOOT (M3):
-- 1) Crear configuracion e inventario exigidos en transacciones de aplicacion.
-- 2) Validar que plantilla este activa al seleccionarla.
-- 3) Validar permisos de propietario y habilitacion de tienda.
-- 4) Garantizar que solo una vinculacion OAuth validada habilite cobros.
-- 5) Sincronizar fecha_actualizacion desde servicios UPDATE de Spring Boot.
-- 6) Validar almacenamiento R2, URL de destino segura y limpieza de archivos huerfanos.

-- #####################################################################
-- MODULO 4 | Clientes, carrito, pedidos, IGV y pago
-- Precios catalogo CON IGV; tasas historicas asociadas a pedidos.
-- #####################################################################

CREATE TABLE ubigeo (
 codigo_ubigeo CHAR(6) PRIMARY KEY CHECK (codigo_ubigeo ~ '^[0-9]{6}$'),
 departamento VARCHAR(100) NOT NULL CHECK (length(btrim(departamento))>0),
 provincia VARCHAR(100) NOT NULL CHECK (length(btrim(provincia))>0),
 distrito VARCHAR(100) NOT NULL CHECK (length(btrim(distrito))>0)
);
-- Cargar datos oficiales de ubigeo en un paso de importacion por separado.

CREATE TABLE direccion_cliente (
 id_direccion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
 codigo_ubigeo CHAR(6) NOT NULL REFERENCES ubigeo(codigo_ubigeo) ON DELETE RESTRICT,
 alias VARCHAR(60),
 direccion VARCHAR(250) NOT NULL CHECK (length(btrim(direccion))>0),
 referencia VARCHAR(250),
 telefono_contacto VARCHAR(20),
 es_principal BOOLEAN NOT NULL DEFAULT FALSE,
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (alias IS NULL OR length(btrim(alias))>0),
 CHECK (telefono_contacto IS NULL OR length(btrim(telefono_contacto))>0)
);
CREATE UNIQUE INDEX uq_direccion_principal_usuario
 ON direccion_cliente(id_usuario) WHERE es_principal;
CREATE INDEX idx_direccion_usuario ON direccion_cliente(id_usuario);
CREATE INDEX idx_direccion_ubigeo ON direccion_cliente(codigo_ubigeo);

-- Fechas de vigencia: [fecha_inicio,fecha_fin), fecha_fin es EXCLUSIVA.
-- creado_por: NULL en cargas iniciales; luego Spring Security asigna el ADMIN_GENERAL.
CREATE TABLE configuracion_impuesto (
 id_impuesto SMALLINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
 codigo VARCHAR(30) NOT NULL DEFAULT 'IGV_GENERAL',
 nombre VARCHAR(100) NOT NULL,
 tasa_porcentaje NUMERIC(5,2) NOT NULL CHECK (tasa_porcentaje BETWEEN 0 AND 100),
 fecha_inicio DATE NOT NULL,
 fecha_fin DATE,
 creado_por UUID REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (length(btrim(codigo))>0 AND length(btrim(nombre))>0),
 CHECK (fecha_fin IS NULL OR fecha_fin>fecha_inicio),
 CONSTRAINT excl_impuesto_vigencia EXCLUDE USING gist (
  codigo WITH =,
  daterange(fecha_inicio,fecha_fin,'[)') WITH &&
 ),
 CONSTRAINT uq_impuesto_tasa UNIQUE (id_impuesto, tasa_porcentaje) -- destino de FK compuesta
);
CREATE INDEX idx_impuesto_creado_por
  ON configuracion_impuesto(creado_por) WHERE creado_por IS NOT NULL;
INSERT INTO configuracion_impuesto(codigo,nombre,tasa_porcentaje,fecha_inicio)
 VALUES ('IGV_GENERAL','IGV + IPM (tasa conjunta)',18.00,'2026-01-01');
-- Actualizar la tasa = crear una nueva vigencia (no sobreescribir el historico).

CREATE TABLE carrito (
 id_carrito UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_usuario UUID NOT NULL,
 id_tienda UUID NOT NULL,
 estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
  CHECK (estado IN ('ACTIVO','ABANDONADO','CERRADO')),
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 FOREIGN KEY (id_usuario,id_tienda) REFERENCES tienda_cliente(id_usuario,id_tienda) ON DELETE RESTRICT,
 CONSTRAINT uq_carrito_identidad_contexto UNIQUE (id_carrito, id_usuario, id_tienda)
);
CREATE UNIQUE INDEX uq_carrito_activo_usuario_tienda
 ON carrito(id_usuario,id_tienda) WHERE estado='ACTIVO';
CREATE INDEX idx_carrito_tienda_usuario ON carrito(id_tienda,id_usuario);

CREATE TABLE carrito_item (
 id_carrito UUID NOT NULL REFERENCES carrito(id_carrito) ON DELETE RESTRICT,
 id_presentacion UUID NOT NULL REFERENCES presentacion_producto(id_presentacion) ON DELETE RESTRICT,
 cantidad INTEGER NOT NULL CHECK (cantidad>0),
 fecha_agregado TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY (id_carrito,id_presentacion)
);
CREATE INDEX idx_carrito_item_presentacion ON carrito_item(id_presentacion);

CREATE TABLE pedido (
 id_pedido UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_usuario UUID NOT NULL,
 id_tienda UUID NOT NULL,
 id_carrito UUID, -- carrito de origen, opcional
 id_impuesto SMALLINT NOT NULL,
 estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE_PAGO'
  CHECK (estado IN ('PENDIENTE_PAGO','PAGADO','CANCELADO')),
 estado_reserva VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
  CHECK (estado_reserva IN ('ACTIVA','CONSUMIDA','LIBERADA')),
 fecha_expiracion_reserva TIMESTAMPTZ NOT NULL,
 subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal>=0),
 tasa_impuesto NUMERIC(5,2) NOT NULL CHECK (tasa_impuesto BETWEEN 0 AND 100),
 monto_impuesto NUMERIC(12,2) NOT NULL CHECK (monto_impuesto>=0),
 total NUMERIC(12,2) NOT NULL CHECK (total>=0),
 -- Snapshot de entrega, no debe cambiar si se modifica direccion_cliente o ubigeo.
 nombre_receptor VARCHAR(160) NOT NULL CHECK (length(btrim(nombre_receptor))>0),
 telefono_receptor VARCHAR(20),
 direccion_entrega VARCHAR(250) NOT NULL CHECK (length(btrim(direccion_entrega))>0),
 referencia_entrega VARCHAR(250),
 ubigeo_entrega CHAR(6) NOT NULL CHECK (ubigeo_entrega ~ '^[0-9]{6}$'),
 departamento_entrega VARCHAR(100) NOT NULL,
 provincia_entrega VARCHAR(100) NOT NULL,
 distrito_entrega VARCHAR(100) NOT NULL,
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 fecha_pago TIMESTAMPTZ,
 FOREIGN KEY (id_usuario,id_tienda) REFERENCES tienda_cliente(id_usuario,id_tienda) ON DELETE RESTRICT,
 -- El carrito de origen pertenece al mismo usuario y tienda.
 CONSTRAINT fk_pedido_carrito_origen_contexto FOREIGN KEY (id_carrito, id_usuario, id_tienda)
   REFERENCES carrito(id_carrito, id_usuario, id_tienda) ON DELETE RESTRICT,
 -- La tasa copiada es exactamente la de la vigencia aplicada (RN-M4-06).
 CONSTRAINT fk_pedido_impuesto_tasa FOREIGN KEY (id_impuesto, tasa_impuesto)
   REFERENCES configuracion_impuesto(id_impuesto, tasa_porcentaje) ON DELETE RESTRICT,
 CONSTRAINT chk_pedido_total CHECK (total=subtotal+monto_impuesto),
 CONSTRAINT chk_pedido_estado_reserva CHECK (
  (estado='PAGADO' AND estado_reserva='CONSUMIDA' AND fecha_pago IS NOT NULL) OR
  (estado='CANCELADO' AND estado_reserva='LIBERADA' AND fecha_pago IS NULL) OR
  (estado='PENDIENTE_PAGO' AND estado_reserva='ACTIVA' AND fecha_pago IS NULL)
 )
);
CREATE INDEX idx_pedido_tienda_fecha ON pedido(id_tienda,fecha_creacion DESC);
CREATE INDEX idx_pedido_usuario_tienda ON pedido(id_usuario,id_tienda);
CREATE INDEX idx_pedido_reserva ON pedido(fecha_expiracion_reserva)
 WHERE estado='PENDIENTE_PAGO';
CREATE INDEX idx_pedido_carrito_origen ON pedido(id_carrito)
 WHERE id_carrito IS NOT NULL;
CREATE INDEX idx_pedido_impuesto ON pedido(id_impuesto);

CREATE TABLE pedido_item (
 id_pedido_item UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_pedido UUID NOT NULL REFERENCES pedido(id_pedido) ON DELETE RESTRICT,
 id_presentacion UUID NOT NULL REFERENCES presentacion_producto(id_presentacion) ON DELETE RESTRICT,
 nombre_producto VARCHAR(150) NOT NULL CHECK (length(btrim(nombre_producto))>0),
 descripcion_presentacion TEXT,
 cantidad INTEGER NOT NULL CHECK (cantidad>0),
 precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario>=0),
 subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal>=0),
 monto_impuesto NUMERIC(12,2) NOT NULL CHECK (monto_impuesto>=0),
 total_linea NUMERIC(12,2) NOT NULL CHECK (total_linea>=0),
 CONSTRAINT uq_pedido_presentacion UNIQUE (id_pedido,id_presentacion),
 CONSTRAINT chk_linea_total CHECK (total_linea=subtotal+monto_impuesto),
 CONSTRAINT chk_linea_precio CHECK (total_linea=precio_unitario*cantidad)
);
CREATE INDEX idx_pedido_item_presentacion ON pedido_item(id_presentacion);

-- REEMBOLSO_PENDIENTE: pago aprobado por Mercado Pago despues de cancelar el pedido.
CREATE TABLE pago (
 id_pago UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 id_pedido UUID NOT NULL REFERENCES pedido(id_pedido) ON DELETE RESTRICT,
 proveedor VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO'
  CHECK (proveedor='MERCADO_PAGO'),
 referencia_externa VARCHAR(150),
 clave_idempotencia VARCHAR(150) NOT NULL UNIQUE,
 estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
  CONSTRAINT pago_estado_check CHECK (estado IN
   ('PENDIENTE','APROBADO','RECHAZADO','CANCELADO','REEMBOLSO_PENDIENTE','REEMBOLSADO')),
 monto NUMERIC(12,2) NOT NULL CHECK (monto>0),
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 fecha_confirmacion TIMESTAMPTZ,
 CONSTRAINT chk_fecha_aprobacion CHECK (
  (estado IN ('APROBADO','REEMBOLSO_PENDIENTE','REEMBOLSADO') AND fecha_confirmacion IS NOT NULL)
  OR (estado IN ('PENDIENTE','RECHAZADO','CANCELADO') AND fecha_confirmacion IS NULL)
 )
);
CREATE UNIQUE INDEX uq_pago_referencia_proveedor
 ON pago(proveedor,referencia_externa) WHERE referencia_externa IS NOT NULL;
CREATE UNIQUE INDEX uq_pago_aprobado_pedido ON pago(id_pedido) WHERE estado='APROBADO';
CREATE INDEX idx_pago_pedido ON pago(id_pedido);

-- ---------- Triggers M4 ----------
-- Los items no pueden relacionar tienda A con presentacion de tienda B.
CREATE OR REPLACE FUNCTION nubea_validar_item_tienda() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE t_compra UUID;
BEGIN
 IF TG_TABLE_NAME='carrito_item' THEN
   SELECT id_tienda INTO t_compra FROM carrito WHERE id_carrito=NEW.id_carrito;
 ELSE
   SELECT id_tienda INTO t_compra FROM pedido WHERE id_pedido=NEW.id_pedido;
 END IF;
 IF t_compra IS DISTINCT FROM (SELECT id_tienda FROM presentacion_producto
                               WHERE id_presentacion=NEW.id_presentacion) THEN
   RAISE EXCEPTION 'Presentacion ajena a la tienda del carrito o pedido';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_carrito_item_tienda BEFORE INSERT OR UPDATE ON carrito_item
 FOR EACH ROW EXECUTE FUNCTION nubea_validar_item_tienda();
CREATE TRIGGER trg_pedido_item_tienda BEFORE INSERT OR UPDATE ON pedido_item
 FOR EACH ROW EXECUTE FUNCTION nubea_validar_item_tienda();

-- Proteger cambio de usuario/tienda en cabeceras.
CREATE OR REPLACE FUNCTION nubea_bloquear_cambio_cliente_tienda() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id_usuario IS DISTINCT FROM OLD.id_usuario OR NEW.id_tienda IS DISTINCT FROM OLD.id_tienda THEN
  RAISE EXCEPTION 'No se permite reasignar cliente o tienda en este registro';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_carrito_no_reasignar BEFORE UPDATE OF id_usuario,id_tienda ON carrito
 FOR EACH ROW EXECUTE FUNCTION nubea_bloquear_cambio_cliente_tienda();
CREATE TRIGGER trg_pedido_no_reasignar BEFORE UPDATE OF id_usuario,id_tienda ON pedido
 FOR EACH ROW EXECUTE FUNCTION nubea_bloquear_cambio_cliente_tienda();

-- Maquina de estados del pedido:
--  * PAGADO y CANCELADO son finales.
--  * Los importes quedan congelados desde el checkout.
--  * PAGADO exige un pago APROBADO por el total.
--  * Reserva ACTIVA -> LIBERADA devuelve el stock (una sola vez, porque es final).
CREATE OR REPLACE FUNCTION nubea_transicion_pedido() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.estado IN ('PAGADO','CANCELADO') AND NEW.estado <> OLD.estado THEN
   RAISE EXCEPTION 'Pedido % en estado final %, no puede pasar a %', OLD.id_pedido, OLD.estado, NEW.estado;
 END IF;
 IF (NEW.subtotal, NEW.tasa_impuesto, NEW.monto_impuesto, NEW.total, NEW.id_impuesto)
    IS DISTINCT FROM (OLD.subtotal, OLD.tasa_impuesto, OLD.monto_impuesto, OLD.total, OLD.id_impuesto) THEN
   RAISE EXCEPTION 'Los importes del pedido están congelados desde el checkout';
 END IF;
 IF NEW.estado='PAGADO' AND OLD.estado<>'PAGADO' AND NOT EXISTS (
     SELECT 1 FROM pago WHERE id_pedido=NEW.id_pedido AND estado='APROBADO' AND monto=NEW.total) THEN
   RAISE EXCEPTION 'No se puede marcar PAGADO sin un pago APROBADO por el total';
 END IF;
 IF OLD.estado_reserva='ACTIVA' AND NEW.estado_reserva='LIBERADA' THEN
   UPDATE inventario i SET stock_actual = i.stock_actual + pi.cantidad, fecha_actualizacion = now()
     FROM pedido_item pi
    WHERE pi.id_pedido = NEW.id_pedido AND pi.id_presentacion = i.id_presentacion;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_pedido_transicion BEFORE UPDATE ON pedido
 FOR EACH ROW EXECUTE FUNCTION nubea_transicion_pedido();

-- Pago: monto = total del pedido; no se aceptan intentos nuevos si el pedido no esta pendiente.
CREATE OR REPLACE FUNCTION nubea_validar_pago() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE p_total NUMERIC; p_estado VARCHAR;
BEGIN
 SELECT total, estado INTO p_total, p_estado FROM pedido WHERE id_pedido=NEW.id_pedido;
 IF NEW.monto <> p_total THEN
   RAISE EXCEPTION 'El monto del pago (%) no coincide con el total del pedido (%)', NEW.monto, p_total;
 END IF;
 IF TG_OP='INSERT' AND p_estado <> 'PENDIENTE_PAGO' THEN
   RAISE EXCEPTION 'No se aceptan nuevos intentos de pago para un pedido %', p_estado;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_pago_validar BEFORE INSERT OR UPDATE OF monto, id_pedido ON pago
 FOR EACH ROW EXECUTE FUNCTION nubea_validar_pago();

-- REGLAS RESUELTAS EN SPRING BOOT (M4):
-- 1. Checkout en UNA transaccion: descontar stock (UPDATE condicional, orden fijo por id),
--    insertar PEDIDO/PEDIDO_ITEM y cerrar carrito.
-- 2. Recalcular precios mientras es carrito; congelar en checkout (BigDecimal HALF_UP).
-- 3. Suma de lineas = totales de cabecera.
-- 4. No invocar Mercado Pago dentro de una transaccion abierta (AFTER_COMMIT).
-- 5. Webhook verificado y consultado en proveedor; actualizar con WHERE estado='PENDIENTE'.
-- 6. Vencimiento: reconciliar con Mercado Pago antes de cancelar; si es incierto, reintentar.
-- 7. Pago aprobado tarde sobre pedido CANCELADO -> estado REEMBOLSO_PENDIENTE y reembolsar.
-- 8. Nunca confiar en id_usuario enviado por el cliente; filtrar por tienda autenticada.
-- 9. Pago contra la cuenta Mercado Pago asociada a la tienda del pedido.
-- 10. Solo ADMIN_GENERAL administra configuracion tributaria.

COMMIT;
