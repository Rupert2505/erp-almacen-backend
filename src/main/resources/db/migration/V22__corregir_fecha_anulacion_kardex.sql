UPDATE kardex kardex_anulacion
SET fecha_movimiento = movimiento.anulado_en
FROM movimientos_inventario movimiento
WHERE kardex_anulacion.movimiento_id = movimiento.id
  AND movimiento.estado = 'ANULADO'
  AND movimiento.anulado_en IS NOT NULL
  AND kardex_anulacion.observacion LIKE 'ANULACION MOVIMIENTO %'
  AND kardex_anulacion.fecha_movimiento IS DISTINCT FROM movimiento.anulado_en;
