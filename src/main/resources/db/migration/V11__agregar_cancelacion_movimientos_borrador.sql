ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS motivo_cancelacion TEXT NULL,
    ADD COLUMN IF NOT EXISTS cancelado_por VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS cancelado_en TIMESTAMP NULL;

ALTER TABLE movimientos_inventario
    DROP CONSTRAINT IF EXISTS chk_movimientos_inventario_estado;

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT chk_movimientos_inventario_estado
        CHECK (estado IN ('BORRADOR', 'CONFIRMADO', 'ANULADO', 'CANCELADO'));

CREATE INDEX IF NOT EXISTS idx_movimientos_cancelado_en
    ON movimientos_inventario(cancelado_en);
