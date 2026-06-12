ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS motivo_movimiento VARCHAR(150),
    ADD COLUMN IF NOT EXISTS orden_trabajo VARCHAR(80),
    ADD COLUMN IF NOT EXISTS area_solicitante VARCHAR(120),
    ADD COLUMN IF NOT EXISTS solicitante VARCHAR(120),
    ADD COLUMN IF NOT EXISTS responsable_entrega VARCHAR(120),
    ADD COLUMN IF NOT EXISTS responsable_recepcion VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_movimientos_motivo_movimiento
    ON movimientos_inventario (motivo_movimiento);

CREATE INDEX IF NOT EXISTS idx_movimientos_orden_trabajo
    ON movimientos_inventario (orden_trabajo);
