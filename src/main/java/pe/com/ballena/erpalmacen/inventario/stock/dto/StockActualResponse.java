package pe.com.ballena.erpalmacen.inventario.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockActualResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        Long ubicacionId,
        String ubicacionCodigo,
        String ubicacionNombre,
        BigDecimal cantidadActual,
        BigDecimal costoPromedio,
        LocalDateTime fechaUltimoMovimiento,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
