package pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto;

import java.time.LocalDateTime;

public record UnidadMedidaResponse(
        Long id,
        String codigo,
        String nombre,
        String abreviatura,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
