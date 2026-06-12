ALTER TABLE movimientos_inventario
    ADD COLUMN tipo_documento VARCHAR(30),
    ADD COLUMN serie_documento VARCHAR(30),
    ADD COLUMN numero_documento VARCHAR(50),
    ADD COLUMN orden_compra VARCHAR(100),
    ADD COLUMN fecha_pedido DATE,
    ADD COLUMN fecha_recepcion DATE,
    ADD COLUMN flete NUMERIC(18,6),
    ADD COLUMN movilidad NUMERIC(18,6),
    ADD COLUMN otros_gastos NUMERIC(18,6),
    ADD CONSTRAINT chk_movimientos_flete_no_negativo CHECK (flete IS NULL OR flete >= 0),
    ADD CONSTRAINT chk_movimientos_movilidad_no_negativa CHECK (movilidad IS NULL OR movilidad >= 0),
    ADD CONSTRAINT chk_movimientos_otros_gastos_no_negativo CHECK (otros_gastos IS NULL OR otros_gastos >= 0),
    ADD CONSTRAINT chk_movimientos_fechas_pedido_recepcion CHECK (
        fecha_pedido IS NULL
        OR fecha_recepcion IS NULL
        OR fecha_recepcion >= fecha_pedido
    );

ALTER TABLE movimiento_detalle
    ALTER COLUMN costo_unitario TYPE NUMERIC(18,6),
    ALTER COLUMN total_linea TYPE NUMERIC(18,6);

ALTER TABLE stock_actual
    ALTER COLUMN costo_promedio TYPE NUMERIC(18,6);

ALTER TABLE kardex
    ALTER COLUMN costo_unitario TYPE NUMERIC(18,6),
    ALTER COLUMN costo_promedio TYPE NUMERIC(18,6);

CREATE INDEX idx_movimientos_documento_separado
    ON movimientos_inventario (tipo_documento, serie_documento, numero_documento);

CREATE INDEX idx_movimientos_orden_compra
    ON movimientos_inventario (orden_compra);
