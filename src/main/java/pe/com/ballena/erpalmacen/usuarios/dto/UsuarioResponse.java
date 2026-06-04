package pe.com.ballena.erpalmacen.usuarios.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String username,
        String nombres,
        String apellidos,
        String email,
        boolean activo,
        List<RolResponse> roles,
        List<PermisoResponse> permisos,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
