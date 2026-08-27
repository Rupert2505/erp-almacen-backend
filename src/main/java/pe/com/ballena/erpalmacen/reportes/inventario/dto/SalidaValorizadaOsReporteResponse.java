package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record SalidaValorizadaOsReporteResponse(
        Page<SalidaValorizadaOsResponse> registros,
        List<SalidaValorizadaOsResumenResponse> resumenPorOs
) {
}
