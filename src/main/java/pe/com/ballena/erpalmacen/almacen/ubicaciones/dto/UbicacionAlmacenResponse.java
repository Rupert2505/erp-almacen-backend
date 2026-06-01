package pe.com.ballena.erpalmacen.almacen.ubicaciones.dto;

import java.time.LocalDateTime;

public record UbicacionAlmacenResponse(
        Long id,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
