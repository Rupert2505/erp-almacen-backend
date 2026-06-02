package pe.com.ballena.erpalmacen.inventario.stock.dto;

import java.math.BigDecimal;

public record StockBajoMinimoResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        Long ubicacionId,
        String ubicacionCodigo,
        String ubicacionNombre,
        BigDecimal cantidadActual,
        BigDecimal stockMinimo,
        BigDecimal diferencia
) {
}
