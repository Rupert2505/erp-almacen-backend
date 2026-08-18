package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record SalidaInventarioResumenResponse(
        Long cantidadMovimientos,
        Long cantidadLineas,
        BigDecimal cantidadTotalSalida,
        BigDecimal valorTotalSalida
) {
}
