package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioReporteResponse(
        Long movimientoId,
        String numero,
        TipoMovimientoInventario tipoMovimiento,
        EstadoMovimientoInventario estado,
        LocalDateTime fechaMovimiento,
        String documentoReferencia,
        String productoCodigo,
        String productoNombre,
        String almacenOrigenNombre,
        String almacenDestinoNombre,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal totalLinea
) {
}
