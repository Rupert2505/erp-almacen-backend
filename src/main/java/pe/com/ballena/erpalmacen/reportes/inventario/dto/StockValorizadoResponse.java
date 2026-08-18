package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record StockValorizadoResponse(
        Long productoId,
        String codigoProducto,
        String nombreProducto,
        String familia,
        String subfamilia,
        String tipoArticulo,
        String marca,
        String unidadMedida,
        Long almacenId,
        String almacen,
        Long ubicacionId,
        String ubicacion,
        BigDecimal cantidadActual,
        BigDecimal stockMinimo,
        BigDecimal stockMaximo,
        BigDecimal costoPromedio,
        BigDecimal valorTotal,
        EstadoStockReporte estadoStock
) {
}
