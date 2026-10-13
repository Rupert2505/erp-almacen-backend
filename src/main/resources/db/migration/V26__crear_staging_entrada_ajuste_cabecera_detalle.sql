CREATE TABLE IF NOT EXISTS stg_mov_entrada_ajuste_cabecera (
    lote_migracion TEXT NOT NULL,
    sistema_origen TEXT,
    id_origen_cabecera TEXT,
    codigo_movimiento_externo TEXT NOT NULL,
    tipo_movimiento TEXT NOT NULL,
    confirmar TEXT NOT NULL,
    fecha_movimiento TIMESTAMP NOT NULL,
    usuario_username TEXT NOT NULL,
    codigo_almacen_destino TEXT NOT NULL,
    codigo_ubicacion_destino_default TEXT,
    motivo_movimiento TEXT NOT NULL,
    documento_referencia TEXT,
    observacion TEXT,
    observacion_documentaria TEXT,
    PRIMARY KEY (codigo_movimiento_externo)
);

CREATE TABLE IF NOT EXISTS stg_mov_entrada_ajuste_detalle (
    codigo_movimiento_externo TEXT NOT NULL,
    id_origen_detalle TEXT,
    linea_origen TEXT NOT NULL,
    codigo_producto TEXT NOT NULL,
    codigo_ubicacion_destino TEXT,
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario_inventariable NUMERIC(18,6),
    valor_unitario_sin_igv NUMERIC(18,6),
    tasa_igv_pct NUMERIC(7,4),
    igv_recuperable TEXT,
    total_inventariable_ref NUMERIC(18,6),
    observacion_detalle TEXT,
    PRIMARY KEY (codigo_movimiento_externo, linea_origen)
);
