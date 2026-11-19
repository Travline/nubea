CREATE TABLE storage_tarea (
    storage_key TEXT PRIMARY KEY,
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('SUBIDA', 'ELIMINAR')),
    disponible_desde TIMESTAMPTZ NOT NULL,
    intentos INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_storage_tarea_disponible ON storage_tarea(disponible_desde);
