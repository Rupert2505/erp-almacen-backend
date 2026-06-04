package pe.com.ballena.erpalmacen.usuarios.dto;

import java.time.LocalDateTime;

public record PermisoResponse(
        Long id,
        String codigo,
        String descripcion,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
