package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record KardexValorizadoResumenResponse(
        BigDecimal totalEntradas,
        BigDecimal totalSalidas,
        BigDecimal saldoFinalCantidad,
        BigDecimal costoPromedioFinal,
        BigDecimal valorSaldoFinal
) {
}
