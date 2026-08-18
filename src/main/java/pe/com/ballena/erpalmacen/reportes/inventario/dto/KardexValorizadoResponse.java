package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.util.List;

public record KardexValorizadoResponse(
        KardexValorizadoCabeceraResponse cabecera,
        List<KardexValorizadoDetalleResponse> detalles,
        KardexValorizadoResumenResponse resumen
) {
}
