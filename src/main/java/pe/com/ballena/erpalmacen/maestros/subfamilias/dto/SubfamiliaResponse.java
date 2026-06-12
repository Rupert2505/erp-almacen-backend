package pe.com.ballena.erpalmacen.maestros.subfamilias.dto;

import java.time.LocalDateTime;

public record SubfamiliaResponse(
        Long id,
        Long familiaId,
        String familiaNombre,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
