package pe.com.ballena.erpalmacen.usuarios.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RolResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activo,
        List<PermisoResponse> permisos,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
