package pe.com.ballena.erpalmacen.reportes.inventario.dto;

public record ArchivoReporteResponse(
        String filename,
        byte[] content
) {
}
