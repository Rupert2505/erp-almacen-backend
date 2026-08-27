package pe.com.ballena.erpalmacen.inventario.service;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.repository.UbicacionAlmacenRepository;
import pe.com.ballena.erpalmacen.inventario.kardex.repository.KardexRepository;
import pe.com.ballena.erpalmacen.inventario.kardex.service.KardexConsultaService;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.DevolucionProveedorCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.DevolucionProveedorDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoAnulacionRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.stock.repository.StockActualRepository;
import pe.com.ballena.erpalmacen.inventario.stock.entity.StockActualEntity;
import pe.com.ballena.erpalmacen.inventario.stock.service.StockConsultaService;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.personal.entity.PersonalEntity;
import pe.com.ballena.erpalmacen.maestros.personal.repository.PersonalRepository;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.maestros.proveedores.repository.ProveedorRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.reportes.inventario.service.ReporteInventarioService;
import pe.com.ballena.erpalmacen.reportes.inventario.pdf.MovimientoPdfService;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InventarioConsultaReporteServiceTest {

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @Autowired
    private StockConsultaService stockConsultaService;

    @Autowired
    private KardexConsultaService kardexConsultaService;

    @Autowired
    private ReporteInventarioService reporteInventarioService;

    @Autowired
    private MovimientoPdfService movimientoPdfService;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private StockActualRepository stockActualRepository;

    @Autowired
    private KardexRepository kardexRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private PersonalRepository personalRepository;

    @Autowired
    private TipoArticuloRepository tipoArticuloRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private UbicacionAlmacenRepository ubicacionAlmacenRepository;

    @Test
    void reporteSinPaginacionDevuelveTodosLosRegistrosFiltradosParaExportar() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        var primera = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-EXP-1-" + data.suffix(),
                new BigDecimal("2.0000"),
                new BigDecimal("3.000000")
        ), authentication());
        var segunda = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-EXP-2-" + data.suffix(),
                new BigDecimal("4.0000"),
                new BigDecimal("3.000000")
        ), authentication());

        var reporte = reporteInventarioService.listarMovimientos(
                null,
                null,
                null,
                EstadoMovimientoInventario.CONFIRMADO,
                data.almacenOrigen().getId(),
                data.producto().getId(),
                Pageable.unpaged()
        );

        assertThat(reporte.getContent())
                .extracting(r -> r.movimientoId())
                .contains(primera.id(), segunda.id());
        assertThat(reporte.getPageable().isUnpaged()).isTrue();
    }

    @Test
    void consultasStockKardexYReportesReflejanMovimientosConfirmados() {
        TestData data = crearDatosBase(new BigDecimal("10.0000"));
        var entrada = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-CONF-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("2.500000")
        ), authentication());
        var borrador = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                false,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-BOR-" + data.suffix(),
                new BigDecimal("8.0000"),
                new BigDecimal("9.000000")
        ), authentication());

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionId(
                data.producto().getId(),
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("5.0000");

        assertThat(stockConsultaService.listar(
                data.producto().getId(),
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                data.producto().getCodigo(),
                true,
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(s -> s.id().equals(stock.getId()));
        assertThat(stockConsultaService.listarBajoMinimo(data.almacenOrigen().getId(), PageRequest.of(0, 10)).getContent())
                .anyMatch(s -> s.productoId().equals(data.producto().getId()));

        assertThat(kardexConsultaService.listarPorProducto(data.producto().getId(), null, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(k -> k.movimientoId().equals(entrada.id()));
        assertThat(kardexConsultaService.listarPorMovimiento(entrada.id()))
                .hasSize(1)
                .allMatch(k -> k.entrada().compareTo(BigDecimal.ZERO) > 0);

        assertThat(reporteInventarioService.listarMovimientos(
                null,
                null,
                null,
                EstadoMovimientoInventario.CONFIRMADO,
                data.almacenOrigen().getId(),
                data.producto().getId(),
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(r -> r.movimientoId().equals(entrada.id()));
        assertThat(reporteInventarioService.listarMovimientos(
                null,
                null,
                null,
                EstadoMovimientoInventario.CONFIRMADO,
                data.almacenOrigen().getId(),
                data.producto().getId(),
                PageRequest.of(0, 10)
        ).getContent()).noneMatch(r -> r.movimientoId().equals(borrador.id()));
        assertThat(reporteInventarioService.listarStockValorizado(
                data.almacenOrigen().getId(),
                data.producto().getId(),
                null,
                true,
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(r -> r.valorTotal().compareTo(BigDecimal.ZERO) > 0);
        assertThat(reporteInventarioService.listarEntradasProveedor(
                data.proveedor().getId(),
                null,
                null,
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(r -> r.movimientoId().equals(entrada.id()));
        assertThat(reporteInventarioService.listarProductosBajoMinimo(
                data.almacenOrigen().getId(),
                null,
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(r ->
                r.productoId().equals(data.producto().getId())
                        && r.almacenCodigo().equals(data.almacenOrigen().getCodigo())
        );
        assertThat(reporteInventarioService.obtenerResumen().totalStockActual()).isNotNull();
    }

    @Test
    void kardexOrdenaPorFechaEIdDescYAnulacionUsaFechaReal() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        LocalDateTime fechaOriginal = LocalDateTime.now().minusDays(1).withNano(0);
        var entrada = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-ORD-KAR-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("2.500000"),
                fechaOriginal
        ), authentication());

        var anulada = movimientoInventarioService.anularMovimiento(
                entrada.id(),
                new MovimientoAnulacionRequest("Error de ingreso"),
                authentication()
        );

        var registros = kardexConsultaService.listarPorProducto(
                        data.producto().getId(),
                        null,
                        null,
                        PageRequest.of(0, 10)
                ).getContent().stream()
                .filter(kardex -> kardex.movimientoId().equals(entrada.id()))
                .toList();

        assertThat(registros).hasSize(2);
        assertThat(registros.get(0).id()).isGreaterThan(registros.get(1).id());
        assertThat(registros.get(0).fechaMovimiento()).isEqualTo(anulada.anuladoEn());
        assertThat(registros.get(0).fechaMovimiento()).isAfter(registros.get(1).fechaMovimiento());
        assertThat(registros.get(0).observacion()).contains("ANULACION MOVIMIENTO");
        assertThat(registros.get(1).fechaMovimiento()).isEqualTo(fechaOriginal);
    }

    @Test
    void costoPromedioPonderadoSalidasYTransferenciasSeMantienenConsistentes() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-UNO-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("2.000000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-DOS-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("4.000000")
        ), authentication());

        var stockAntesSalida = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stockAntesSalida.getCantidadActual()).isEqualByComparingTo("20.0000");
        assertThat(stockAntesSalida.getCostoPromedio()).isEqualByComparingTo("3.000000");

        var salida = movimientoInventarioService.crearMovimiento(salida(
                TipoMovimientoInventario.SALIDA_VENTA,
                data,
                true,
                "SAL-VENTA-" + data.suffix(),
                new BigDecimal("5.0000")
        ), authentication());
        var stockDespuesSalida = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stockDespuesSalida.getCantidadActual()).isEqualByComparingTo("15.0000");
        assertThat(stockDespuesSalida.getCostoPromedio()).isEqualByComparingTo("3.000000");
        assertThat(kardexRepository.findByMovimientoId(salida.id()).get(0).getCostoUnitario())
                .isEqualByComparingTo("3.000000");

        movimientoInventarioService.crearMovimiento(transferencia(
                data,
                true,
                "TRF-" + data.suffix(),
                new BigDecimal("5.0000")
        ), authentication());
        var stockOrigen = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        var stockDestino = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();

        assertThat(stockOrigen.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stockOrigen.getCostoPromedio()).isEqualByComparingTo("3.000000");
        assertThat(stockDestino.getCantidadActual()).isEqualByComparingTo("5.0000");
        assertThat(stockDestino.getCostoPromedio()).isEqualByComparingTo("3.000000");
    }

    @Test
    void reportesNoFallanConFiltrosSinResultados() {
        Long idInexistente = -999999L;

        assertThat(reporteInventarioService.listarMovimientos(
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(11),
                null,
                EstadoMovimientoInventario.CONFIRMADO,
                idInexistente,
                idInexistente,
                PageRequest.of(0, 5)
        ).getContent()).isEmpty();
        assertThat(reporteInventarioService.listarStockValorizado(
                idInexistente,
                idInexistente,
                null,
                true,
                PageRequest.of(0, 5)
        ).getContent()).isEmpty();
        assertThat(reporteInventarioService.listarEntradasProveedor(
                idInexistente,
                null,
                null,
                PageRequest.of(0, 5)
        ).getContent()).isEmpty();
        assertThat(reporteInventarioService.listarSalidas(
                null,
                null,
                TipoMovimientoInventario.SALIDA_CONSUMO,
                idInexistente,
                idInexistente,
                PageRequest.of(0, 5)
        ).getContent()).isEmpty();
    }

    @Test
    void devolucionProveedorApareceEnMovimientosKardexYReporteDeSalidas() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        var entrada = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-DEV-REP-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());

        var devolucion = movimientoInventarioService.crearDevolucionProveedor(new DevolucionProveedorCreateRequest(
                entrada.id(),
                data.proveedor().getId(),
                LocalDateTime.now(),
                LocalDate.now(),
                "NC-" + data.suffix(),
                "Devolucion por reporte",
                "Devolucion visible en reportes",
                true,
                List.of(new DevolucionProveedorDetalleCreateRequest(
                        entrada.detalles().get(0).id(),
                        data.producto().getId(),
                        new BigDecimal("3.0000"),
                        "Detalle devolucion reporte"
                ))
        ), authentication());

        assertThat(reporteInventarioService.listarMovimientos(
                null,
                null,
                TipoMovimientoInventario.DEVOLUCION_PROVEEDOR,
                EstadoMovimientoInventario.CONFIRMADO,
                data.almacenOrigen().getId(),
                data.producto().getId(),
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(r -> r.movimientoId().equals(devolucion.id()));
        assertThat(reporteInventarioService.listarSalidas(
                null,
                null,
                null,
                data.almacenOrigen().getId(),
                data.producto().getId(),
                PageRequest.of(0, 10)
        ).getContent()).noneMatch(r -> r.movimientoId().equals(devolucion.id()));
        assertThat(reporteInventarioService.listarSalidas(
                null,
                null,
                TipoMovimientoInventario.DEVOLUCION_PROVEEDOR,
                data.almacenOrigen().getId(),
                data.producto().getId(),
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(r -> r.movimientoId().equals(devolucion.id()));
        assertThat(kardexConsultaService.listarPorMovimiento(devolucion.id()))
                .hasSize(1)
                .allSatisfy(kardex -> assertThat(kardex.salida()).isEqualByComparingTo("3.0000"));
    }

    @Test
    void stockValorizadoDevuelveResumenFiltrosYEstadoBajoMinimo() {
        TestData data = crearDatosBase(new BigDecimal("10.0000"));
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-STOCK-VAL-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("2.500000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerStockValorizado(
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                data.producto().getId(),
                data.producto().getFamilia().getId(),
                null,
                data.producto().getTipoArticulo().getId(),
                null,
                null,
                true,
                true,
                data.producto().getCodigo(),
                true,
                PageRequest.of(0, 10)
        );

        assertThat(reporte.registros().getContent()).hasSize(1);
        var item = reporte.registros().getContent().get(0);
        assertThat(item.valorTotal()).isEqualByComparingTo("12.5000000000");
        assertThat(item.unidadMedida()).isEqualTo(data.producto().getUnidadMedida().getAbreviatura());
        assertThat(item.estadoStock().name()).isEqualTo("BAJO_MINIMO");
        assertThat(reporte.resumen().cantidadProductos()).isEqualTo(1);
        assertThat(reporte.resumen().cantidadUnidades()).isEqualByComparingTo("5.0000");
        assertThat(reporte.resumen().valorTotalInventario()).isEqualByComparingTo("12.5000000000");
        assertThat(reporte.resumen().productosBajoMinimo()).isEqualTo(1);
        assertThat(reporte.resumen().productosSinStockActivos()).isZero();
    }

    @Test
    void resumenStockCuentaComoUnicoSoloElProductoActivoConSaldoConsolidadoCero() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-SIN-STOCK-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("2.500000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(salida(
                TipoMovimientoInventario.SALIDA_AJUSTE,
                data,
                true,
                "SAL-SIN-STOCK-" + data.suffix(),
                new BigDecimal("5.0000")
        ), authentication());
        StockActualEntity segundaLineaSinStock = new StockActualEntity();
        segundaLineaSinStock.setProducto(data.producto());
        segundaLineaSinStock.setAlmacen(data.almacenDestino());
        segundaLineaSinStock.setCantidadActual(BigDecimal.ZERO);
        stockActualRepository.saveAndFlush(segundaLineaSinStock);

        var reporteActivo = reporteInventarioService.obtenerStockValorizado(
                null,
                null,
                data.producto().getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10)
        );

        assertThat(reporteActivo.resumen().cantidadProductos()).isEqualTo(1);
        assertThat(reporteActivo.registros().getTotalElements()).isEqualTo(2);
        assertThat(reporteActivo.resumen().productosSinStockActivos()).isEqualTo(1);

        data.producto().setActivo(false);
        productoRepository.saveAndFlush(data.producto());

        var reporteInactivo = reporteInventarioService.obtenerStockValorizado(
                null,
                null,
                data.producto().getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10)
        );

        assertThat(reporteInactivo.resumen().productosSinStockActivos()).isZero();
    }

    @Test
    void kardexValorizadoResumenConUnaSolaUbicacionUsaUltimoSaldoDeLaLinea() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-KAR-1UB-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("2.500000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerKardexValorizado(
                data.producto().getId(),
                data.almacenOrigen().getId(),
                null,
                null,
                null,
                true
        );

        assertThat(reporte.detalles()).hasSize(1);
        assertThat(reporte.resumen().totalEntradas()).isEqualByComparingTo("10.0000");
        assertThat(reporte.resumen().totalSalidas()).isEqualByComparingTo("0.0000");
        assertThat(reporte.resumen().saldoFinalCantidad()).isEqualByComparingTo("10.0000");
        assertThat(reporte.resumen().costoPromedioFinal()).isEqualByComparingTo("2.500000");
        assertThat(reporte.resumen().valorSaldoFinal()).isEqualByComparingTo("25.0000000000");
    }

    @Test
    void kardexValorizadoResumenConsolidaDosUbicacionesDelMismoAlmacen() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        UbicacionAlmacenEntity segundaUbicacion = crearUbicacion("U2" + data.suffix(), data.almacenOrigen());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-KAR-U1-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("2.000000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                segundaUbicacion.getId(),
                "ENT-KAR-U2-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("3.000000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerKardexValorizado(
                data.producto().getId(),
                data.almacenOrigen().getId(),
                null,
                null,
                null,
                true
        );

        assertThat(reporte.detalles()).hasSize(2);
        assertThat(reporte.resumen().totalEntradas()).isEqualByComparingTo("15.0000");
        assertThat(reporte.resumen().saldoFinalCantidad()).isEqualByComparingTo("15.0000");
        assertThat(reporte.resumen().valorSaldoFinal()).isEqualByComparingTo("35.0000000000");
    }

    @Test
    void kardexValorizadoResumenConsolidaDosAlmacenes() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-KAR-A1-" + data.suffix(),
                new BigDecimal("4.0000"),
                new BigDecimal("2.000000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenDestino().getId(),
                null,
                "ENT-KAR-A2-" + data.suffix(),
                new BigDecimal("6.0000"),
                new BigDecimal("5.000000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerKardexValorizado(
                data.producto().getId(),
                null,
                null,
                null,
                null,
                true
        );

        assertThat(reporte.detalles()).hasSize(2);
        assertThat(reporte.resumen().totalEntradas()).isEqualByComparingTo("10.0000");
        assertThat(reporte.resumen().saldoFinalCantidad()).isEqualByComparingTo("10.0000");
        assertThat(reporte.resumen().valorSaldoFinal()).isEqualByComparingTo("38.0000000000");
    }

    @Test
    void kardexValorizadoResumenRespetaFiltroPorUbicacion() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        UbicacionAlmacenEntity segundaUbicacion = crearUbicacion("U2" + data.suffix(), data.almacenOrigen());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-KAR-FIL-U1-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("2.000000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                segundaUbicacion.getId(),
                "ENT-KAR-FIL-U2-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("3.000000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerKardexValorizado(
                data.producto().getId(),
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                null,
                null,
                true
        );

        assertThat(reporte.detalles()).hasSize(1);
        assertThat(reporte.resumen().totalEntradas()).isEqualByComparingTo("10.0000");
        assertThat(reporte.resumen().saldoFinalCantidad()).isEqualByComparingTo("10.0000");
        assertThat(reporte.resumen().valorSaldoFinal()).isEqualByComparingTo("20.0000000000");
    }

    @Test
    void kardexValorizadoResumenConRangoIncluyeSaldoAnteriorDeTodasLasLineas() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        UbicacionAlmacenEntity segundaUbicacion = crearUbicacion("U2" + data.suffix(), data.almacenOrigen());
        LocalDateTime base = LocalDateTime.now().withNano(0);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-KAR-ANT-U1-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("2.000000"),
                base.minusDays(2)
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                segundaUbicacion.getId(),
                "ENT-KAR-ANT-U2-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("3.000000"),
                base.minusDays(2)
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-KAR-RAN-U1-" + data.suffix(),
                new BigDecimal("2.0000"),
                new BigDecimal("2.000000"),
                base.plusHours(1)
        ), authentication());

        var reporte = reporteInventarioService.obtenerKardexValorizado(
                data.producto().getId(),
                data.almacenOrigen().getId(),
                null,
                base.minusDays(1),
                base.plusDays(1),
                true
        );

        assertThat(reporte.cabecera().saldoInicialCantidad()).isEqualByComparingTo("15.0000");
        assertThat(reporte.cabecera().saldoInicialValor()).isEqualByComparingTo("35.0000000000");
        assertThat(reporte.detalles()).hasSize(1);
        assertThat(reporte.resumen().totalEntradas()).isEqualByComparingTo("2.0000");
        assertThat(reporte.resumen().totalSalidas()).isEqualByComparingTo("0.0000");
        assertThat(reporte.resumen().saldoFinalCantidad()).isEqualByComparingTo("17.0000");
        assertThat(reporte.resumen().valorSaldoFinal()).isEqualByComparingTo("39.0000000000");
    }

    @Test
    void kardexValorizadoResumenConciliaConStockValorizado() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        UbicacionAlmacenEntity segundaUbicacion = crearUbicacion("U2" + data.suffix(), data.almacenOrigen());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                data.ubicacionOrigen().getId(),
                "ENT-KAR-CONC-U1-" + data.suffix(),
                new BigDecimal("7.0000"),
                new BigDecimal("2.000000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                segundaUbicacion.getId(),
                "ENT-KAR-CONC-U2-" + data.suffix(),
                new BigDecimal("3.0000"),
                new BigDecimal("4.000000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenDestino().getId(),
                null,
                "ENT-KAR-CONC-A2-" + data.suffix(),
                new BigDecimal("5.0000"),
                new BigDecimal("6.000000")
        ), authentication());

        var kardex = reporteInventarioService.obtenerKardexValorizado(
                data.producto().getId(),
                null,
                null,
                null,
                null,
                true
        );
        var stock = reporteInventarioService.obtenerStockValorizado(
                null,
                null,
                data.producto().getId(),
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                true,
                PageRequest.of(0, 10)
        );

        assertThat(kardex.resumen().saldoFinalCantidad()).isEqualByComparingTo(stock.resumen().cantidadUnidades());
        assertThat(kardex.resumen().valorSaldoFinal()).isEqualByComparingTo(stock.resumen().valorTotalInventario());
    }

    @Test
    void kardexValorizadoCalculaSaldoInicialAntesDelRango() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-KAR-INI-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("3.000000"),
                LocalDateTime.now().minusDays(2)
        ), authentication());
        var salida = movimientoInventarioService.crearMovimiento(salida(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data,
                true,
                "SAL-KAR-RANGO-" + data.suffix(),
                new BigDecimal("4.0000"),
                LocalDateTime.now()
        ), authentication());

        var reporte = reporteInventarioService.obtenerKardexValorizado(
                data.producto().getId(),
                data.almacenOrigen().getId(),
                null,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                true
        );

        assertThat(reporte.cabecera().saldoInicialCantidad()).isEqualByComparingTo("10.0000");
        assertThat(reporte.cabecera().saldoInicialCostoPromedio()).isEqualByComparingTo("3.000000");
        assertThat(reporte.detalles()).hasSize(1);
        assertThat(reporte.detalles().get(0).movimientoId()).isEqualTo(salida.id());
        assertThat(reporte.resumen().saldoFinalCantidad()).isEqualByComparingTo("6.0000");
        assertThat(reporte.resumen().valorSaldoFinal()).isEqualByComparingTo("18.0000000000");
    }

    @Test
    void salidasDetalleExcluyeBorradoresYAgrupaPorFamilia() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-SAL-DET-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("4.000000")
        ), authentication());
        var salidaConfirmada = movimientoInventarioService.crearMovimiento(salida(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data,
                true,
                "SAL-CONF-" + data.suffix(),
                new BigDecimal("3.0000")
        ), authentication());
        var salidaBorrador = movimientoInventarioService.crearMovimiento(salida(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data,
                false,
                "SAL-BOR-" + data.suffix(),
                new BigDecimal("2.0000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerSalidasDetalle(
                null,
                null,
                data.almacenOrigen().getId(),
                data.producto().getId(),
                data.producto().getFamilia().getId(),
                null,
                null,
                null,
                null,
                null,
                data.producto().getCodigo(),
                "familia",
                true,
                PageRequest.of(0, 10)
        );

        assertThat(reporte.registros().getContent()).anyMatch(r -> r.movimientoId().equals(salidaConfirmada.id()));
        assertThat(reporte.registros().getContent()).noneMatch(r -> r.movimientoId().equals(salidaBorrador.id()));
        assertThat(reporte.registros().getContent()).allSatisfy(r -> {
            assertThat(r.almacenOrigenCodigo()).isEqualTo(data.almacenOrigen().getCodigo());
            assertThat(r.unidadMedidaAbreviatura()).isEqualTo(data.producto().getUnidadMedida().getAbreviatura());
        });
        assertThat(reporte.resumen().cantidadTotalSalida()).isEqualByComparingTo("3.0000");
        assertThat(reporte.resumen().valorTotalSalida()).isEqualByComparingTo("12.0000000000");
        assertThat(reporte.agrupaciones()).hasSize(1);
        assertThat(reporte.agrupaciones().get(0).cantidadTotal()).isEqualByComparingTo("3.0000");
    }

    @Test
    void salidasValorizadasPorOsAgrupaYConservaSnapshotHistorico() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        data.producto().setCostoReferencial(new BigDecimal("12.7100"));
        productoRepository.save(data.producto());
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-OS-" + data.suffix(),
                new BigDecimal("20.0000"),
                new BigDecimal("5.000000")
        ), authentication());

        var primeraOs100 = movimientoInventarioService.crearMovimiento(salidaVentaOs(
                data,
                true,
                "OS-100",
                "SAL-OS-100-A-" + data.suffix(),
                new BigDecimal("2.0000"),
                new BigDecimal("35.00")
        ), authentication());
        data.producto().setCostoReferencial(new BigDecimal("15.0000"));
        productoRepository.save(data.producto());
        var segundaOs100 = movimientoInventarioService.crearMovimiento(salidaVentaOs(
                data,
                true,
                "OS-100",
                "SAL-OS-100-B-" + data.suffix(),
                new BigDecimal("1.0000"),
                new BigDecimal("25.00")
        ), authentication());
        movimientoInventarioService.crearMovimiento(salidaVentaOs(
                data,
                true,
                "OS-200",
                "SAL-OS-200-" + data.suffix(),
                new BigDecimal("1.0000"),
                new BigDecimal("25.00")
        ), authentication());
        var anulada = movimientoInventarioService.crearMovimiento(salidaVentaOs(
                data,
                true,
                "OS-100",
                "SAL-OS-100-ANU-" + data.suffix(),
                new BigDecimal("1.0000"),
                new BigDecimal("100.00")
        ), authentication());
        movimientoInventarioService.anularMovimiento(anulada.id(), "No procede para reporte");
        movimientoInventarioService.crearMovimiento(salida(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data,
                true,
                "SAL-NO-COM-" + data.suffix(),
                new BigDecimal("1.0000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerSalidasValorizadasPorOs(
                null,
                null,
                "OS-100",
                null,
                null,
                true,
                PageRequest.of(0, 10)
        );

        assertThat(reporte.registros().getContent())
                .extracting(r -> r.movimientoId())
                .containsExactlyInAnyOrder(primeraOs100.id(), segundaOs100.id());
        assertThat(reporte.registros().getContent())
                .extracting(r -> r.costoBaseComercialUnitarioSinIgv())
                .containsExactlyInAnyOrder(new BigDecimal("12.71"), new BigDecimal("15.00"));
        assertThat(reporte.resumenPorOs()).hasSize(1);
        var resumen = reporte.resumenPorOs().get(0);
        assertThat(resumen.ordenTrabajo()).isEqualTo("OS-100");
        assertThat(resumen.cantidadMovimientos()).isEqualTo(2);
        assertThat(resumen.cantidadItems()).isEqualTo(2);
        assertThat(resumen.costoTotalInventario()).isEqualByComparingTo("15.00");
        assertThat(resumen.valorComercialTotalSinIgv()).isEqualByComparingTo("53.07");
        assertThat(resumen.igvTotal()).isEqualByComparingTo("9.56");
        assertThat(resumen.precioTotalConIgv()).isEqualByComparingTo("62.63");
        assertThat(resumen.diferenciaComercialSinIgv()).isEqualByComparingTo("38.07");
    }

    @Test
    void entradasProveedorIncluyeDocumentoYGastosSeparados() {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        var entrada = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-PROV-DOC-" + data.suffix(),
                new BigDecimal("8.0000"),
                new BigDecimal("7.250000")
        ), authentication());

        var reporte = reporteInventarioService.obtenerEntradasProveedor(
                null,
                null,
                data.proveedor().getId(),
                data.producto().getId(),
                data.producto().getFamilia().getId(),
                data.almacenOrigen().getId(),
                "FACTURA",
                data.proveedor().getRazonSocial(),
                true,
                PageRequest.of(0, 10)
        );

        assertThat(reporte.registros().getContent()).hasSize(1);
        var item = reporte.registros().getContent().get(0);
        assertThat(item.movimientoId()).isEqualTo(entrada.id());
        assertThat(item.comprobanteTipo()).isEqualTo("FACTURA");
        assertThat(item.guiaSerie()).isEqualTo("T001");
        assertThat(item.almacenDestinoCodigo()).isEqualTo(data.almacenOrigen().getCodigo());
        assertThat(item.unidadMedidaAbreviatura()).isEqualTo(data.producto().getUnidadMedida().getAbreviatura());
        assertThat(item.valorLinea()).isEqualByComparingTo("58.0000000000");
        assertThat(reporte.resumen().valorProductos()).isEqualByComparingTo("58.0000000000");
        assertThat(reporte.resumen().totalDocumentario()).isEqualByComparingTo("58.0000000000");
    }

    @Test
    void pdfMovimientoSeGeneraComoDocumentoPdfConValoresDeDosDecimales() throws Exception {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        var entrada = movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-PDF-" + data.suffix(),
                new BigDecimal("2.0000"),
                new BigDecimal("9.000000")
        ), authentication());

        var archivo = movimientoPdfService.generarMovimientoPdf(entrada.id(), true, "admin");

        assertThat(archivo.filename()).contains("nota-de-ingreso");
        assertThat(archivo.content()).startsWith("%PDF".getBytes());
        assertThat(archivo.content().length).isGreaterThan(1000);
        try (PDDocument pdf = PDDocument.load(archivo.content())) {
            String texto = new PDFTextStripper().getText(pdf);
            assertThat(texto).contains("2.00", "9.00", "18.00");
        }
    }

    @Test
    void pdfDetalleSalidaUsaTablaCompactaYRedondeaMontosADosDecimales() throws Exception {
        TestData data = crearDatosBase(BigDecimal.ZERO);
        movimientoInventarioService.crearMovimiento(entradaCompra(
                data,
                true,
                data.almacenOrigen().getId(),
                null,
                "ENT-PDF-SAL-" + data.suffix(),
                new BigDecimal("10.0000"),
                new BigDecimal("4.123456")
        ), authentication());
        var salida = movimientoInventarioService.crearMovimiento(salida(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data,
                true,
                "SAL-PDF-" + data.suffix(),
                new BigDecimal("3.0000")
        ), authentication());

        var archivo = movimientoPdfService.generarMovimientoPdf(salida.id(), true, "admin");

        assertThat(archivo.filename()).contains("vale-de-salida");
        try (PDDocument pdf = PDDocument.load(archivo.content())) {
            assertThat(pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext()).isTrue();
            String texto = new PDFTextStripper().getText(pdf);
            assertThat(texto).contains(
                    "VALE DE SALIDA",
                    "COSTO UNIT. (S/)",
                    "VALOR TOTAL (S/)",
                    "3.00",
                    "4.12",
                    "12.37"
            );
            assertThat(texto).doesNotContain("4.123456", "12.370368");
        }
    }

    private MovimientoInventarioCreateRequest entradaCompra(
            TestData data,
            Boolean confirmar,
            Long almacenDestinoId,
            Long ubicacionDestinoId,
            String documento,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        return entradaCompra(data, confirmar, almacenDestinoId, ubicacionDestinoId, documento, cantidad, costoUnitario, LocalDateTime.now());
    }

    private MovimientoInventarioCreateRequest entradaCompra(
            TestData data,
            Boolean confirmar,
            Long almacenDestinoId,
            Long ubicacionDestinoId,
            String documento,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            LocalDateTime fechaMovimiento
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_COMPRA,
                fechaMovimiento,
                data.proveedor().getId(),
                null,
                null,
                almacenDestinoId,
                ubicacionDestinoId,
                documento,
                "Entrada compra",
                null,
                null,
                null,
                null,
                null,
                null,
                confirmar,
                "T001",
                "000001",
                LocalDate.now(),
                "FACTURA",
                "F001",
                "000001",
                LocalDate.now(),
                "OC-" + data.suffix(),
                null,
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Documento test",
                List.of(new MovimientoDetalleCreateRequest(
                        data.producto().getId(),
                        null,
                        ubicacionDestinoId,
                        cantidad,
                        costoUnitario,
                        "Detalle entrada"
                ))
        );
    }

    private MovimientoInventarioCreateRequest salida(
            TipoMovimientoInventario tipo,
            TestData data,
            Boolean confirmar,
            String documento,
            BigDecimal cantidad
    ) {
        return salida(tipo, data, confirmar, documento, cantidad, LocalDateTime.now());
    }

    private MovimientoInventarioCreateRequest salida(
            TipoMovimientoInventario tipo,
            TestData data,
            Boolean confirmar,
            String documento,
            BigDecimal cantidad,
            LocalDateTime fechaMovimiento
    ) {
        return new MovimientoInventarioCreateRequest(
                tipo,
                fechaMovimiento,
                null,
                data.almacenOrigen().getId(),
                null,
                null,
                null,
                documento,
                "Salida de prueba",
                tipo == TipoMovimientoInventario.SALIDA_VENTA ? null : "Motivo salida",
                tipo == TipoMovimientoInventario.SALIDA_VENTA ? "OS-" + data.suffix() : null,
                null,
                null,
                null,
                null,
                confirmar,
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
                data.personal().getId(),
                List.of(tipo == TipoMovimientoInventario.SALIDA_VENTA
                        ? new MovimientoDetalleCreateRequest(
                                data.producto().getId(),
                                null,
                                null,
                                cantidad,
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("25.00"),
                                "Detalle salida"
                        )
                        : new MovimientoDetalleCreateRequest(
                                data.producto().getId(),
                                null,
                                null,
                                cantidad,
                                null,
                                "Detalle salida"
                        ))
        );
    }

    private MovimientoInventarioCreateRequest salidaVentaOs(
            TestData data,
            Boolean confirmar,
            String ordenTrabajo,
            String documento,
            BigDecimal cantidad,
            BigDecimal recargoComercialPct
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.SALIDA_VENTA,
                LocalDateTime.now(),
                null,
                data.almacenOrigen().getId(),
                null,
                null,
                null,
                documento,
                "Salida venta por OS",
                null,
                ordenTrabajo,
                null,
                null,
                null,
                null,
                confirmar,
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
                List.of(new MovimientoDetalleCreateRequest(
                        data.producto().getId(),
                        null,
                        null,
                        cantidad,
                        null,
                        null,
                        null,
                        null,
                        recargoComercialPct,
                        "Detalle salida venta OS"
                ))
        );
    }

    private MovimientoInventarioCreateRequest transferencia(
            TestData data,
            Boolean confirmar,
            String documento,
            BigDecimal cantidad
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.TRANSFERENCIA,
                LocalDateTime.now(),
                null,
                data.almacenOrigen().getId(),
                null,
                data.almacenDestino().getId(),
                null,
                documento,
                "Transferencia",
                "Reposicion",
                null,
                null,
                null,
                "Entrega",
                "Recepcion",
                confirmar,
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
                List.of(new MovimientoDetalleCreateRequest(
                        data.producto().getId(),
                        null,
                        null,
                        cantidad,
                        null,
                        "Detalle transferencia"
                ))
        );
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private TestData crearDatosBase(BigDecimal stockMinimo) {
        String suffix = suffix();
        UsuarioEntity usuario = usuarioRepository.findByUsername("admin").orElseThrow();
        UnidadMedidaEntity unidad = crearUnidad(suffix);
        FamiliaEntity familia = crearFamilia(suffix);
        TipoArticuloEntity tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();
        ProductoEntity producto = crearProducto(suffix, unidad, familia, tipoArticulo, stockMinimo);
        ProveedorEntity proveedor = crearProveedor(suffix);
        PersonalEntity personal = crearPersonal(suffix);
        AlmacenEntity almacenOrigen = crearAlmacen("O" + suffix);
        AlmacenEntity almacenDestino = crearAlmacen("D" + suffix);
        UbicacionAlmacenEntity ubicacionOrigen = crearUbicacion("UO" + suffix, almacenOrigen);
        UbicacionAlmacenEntity ubicacionDestino = crearUbicacion("UD" + suffix, almacenDestino);
        return new TestData(suffix, usuario, producto, proveedor, personal, almacenOrigen, almacenDestino, ubicacionOrigen, ubicacionDestino);
    }

    private UnidadMedidaEntity crearUnidad(String suffix) {
        UnidadMedidaEntity unidad = new UnidadMedidaEntity();
        unidad.setCodigo("U" + suffix);
        unidad.setNombre("Unidad " + suffix);
        unidad.setAbreviatura("U" + suffix.substring(0, 4));
        unidad.setActivo(true);
        return unidadMedidaRepository.save(unidad);
    }

    private FamiliaEntity crearFamilia(String suffix) {
        FamiliaEntity familia = new FamiliaEntity();
        familia.setNombre("Familia " + suffix);
        familia.setPrefijo("FI" + suffix.substring(0, 8));
        familia.setActivo(true);
        return familiaRepository.save(familia);
    }

    private ProductoEntity crearProducto(
            String suffix,
            UnidadMedidaEntity unidad,
            FamiliaEntity familia,
            TipoArticuloEntity tipoArticulo,
            BigDecimal stockMinimo
    ) {
        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo("P" + suffix);
        producto.setNombre("Producto " + suffix);
        producto.setDescripcion("Producto para pruebas integrales");
        producto.setTipoArticulo(tipoArticulo);
        producto.setFamilia(familia);
        producto.setUnidadMedida(unidad);
        producto.setStockMinimo(stockMinimo);
        producto.setCostoReferencial(new BigDecimal("1.0000"));
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private ProveedorEntity crearProveedor(String suffix) {
        ProveedorEntity proveedor = new ProveedorEntity();
        proveedor.setTipoDocumento("RUC");
        proveedor.setNumeroDocumento("20" + suffix.substring(0, 8));
        proveedor.setRazonSocial("Proveedor " + suffix);
        proveedor.setNombreComercial("Comercial " + suffix);
        proveedor.setActivo(true);
        return proveedorRepository.save(proveedor);
    }

    private PersonalEntity crearPersonal(String suffix) {
        PersonalEntity personal = new PersonalEntity();
        personal.setTipoDocumento("DNI");
        personal.setNumeroDocumento("7" + suffix.substring(0, 8));
        personal.setNombres("Personal " + suffix);
        personal.setApellidos("Salida");
        personal.setCargo("Tecnico");
        personal.setArea("Operaciones");
        personal.setActivo(true);
        return personalRepository.save(personal);
    }

    private AlmacenEntity crearAlmacen(String codigo) {
        AlmacenEntity almacen = new AlmacenEntity();
        almacen.setCodigo(codigo);
        almacen.setNombre("Almacen " + codigo);
        almacen.setActivo(true);
        return almacenRepository.save(almacen);
    }

    private UbicacionAlmacenEntity crearUbicacion(String codigo, AlmacenEntity almacen) {
        UbicacionAlmacenEntity ubicacion = new UbicacionAlmacenEntity();
        ubicacion.setAlmacen(almacen);
        ubicacion.setCodigo(codigo);
        ubicacion.setNombre("Ubicacion " + codigo);
        ubicacion.setActivo(true);
        return ubicacionAlmacenRepository.save(ubicacion);
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private record TestData(
            String suffix,
            UsuarioEntity usuario,
            ProductoEntity producto,
            ProveedorEntity proveedor,
            PersonalEntity personal,
            AlmacenEntity almacenOrigen,
            AlmacenEntity almacenDestino,
            UbicacionAlmacenEntity ubicacionOrigen,
            UbicacionAlmacenEntity ubicacionDestino
    ) {
    }
}
