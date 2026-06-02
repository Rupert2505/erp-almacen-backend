package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record StockValorizadoResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Long almacenId,
        String almacenNombre,
        Long ubicacionId,
        String ubicacionNombre,
        BigDecimal cantidadActual,
        BigDecimal costoPromedio,
        BigDecimal valorTotal
) {
}
