package pe.com.ballena.erpalmacen.maestros.productos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Long categoriaId,
        String categoriaCodigo,
        String categoriaNombre,
        Long marcaId,
        String marcaCodigo,
        String marcaNombre,
        Long unidadMedidaId,
        String unidadMedidaCodigo,
        String unidadMedidaNombre,
        String unidadMedidaAbreviatura,
        BigDecimal stockMinimo,
        BigDecimal stockMaximo,
        BigDecimal costoReferencial,
        boolean controlaLote,
        boolean controlaSerie,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
