package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record ResumenInventarioResponse(
        Long totalProductos,
        Long productosActivos,
        Long totalAlmacenes,
        Long totalMovimientos,
        Long movimientosConfirmados,
        Long movimientosBorrador,
        Long movimientosAnulados,
        BigDecimal totalStockActual,
        Long productosBajoMinimo,
        BigDecimal valorInventarioEstimado
) {
}
