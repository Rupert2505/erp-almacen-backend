package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KardexValorizadoDetalleResponse(
        LocalDateTime fecha,
        Long movimientoId,
        String numeroMovimiento,
        TipoMovimientoInventario tipoMovimiento,
        EstadoMovimientoInventario estadoMovimiento,
        String documentoReferencia,
        BigDecimal cantidadEntrada,
        BigDecimal cantidadSalida,
        BigDecimal saldoAnterior,
        BigDecimal saldoFinal,
        BigDecimal costoUnitarioMovimiento,
        BigDecimal costoPromedio,
        BigDecimal valorEntrada,
        BigDecimal valorSalida,
        BigDecimal valorSaldo,
        String usuario
) {
}
