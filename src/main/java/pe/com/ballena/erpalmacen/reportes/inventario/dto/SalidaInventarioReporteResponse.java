package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record SalidaInventarioReporteResponse(
        Page<SalidaInventarioResponse> registros,
        SalidaInventarioResumenResponse resumen,
        List<SalidaInventarioAgrupacionResponse> agrupaciones
) {
}
