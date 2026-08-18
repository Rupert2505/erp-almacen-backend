package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDetalleResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Long ubicacionOrigenId,
        String ubicacionOrigenCodigo,
        String ubicacionOrigenNombre,
        Long ubicacionDestinoId,
        String ubicacionDestinoCodigo,
        String ubicacionDestinoNombre,
        Long movimientoDetalleOrigenId,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal valorUnitarioSinIgv,
        BigDecimal tasaIgv,
        BigDecimal montoIgvUnitario,
        BigDecimal montoIgvTotal,
        BigDecimal precioUnitarioConIgv,
        Boolean igvRecuperable,
        BigDecimal costoUnitarioInventariable,
        BigDecimal totalInventariable,
        BigDecimal totalConIgv,
        BigDecimal totalLinea,
        BigDecimal costoDocumentoUnitario,
        BigDecimal valorStock,
        BigDecimal valorDocumento,
        BigDecimal diferenciaValorizacion,
        String observacion,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
