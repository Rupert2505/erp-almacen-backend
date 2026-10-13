INSERT INTO permisos (codigo, descripcion)
VALUES
    ('PERSONAL_VER', 'Ver personal'),
    ('PERSONAL_CREAR', 'Crear personal'),
    ('PERSONAL_EDITAR', 'Editar personal'),
    ('PERSONAL_DESACTIVAR', 'Activar y desactivar personal')
ON CONFLICT (codigo) DO UPDATE
SET descripcion = EXCLUDED.descripcion,
    actualizado_en = CURRENT_TIMESTAMP;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre IN ('ADMIN', 'JEFE_ALMACEN')
  AND p.codigo IN (
      'PERSONAL_VER',
      'PERSONAL_CREAR',
      'PERSONAL_EDITAR',
      'PERSONAL_DESACTIVAR'
  )
ON CONFLICT DO NOTHING;
