-- =========================================================================
-- Script de Creación de Base de Datos - PostgreSQL 18 (UUIDv7 Nativo)
-- =========================================================================

-- 1. Tabla: roles
CREATE TABLE roles (
    id_rol SMALLINT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,

    -- Restricción Check para validar que solo existan los roles permitidos
    CONSTRAINT chk_roles_nombre CHECK (nombre IN ('ADMIN_GENERAL', 'DUENO_TIENDA', 'CLIENTE'))
);

-- Inserción de roles iniciales
INSERT INTO roles (id_rol, nombre) VALUES 
(1, 'ADMIN_GENERAL'),
(2, 'DUENO_TIENDA'),
(3, 'CLIENTE');


-- 2. Tabla: usuario_plataforma
CREATE TABLE usuario_plataforma (
    id_usuario UUID PRIMARY KEY DEFAULT uuidv7(),
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    fecha_cierre TIMESTAMPTZ,

    -- Restricciones Check
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'CIERRE_PENDIENTE', 'CERRADO')),
    CONSTRAINT chk_usuario_fecha_cierre CHECK (
        (estado = 'CERRADO' AND fecha_cierre IS NOT NULL) 
        OR 
        (estado <> 'CERRADO' AND fecha_cierre IS NULL)
    )
);

-- Índice funcional para búsquedas normalizadas por email (case-insensitive y sin espacios)
CREATE INDEX idx_usuario_plataforma_email_lower 
ON usuario_plataforma (lower(trim(email)));


-- 3. Tabla: usuario_identidad
CREATE TABLE usuario_identidad (
    id_identidad UUID PRIMARY KEY DEFAULT uuidv7(),
    id_usuario UUID NOT NULL,
    proveedor VARCHAR(30) NOT NULL,
    issuer VARCHAR(255),
    subject VARCHAR(255),
    password_hash VARCHAR(255),
    fecha_vinculacion TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    -- Llave Foránea
    CONSTRAINT fk_usuario_identidad_usuario 
        FOREIGN KEY (id_usuario) 
        REFERENCES usuario_plataforma(id_usuario) 
        ON DELETE CASCADE,

    -- Restricción Check para proveedores (LOCAL vs GOOGLE)
    CONSTRAINT chk_usuario_identidad_proveedor CHECK (
        (proveedor = 'LOCAL' AND password_hash IS NOT NULL AND issuer IS NULL AND subject IS NULL) 
        OR 
        (proveedor = 'GOOGLE' AND password_hash IS NULL AND issuer IS NOT NULL AND subject IS NOT NULL)
    )
);

-- Índices adicionales para usuario_identidad
CREATE INDEX idx_usuario_identidad_usuario_proveedor 
ON usuario_identidad (id_usuario, proveedor);

CREATE INDEX idx_usuario_identidad_issuer_subject 
ON usuario_identidad (issuer, subject);


-- 4. Tabla: usuario_rol (Tabla intermedia / Relación Muchos a Muchos)
CREATE TABLE usuario_rol (
    id_usuario UUID NOT NULL,
    id_rol SMALLINT NOT NULL,

    -- Llave Primaria Compuesta (esto ya crea automáticamente el índice en (id_usuario, id_rol))
    CONSTRAINT pk_usuario_rol PRIMARY KEY (id_usuario, id_rol),

    -- Llaves Foráneas
    CONSTRAINT fk_usuario_rol_usuario 
        FOREIGN KEY (id_usuario) 
        REFERENCES usuario_plataforma(id_usuario) 
        ON DELETE CASCADE,

    CONSTRAINT fk_usuario_rol_rol 
        FOREIGN KEY (id_rol) 
        REFERENCES roles(id_rol) 
        ON DELETE RESTRICT
);

-- Índice adicional para la segunda columna de la llave compuesta (id_rol)
CREATE INDEX idx_usuario_rol_id_rol 
ON usuario_rol (id_rol);