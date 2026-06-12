package pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto;

import java.time.LocalDateTime;

public record TipoArticuloResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
