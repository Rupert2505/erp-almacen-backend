CREATE TABLE IF NOT EXISTS familias (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subfamilias (
    id BIGSERIAL PRIMARY KEY,
    familia_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT fk_subfamilias_familia FOREIGN KEY (familia_id) REFERENCES familias (id)
);

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS familia_id BIGINT;

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS subfamilia_id BIGINT;

INSERT INTO familias (nombre, descripcion, activo, creado_en, actualizado_en)
SELECT c.nombre, c.descripcion, c.activo, c.creado_en, c.actualizado_en
FROM (
    SELECT DISTINCT ON (lower(nombre))
        nombre,
        descripcion,
        activo,
        creado_en,
        actualizado_en
    FROM categorias
    ORDER BY lower(nombre), id
) c
WHERE NOT EXISTS (
    SELECT 1
    FROM familias f
    WHERE lower(f.nombre) = lower(c.nombre)
);

INSERT INTO familias (nombre, descripcion)
SELECT 'GENERAL', 'Familia temporal para productos sin categoria legacy'
WHERE EXISTS (
    SELECT 1
    FROM productos p
    WHERE p.familia_id IS NULL
      AND p.categoria_id IS NULL
)
AND NOT EXISTS (
    SELECT 1
    FROM familias f
    WHERE lower(f.nombre) = lower('GENERAL')
);

UPDATE productos p
SET familia_id = f.id
FROM categorias c
JOIN familias f ON lower(f.nombre) = lower(c.nombre)
WHERE p.categoria_id = c.id
  AND p.familia_id IS NULL;

UPDATE productos p
SET familia_id = f.id
FROM familias f
WHERE lower(f.nombre) = lower('GENERAL')
  AND p.familia_id IS NULL;

ALTER TABLE productos
    ALTER COLUMN familia_id SET NOT NULL;

ALTER TABLE productos
    ADD CONSTRAINT fk_productos_familia FOREIGN KEY (familia_id) REFERENCES familias (id);

ALTER TABLE productos
    ADD CONSTRAINT fk_productos_subfamilia FOREIGN KEY (subfamilia_id) REFERENCES subfamilias (id);

CREATE INDEX IF NOT EXISTS idx_familias_nombre ON familias (nombre);
CREATE INDEX IF NOT EXISTS idx_familias_activo ON familias (activo);
CREATE UNIQUE INDEX IF NOT EXISTS uk_familias_nombre_lower ON familias (lower(nombre));

CREATE INDEX IF NOT EXISTS idx_subfamilias_familia_id ON subfamilias (familia_id);
CREATE INDEX IF NOT EXISTS idx_subfamilias_nombre ON subfamilias (nombre);
CREATE INDEX IF NOT EXISTS idx_subfamilias_activo ON subfamilias (activo);
CREATE UNIQUE INDEX IF NOT EXISTS uk_subfamilias_familia_nombre_lower ON subfamilias (familia_id, lower(nombre));

CREATE INDEX IF NOT EXISTS idx_productos_familia_id ON productos (familia_id);
CREATE INDEX IF NOT EXISTS idx_productos_subfamilia_id ON productos (subfamilia_id);
