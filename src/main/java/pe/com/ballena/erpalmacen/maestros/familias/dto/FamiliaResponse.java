package pe.com.ballena.erpalmacen.maestros.familias.dto;

import java.time.LocalDateTime;

public record FamiliaResponse(
        Long id,
        String nombre,
        String prefijo,
        String descripcion,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
