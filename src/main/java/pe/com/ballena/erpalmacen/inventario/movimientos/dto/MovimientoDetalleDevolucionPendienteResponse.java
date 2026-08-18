package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import java.math.BigDecimal;

public record MovimientoDetalleDevolucionPendienteResponse(
        Long movimientoDetalleOrigenId,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        Long ubicacionId,
        String ubicacionCodigo,
        String ubicacionNombre,
        BigDecimal cantidadRecibida,
        BigDecimal cantidadDevueltaConfirmada,
        BigDecimal cantidadPendienteDevolver,
        BigDecimal costoDocumentoUnitario
) {
}
