package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record ProductoBajoMinimoReporteResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        String categoriaNombre,
        Long almacenId,
        String almacenNombre,
        BigDecimal cantidadActual,
        BigDecimal stockMinimo,
        BigDecimal diferencia
) {
}
