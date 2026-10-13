CREATE TABLE IF NOT EXISTS importacion_saldos_iniciales_staging (
    id BIGSERIAL PRIMARY KEY,
    lote_migracion VARCHAR(80) NOT NULL,
    sistema_origen VARCHAR(80),
    id_origen_cabecera VARCHAR(120),
    codigo_movimiento_externo VARCHAR(100),
    tipo_movimiento VARCHAR(40),
    confirmar VARCHAR(10),
    fecha_movimiento TIMESTAMP,
    usuario_username VARCHAR(50),
    codigo_almacen_destino VARCHAR(30),
    codigo_ubicacion_destino VARCHAR(30),
    motivo_movimiento VARCHAR(150),
    documento_referencia VARCHAR(100),
    observacion_cabecera VARCHAR(500),
    observacion_documentaria TEXT,
    id_origen_detalle VARCHAR(120),
    linea_origen VARCHAR(80),
    codigo_producto VARCHAR(50),
    cantidad NUMERIC(18,4),
    costo_unitario_inventariable NUMERIC(18,6),
    valor_unitario_sin_igv NUMERIC(18,6),
    tasa_igv_pct NUMERIC(7,4),
    igv_recuperable VARCHAR(10),
    total_inventariable_ref NUMERIC(18,6),
    observacion_detalle VARCHAR(255),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_error TEXT,
    movimiento_id BIGINT,
    movimiento_detalle_id BIGINT,
    numero_movimiento VARCHAR(30),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    procesado_en TIMESTAMP,
    CONSTRAINT uk_imp_saldos_stage_lote_mov_linea
        UNIQUE (lote_migracion, codigo_movimiento_externo, linea_origen),
    CONSTRAINT fk_imp_saldos_stage_movimiento
        FOREIGN KEY (movimiento_id) REFERENCES movimientos_inventario (id),
    CONSTRAINT fk_imp_saldos_stage_detalle
        FOREIGN KEY (movimiento_detalle_id) REFERENCES movimiento_detalle (id),
    CONSTRAINT chk_imp_saldos_stage_estado
        CHECK (estado IN ('PENDIENTE', 'VALIDADO', 'ERROR', 'PROCESADO'))
);

CREATE TABLE IF NOT EXISTS importacion_saldos_iniciales_errores (
    id BIGSERIAL PRIMARY KEY,
    staging_id BIGINT,
    lote_migracion VARCHAR(80) NOT NULL,
    codigo_movimiento_externo VARCHAR(100),
    linea_origen VARCHAR(80),
    codigo_error VARCHAR(80) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_imp_saldos_error_stage
        FOREIGN KEY (staging_id) REFERENCES importacion_saldos_iniciales_staging (id)
);

CREATE TABLE IF NOT EXISTS importacion_saldos_iniciales_traza (
    id BIGSERIAL PRIMARY KEY,
    staging_id BIGINT NOT NULL,
    lote_migracion VARCHAR(80) NOT NULL,
    sistema_origen VARCHAR(80),
    id_origen_cabecera VARCHAR(120),
    id_origen_detalle VARCHAR(120),
    codigo_movimiento_externo VARCHAR(100) NOT NULL,
    linea_origen VARCHAR(80) NOT NULL,
    codigo_producto VARCHAR(50) NOT NULL,
    codigo_almacen_destino VARCHAR(30) NOT NULL,
    codigo_ubicacion_destino VARCHAR(30),
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario_inventariable NUMERIC(18,6),
    movimiento_id BIGINT NOT NULL,
    movimiento_detalle_id BIGINT NOT NULL,
    numero_movimiento VARCHAR(30) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_imp_saldos_traza_staging UNIQUE (staging_id),
    CONSTRAINT fk_imp_saldos_traza_stage
        FOREIGN KEY (staging_id) REFERENCES importacion_saldos_iniciales_staging (id),
    CONSTRAINT fk_imp_saldos_traza_mov
        FOREIGN KEY (movimiento_id) REFERENCES movimientos_inventario (id),
    CONSTRAINT fk_imp_saldos_traza_det
        FOREIGN KEY (movimiento_detalle_id) REFERENCES movimiento_detalle (id)
);

CREATE INDEX IF NOT EXISTS idx_imp_saldos_stage_lote
    ON importacion_saldos_iniciales_staging (lote_migracion);

CREATE INDEX IF NOT EXISTS idx_imp_saldos_stage_estado
    ON importacion_saldos_iniciales_staging (estado);

CREATE INDEX IF NOT EXISTS idx_imp_saldos_stage_movimiento_ext
    ON importacion_saldos_iniciales_staging (lote_migracion, codigo_movimiento_externo);

CREATE INDEX IF NOT EXISTS idx_imp_saldos_stage_producto
    ON importacion_saldos_iniciales_staging (codigo_producto);

CREATE INDEX IF NOT EXISTS idx_imp_saldos_error_lote
    ON importacion_saldos_iniciales_errores (lote_migracion);

CREATE INDEX IF NOT EXISTS idx_imp_saldos_traza_lote
    ON importacion_saldos_iniciales_traza (lote_migracion);

INSERT INTO permisos (codigo, descripcion)
VALUES (
    'INVENTARIO_IMPORTACION_SALDOS_INICIALES',
    'Validar y procesar importacion masiva de saldos iniciales de inventario'
)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre IN ('ADMIN', 'JEFE_ALMACEN')
  AND p.codigo = 'INVENTARIO_IMPORTACION_SALDOS_INICIALES'
ON CONFLICT DO NOTHING;
