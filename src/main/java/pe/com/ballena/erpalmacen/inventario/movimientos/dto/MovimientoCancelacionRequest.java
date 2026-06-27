package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovimientoCancelacionRequest(
        @JsonAlias("motivo")
        @NotBlank(message = "El motivo de cancelacion es obligatorio")
        @Size(max = 500, message = "El motivo de cancelacion no puede superar 500 caracteres")
        String motivoCancelacion
) {
}
