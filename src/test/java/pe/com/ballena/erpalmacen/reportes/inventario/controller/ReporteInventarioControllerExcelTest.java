package pe.com.ballena.erpalmacen.reportes.inventario.controller;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.MovimientoInventarioReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ProductoBajoMinimoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EstadoStockReporte;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.excel.ExcelReportService;
import pe.com.ballena.erpalmacen.reportes.inventario.pdf.MovimientoPdfService;
import pe.com.ballena.erpalmacen.reportes.inventario.service.ReporteInventarioService;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteInventarioControllerExcelTest {

    @Mock
    private ReporteInventarioService reporteInventarioService;

    @Mock
    private MovimientoPdfService movimientoPdfService;

    private ReporteInventarioController controller;

    @BeforeEach
    void setUp() {
        controller = new ReporteInventarioController(
                reporteInventarioService,
                new ExcelReportService(),
                movimientoPdfService
        );
    }

    @Test
    void exportaTodosLosMovimientosComoArchivoXlsxAplicandoElRangoDeFechas() throws Exception {
        MovimientoInventarioReporteResponse row = new MovimientoInventarioReporteResponse(
                12L,
                "MOV-000012",
                TipoMovimientoInventario.ENTRADA_COMPRA,
                EstadoMovimientoInventario.CONFIRMADO,
                LocalDateTime.of(2026, 8, 10, 11, 30),
                "FAC-001",
                "REP-1004",
                "VALVULA",
                null,
                "ALMACEN GENERAL",
                new BigDecimal("3.00"),
                new BigDecimal("11.33"),
                new BigDecimal("33.99")
        );
        when(reporteInventarioService.listarMovimientos(
                eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 8, 25, 0, 0)),
                eq(TipoMovimientoInventario.ENTRADA_COMPRA),
                eq(EstadoMovimientoInventario.CONFIRMADO),
                eq(3L),
                eq(7L),
                argThat(Pageable::isUnpaged)
        )).thenReturn(new PageImpl<>(List.of(row)));

        ResponseEntity<byte[]> response = controller.exportarMovimientos(
                "2026-08-01",
                "2026-08-24",
                TipoMovimientoInventario.ENTRADA_COMPRA,
                EstadoMovimientoInventario.CONFIRMADO,
                3L,
                7L
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("movimientos-inventario.xlsx");
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getBody()).isNotNull().startsWith((byte) 'P', (byte) 'K');

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Movimientos");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("MOV-000012");
        }
    }

    @Test
    void exportaTodosLosProductosBajoMinimoComoArchivoXlsx() throws Exception {
        ProductoBajoMinimoReporteResponse row = new ProductoBajoMinimoReporteResponse(
                7L,
                "REP-1004",
                "VALVULA",
                "REPUESTOS",
                "VEHICULAR",
                3L,
                "ALMACEN GENERAL",
                "ALM-01",
                new BigDecimal("2.00"),
                new BigDecimal("10.00"),
                new BigDecimal("-8.00")
        );
        when(reporteInventarioService.listarProductosBajoMinimo(
                eq(3L),
                isNull(),
                argThat(Pageable::isUnpaged)
        )).thenReturn(new PageImpl<>(List.of(row)));

        ResponseEntity<byte[]> response = controller.exportarProductosBajoMinimo(3L, null);

        verify(reporteInventarioService).listarProductosBajoMinimo(
                eq(3L),
                isNull(),
                argThat(Pageable::isUnpaged)
        );
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("productos-bajo-minimo.xlsx");
        assertThat(response.getBody()).isNotNull().startsWith((byte) 'P', (byte) 'K');

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Productos bajo minimo");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("REP-1004");
        }
    }

    @Test
    void exportaStockValorizadoSinColumnasDeFamilia() throws Exception {
        StockValorizadoResponse row = new StockValorizadoResponse(
                7L,
                "REP-1004",
                "VALVULA",
                "REPUESTOS",
                "VEHICULAR",
                "REPUESTO",
                "MARCA",
                "UNIDAD",
                3L,
                "ALMACEN GENERAL",
                4L,
                "A1 - 1",
                new BigDecimal("2.00"),
                new BigDecimal("10.00"),
                null,
                new BigDecimal("11.33"),
                new BigDecimal("22.66"),
                EstadoStockReporte.BAJO_MINIMO
        );
        when(reporteInventarioService.obtenerStockValorizado(
                eq(3L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(false), argThat(Pageable::isUnpaged)
        )).thenReturn(new StockValorizadoReporteResponse(new PageImpl<>(List.of(row)), null));

        ResponseEntity<byte[]> response = controller.exportarStockValorizado(
                3L, null, null, null, null, null, null, null, null, null, null
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            List<String> headers = new ArrayList<>();
            workbook.getSheetAt(0).getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));

            assertThat(headers).doesNotContain("Familia", "Subfamilia");
            assertThat(headers).contains("Producto", "Tipo articulo", "Almacen");
        }
    }
}
