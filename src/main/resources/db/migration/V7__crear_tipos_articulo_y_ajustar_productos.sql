CREATE TABLE tipos_articulo (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT chk_tipos_articulo_codigo_no_vacio CHECK (btrim(codigo) <> ''),
    CONSTRAINT chk_tipos_articulo_nombre_no_vacio CHECK (btrim(nombre) <> '')
);

CREATE UNIQUE INDEX uk_tipos_articulo_codigo_lower ON tipos_articulo (lower(codigo));
CREATE UNIQUE INDEX uk_tipos_articulo_nombre_lower ON tipos_articulo (lower(nombre));
CREATE INDEX idx_tipos_articulo_activo ON tipos_articulo (activo);
CREATE INDEX idx_tipos_articulo_nombre ON tipos_articulo (nombre);

INSERT INTO tipos_articulo (codigo, nombre, descripcion)
VALUES
    ('ALMACEN', 'Almacén', 'Tipo de artículo para gestión de almacén'),
    ('AREA_SERVICIOS', 'Área servicios', 'Tipo de artículo para área de servicios'),
    ('CONSUMIDOS', 'Consumidos', 'Tipo de artículo consumido'),
    ('EPP_PERSONAL', 'EPP personal', 'Equipo de protección personal'),
    ('HERRAMIENTA_PERSONAL', 'Herramienta personal', 'Herramienta asignada a personal'),
    ('MAQUINA_PERSONAL', 'Máquina personal', 'Máquina asignada a personal'),
    ('NO_TERMINADO', 'No terminado', 'Tipo de artículo no terminado'),
    ('VENTA', 'Venta', 'Tipo de artículo para venta'),
    ('VENTA_ALMACEN', 'Venta y almacén', 'Tipo de artículo para venta y almacén');

ALTER TABLE productos ADD COLUMN tipo_articulo_id BIGINT;

UPDATE productos
SET tipo_articulo_id = (
    SELECT id
    FROM tipos_articulo
    WHERE codigo = 'ALMACEN'
)
WHERE tipo_articulo_id IS NULL;

ALTER TABLE productos
    ALTER COLUMN tipo_articulo_id SET NOT NULL,
    ADD CONSTRAINT fk_productos_tipo_articulo
        FOREIGN KEY (tipo_articulo_id) REFERENCES tipos_articulo (id);

CREATE INDEX idx_productos_tipo_articulo_id ON productos (tipo_articulo_id);
