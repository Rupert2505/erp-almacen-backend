package pe.com.ballena.erpalmacen.almacen.almacenes.dto;

import java.time.LocalDateTime;

public record AlmacenResponse(
        Long id,
        String codigo,
        String nombre,
        String direccion,
        String responsable,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
