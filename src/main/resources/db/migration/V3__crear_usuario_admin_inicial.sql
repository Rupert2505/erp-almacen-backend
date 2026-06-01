INSERT INTO usuarios (username, password, nombres, apellidos, email, activo)
SELECT
    'admin',
    '$2a$10$DC6mitZ4voyiP128KCbOveDn3xqQXj0MRsgz4bE9I2hc73aGO4BDO',
    'Administrador',
    'Sistema',
    'admin@erpalmacen.local',
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM usuarios
    WHERE username = 'admin'
);

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.nombre = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM usuario_roles ur
      WHERE ur.usuario_id = u.id
        AND ur.rol_id = r.id
  );
