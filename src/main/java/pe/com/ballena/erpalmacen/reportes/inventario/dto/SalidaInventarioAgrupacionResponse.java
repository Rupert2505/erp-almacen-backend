package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record SalidaInventarioAgrupacionResponse(
        String grupo,
        BigDecimal cantidadTotal,
        BigDecimal valorTotal,
        Long cantidadLineas
) {
}
