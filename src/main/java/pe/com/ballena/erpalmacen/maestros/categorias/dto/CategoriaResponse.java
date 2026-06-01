package pe.com.ballena.erpalmacen.maestros.categorias.dto;

import java.time.LocalDateTime;

public record CategoriaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
