package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalidaValorizadaOsResponse(
        LocalDateTime fecha,
        Long movimientoId,
        String numeroMovimiento,
        EstadoMovimientoInventario estado,
        String ordenTrabajo,
        Long productoId,
        String codigoProducto,
        String descripcionProducto,
        BigDecimal cantidad,
        String unidadMedida,
        BigDecimal costoInventarioUnitario,
        BigDecimal costoInventarioTotal,
        BigDecimal costoBaseComercialUnitarioSinIgv,
        BigDecimal recargoAplicadoPct,
        BigDecimal valorVentaUnitarioSinIgv,
        BigDecimal valorVentaTotalSinIgv,
        BigDecimal igvUnitario,
        BigDecimal igvTotal,
        BigDecimal precioVentaUnitarioConIgv,
        BigDecimal precioVentaTotalConIgv,
        BigDecimal diferenciaComercialSinIgv
) {
}
