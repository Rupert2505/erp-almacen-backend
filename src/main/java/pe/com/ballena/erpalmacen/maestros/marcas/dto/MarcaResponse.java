package pe.com.ballena.erpalmacen.maestros.marcas.dto;

import java.time.LocalDateTime;

public record MarcaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
