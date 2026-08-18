package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EntradaProveedorResponse(
        LocalDateTime fechaMovimiento,
        LocalDate fechaPedido,
        LocalDate fechaRecepcion,
        Long movimientoId,
        String numeroMovimiento,
        Long proveedorId,
        String proveedorNumeroDocumento,
        String proveedorRazonSocial,
        String ordenCompra,
        String guiaSerie,
        String guiaNumero,
        LocalDate guiaFecha,
        String comprobanteTipo,
        String comprobanteSerie,
        String comprobanteNumero,
        LocalDate comprobanteFecha,
        String almacenDestino,
        Long productoId,
        String codigoProducto,
        String nombreProducto,
        String unidadMedida,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal valorLinea,
        BigDecimal flete,
        BigDecimal movilidad,
        BigDecimal otrosGastos
) {
}
