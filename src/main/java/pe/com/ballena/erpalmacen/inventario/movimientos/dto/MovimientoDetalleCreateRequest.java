package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MovimientoDetalleCreateRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        Long ubicacionOrigenId,

        Long ubicacionDestinoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad,

        @DecimalMin(value = "0.0000", message = "El costo unitario no puede ser negativo")
        BigDecimal costoUnitario,

        @DecimalMin(value = "0.0000", message = "El valor unitario sin IGV no puede ser negativo")
        BigDecimal valorUnitarioSinIgv,

        @DecimalMin(value = "0.0000", message = "La tasa IGV no puede ser negativa")
        BigDecimal tasaIgv,

        Boolean igvRecuperable,

        @DecimalMin(value = "0.0000", message = "El recargo comercial debe ser mayor o igual a cero")
        BigDecimal recargoComercialPct,

        String observacion
) {
    public MovimientoDetalleCreateRequest(
            Long productoId,
            Long ubicacionOrigenId,
            Long ubicacionDestinoId,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal valorUnitarioSinIgv,
            BigDecimal tasaIgv,
            Boolean igvRecuperable,
            String observacion
    ) {
        this(
                productoId,
                ubicacionOrigenId,
                ubicacionDestinoId,
                cantidad,
                costoUnitario,
                valorUnitarioSinIgv,
                tasaIgv,
                igvRecuperable,
                null,
                observacion
        );
    }

    public MovimientoDetalleCreateRequest(
            Long productoId,
            Long ubicacionOrigenId,
            Long ubicacionDestinoId,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            String observacion
    ) {
        this(
                productoId,
                ubicacionOrigenId,
                ubicacionDestinoId,
                cantidad,
                costoUnitario,
                null,
                null,
                null,
                null,
                observacion
        );
    }
}
