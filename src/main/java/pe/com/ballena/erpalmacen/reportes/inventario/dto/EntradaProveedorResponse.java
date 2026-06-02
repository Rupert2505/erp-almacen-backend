package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EntradaProveedorResponse(
        Long proveedorId,
        String proveedorDocumento,
        String proveedorRazonSocial,
        Long movimientoId,
        String numeroMovimiento,
        LocalDateTime fechaMovimiento,
        String documentoReferencia,
        String productoCodigo,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal totalLinea
) {
}
