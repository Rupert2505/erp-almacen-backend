package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import org.springframework.data.domain.Page;

public record StockValorizadoReporteResponse(
        Page<StockValorizadoResponse> registros,
        StockValorizadoResumenResponse resumen
) {
}
