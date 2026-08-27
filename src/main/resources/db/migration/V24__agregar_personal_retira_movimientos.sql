ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS personal_retira_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_movimientos_inventario_personal_retira'
    ) THEN
        ALTER TABLE movimientos_inventario
            ADD CONSTRAINT fk_movimientos_inventario_personal_retira
            FOREIGN KEY (personal_retira_id) REFERENCES personal (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_movimientos_inventario_personal_retira_id
    ON movimientos_inventario (personal_retira_id);
