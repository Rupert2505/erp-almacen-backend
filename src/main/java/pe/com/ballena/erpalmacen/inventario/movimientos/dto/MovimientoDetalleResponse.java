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
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal totalLinea,
        String observacion,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
