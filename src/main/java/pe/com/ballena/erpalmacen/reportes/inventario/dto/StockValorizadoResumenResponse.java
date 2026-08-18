package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record StockValorizadoResumenResponse(
        Long cantidadProductos,
        BigDecimal cantidadUnidades,
        BigDecimal valorTotalInventario,
        Long productosBajoMinimo
) {
}
