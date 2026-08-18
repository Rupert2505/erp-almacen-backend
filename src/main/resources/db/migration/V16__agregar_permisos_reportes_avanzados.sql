INSERT INTO permisos (codigo, descripcion)
VALUES
    ('REPORTE_COSTOS_VER', 'Ver costos y valorizaciones en reportes'),
    ('REPORTE_AUDITORIA_VER', 'Ver reportes de auditoria'),
    ('REPORTE_EXPORTAR', 'Exportar reportes')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN'
  AND p.codigo IN (
      'REPORTE_COSTOS_VER',
      'REPORTE_AUDITORIA_VER',
      'REPORTE_EXPORTAR'
  )
ON CONFLICT DO NOTHING;
