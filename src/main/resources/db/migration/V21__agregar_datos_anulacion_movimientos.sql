ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS motivo_anulacion TEXT NULL,
    ADD COLUMN IF NOT EXISTS anulado_por VARCHAR(120) NULL;

-- Los movimientos historicos no guardaban el motivo en la cabecera. Se recupera
-- del asiento de reversion del Kardex cuando conserva el formato estandar.
UPDATE movimientos_inventario movimiento
SET motivo_anulacion = historial.motivo_anulacion
FROM (
    SELECT DISTINCT ON (movimiento_id)
        movimiento_id,
        SUBSTRING(observacion FROM POSITION(': ' IN observacion) + 2) AS motivo_anulacion
    FROM kardex
    WHERE observacion LIKE 'ANULACION MOVIMIENTO %: %'
    ORDER BY movimiento_id, id DESC
) historial
WHERE movimiento.id = historial.movimiento_id
  AND movimiento.estado = 'ANULADO'
  AND movimiento.motivo_anulacion IS NULL;
