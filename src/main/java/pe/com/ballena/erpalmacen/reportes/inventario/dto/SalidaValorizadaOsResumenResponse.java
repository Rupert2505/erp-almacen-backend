package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record SalidaValorizadaOsResumenResponse(
        String ordenTrabajo,
        Long cantidadMovimientos,
        Long cantidadItems,
        BigDecimal costoTotalInventario,
        BigDecimal valorComercialTotalSinIgv,
        BigDecimal igvTotal,
        BigDecimal precioTotalConIgv,
        BigDecimal diferenciaComercialSinIgv
) {
}
