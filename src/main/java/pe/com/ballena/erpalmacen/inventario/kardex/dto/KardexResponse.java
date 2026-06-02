package pe.com.ballena.erpalmacen.inventario.kardex.dto;

import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KardexResponse(
        Long id,
        Long movimientoId,
        Long movimientoDetalleId,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        Long ubicacionId,
        String ubicacionCodigo,
        String ubicacionNombre,
        LocalDateTime fechaMovimiento,
        TipoMovimientoInventario tipoMovimiento,
        String numeroMovimiento,
        BigDecimal entrada,
        BigDecimal salida,
        BigDecimal saldoAnterior,
        BigDecimal saldoFinal,
        BigDecimal costoUnitario,
        BigDecimal costoPromedio,
        Long usuarioId,
        String username,
        String observacion,
        LocalDateTime creadoEn
) {
}
