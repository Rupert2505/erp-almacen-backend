package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import org.springframework.data.domain.Page;

public record ReportePaginadoResponse<T, R>(
        Page<T> registros,
        R resumen
) {
}
