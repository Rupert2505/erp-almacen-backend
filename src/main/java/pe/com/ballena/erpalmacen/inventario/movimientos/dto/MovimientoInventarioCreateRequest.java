package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MovimientoInventarioCreateRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimientoInventario tipoMovimiento,

        LocalDateTime fechaMovimiento,

        Long proveedorId,

        Long almacenOrigenId,

        Long ubicacionOrigenId,

        Long almacenDestinoId,

        Long ubicacionDestinoId,

        @Size(max = 100, message = "El documento de referencia no puede superar 100 caracteres")
        String documentoReferencia,

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        String observacion,

        @Size(max = 150, message = "El motivo del movimiento no puede superar 150 caracteres")
        String motivoMovimiento,

        @Size(max = 80, message = "La orden de trabajo no puede superar 80 caracteres")
        String ordenTrabajo,

        @Size(max = 120, message = "El area solicitante no puede superar 120 caracteres")
        String areaSolicitante,

        @Size(max = 120, message = "El solicitante no puede superar 120 caracteres")
        String solicitante,

        @Size(max = 120, message = "El responsable de entrega no puede superar 120 caracteres")
        String responsableEntrega,

        @Size(max = 120, message = "El responsable de recepcion no puede superar 120 caracteres")
        String responsableRecepcion,

        Boolean confirmar,

        @Size(max = 20, message = "La serie de la guia no puede superar 20 caracteres")
        String guiaSerie,

        @Size(max = 50, message = "El numero de la guia no puede superar 50 caracteres")
        String guiaNumero,

        LocalDate guiaFecha,

        @Size(max = 30, message = "El tipo de comprobante no puede superar 30 caracteres")
        String comprobanteTipo,

        @Size(max = 20, message = "La serie del comprobante no puede superar 20 caracteres")
        String comprobanteSerie,

        @Size(max = 50, message = "El numero del comprobante no puede superar 50 caracteres")
        String comprobanteNumero,

        LocalDate comprobanteFechaEmision,

        @Size(max = 50, message = "El numero de orden de compra no puede superar 50 caracteres")
        String ordenCompraNumero,

        @Size(max = 30, message = "El tipo de documento no puede superar 30 caracteres")
        String tipoDocumento,

        @Size(max = 30, message = "La serie del documento no puede superar 30 caracteres")
        String serieDocumento,

        @Size(max = 50, message = "El numero del documento no puede superar 50 caracteres")
        String numeroDocumento,

        @Size(max = 100, message = "La orden de compra no puede superar 100 caracteres")
        String ordenCompra,

        LocalDate fechaPedido,

        LocalDate fechaRecepcion,

        @jakarta.validation.constraints.DecimalMin(value = "0.000000", message = "El flete no puede ser negativo")
        BigDecimal flete,

        @jakarta.validation.constraints.DecimalMin(value = "0.000000", message = "La movilidad no puede ser negativa")
        BigDecimal movilidad,

        @jakarta.validation.constraints.DecimalMin(value = "0.000000", message = "Otros gastos no puede ser negativo")
        BigDecimal otrosGastos,

        String observacionDocumentaria,

        @Valid
        @NotEmpty(message = "El movimiento debe tener al menos un detalle")
        List<MovimientoDetalleCreateRequest> detalles
) {
    public MovimientoInventarioCreateRequest(
            TipoMovimientoInventario tipoMovimiento,
            Long proveedorId,
            Long almacenOrigenId,
            Long almacenDestinoId,
            String documentoReferencia,
            String observacion,
            List<MovimientoDetalleCreateRequest> detalles
    ) {
        this(
                tipoMovimiento,
                null,
                proveedorId,
                almacenOrigenId,
                null,
                almacenDestinoId,
                null,
                documentoReferencia,
                observacion,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                detalles
        );
    }
}
