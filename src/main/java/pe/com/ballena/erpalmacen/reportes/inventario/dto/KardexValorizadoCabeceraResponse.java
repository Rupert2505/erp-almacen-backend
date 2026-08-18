package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KardexValorizadoCabeceraResponse(
        Long productoId,
        String codigoProducto,
        String nombreProducto,
        String unidadMedida,
        Long almacenId,
        String almacen,
        Long ubicacionId,
        String ubicacion,
        LocalDateTime fechaDesde,
        LocalDateTime fechaHasta,
        BigDecimal saldoInicialCantidad,
        BigDecimal saldoInicialCostoPromedio,
        BigDecimal saldoInicialValor
) {
}
