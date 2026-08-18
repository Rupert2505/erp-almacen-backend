package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import org.springframework.data.domain.Page;

public record EntradaProveedorReporteResponse(
        Page<EntradaProveedorResponse> registros,
        EntradaProveedorResumenResponse resumen
) {
}
