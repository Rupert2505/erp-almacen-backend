package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;

public record EntradaProveedorResumenResponse(
        Long cantidadMovimientos,
        Long cantidadProveedores,
        Long cantidadProductos,
        BigDecimal valorProductos,
        BigDecimal totalFlete,
        BigDecimal totalMovilidad,
        BigDecimal totalOtrosGastos,
        BigDecimal totalDocumentario
) {
}
