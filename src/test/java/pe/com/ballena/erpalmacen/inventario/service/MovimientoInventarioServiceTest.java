package pe.com.ballena.erpalmacen.inventario.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.inventario.kardex.repository.KardexRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.DevolucionProveedorCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.DevolucionProveedorDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoCancelacionRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoDetalleRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.stock.repository.StockActualRepository;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.productos.service.ProductoService;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.maestros.proveedores.repository.ProveedorRepository;
import pe.com.ballena.erpalmacen.maestros.proveedores.service.ProveedorService;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MovimientoInventarioServiceTest {

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private MovimientoDetalleRepository movimientoDetalleRepository;

    @Autowired
    private StockActualRepository stockActualRepository;

    @Autowired
    private KardexRepository kardexRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private TipoArticuloRepository tipoArticuloRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void crearMovimientosGeneraCodigoConPrefijoYCorrelativo() {
        TestData data = crearDatosBase();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var primero = movimientoInventarioService.crearMovimiento(crearEntradaAjusteRequest(
                data,
                false,
                "DOC-CORR-1-" + suffix,
                new BigDecimal("1.0000"),
                new BigDecimal("2.000000")
        ), authentication());
        var segundo = movimientoInventarioService.crearMovimiento(crearEntradaAjusteRequest(
                data,
                false,
                "DOC-CORR-2-" + suffix,
                new BigDecimal("1.0000"),
                new BigDecimal("2.000000")
        ), authentication());

        assertThat(primero.numero()).matches("MOV-[0-9]{6,}");
        assertThat(segundo.numero()).matches("MOV-[0-9]{6,}");
        assertThat(correlativoMovimiento(segundo.numero())).isEqualTo(correlativoMovimiento(primero.numero()) + 1);
    }

    @Test
    void crearDevolucionesProveedorGeneraCorrelativoDevIndependiente() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "DOC-CORR-DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());
        Long detalleOrigenId = entrada.detalles().get(0).id();

        var primera = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigenId,
                data.producto().getId(),
                new BigDecimal("1.0000"),
                false
        ), authentication());
        var segunda = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigenId,
                data.producto().getId(),
                new BigDecimal("1.0000"),
                false
        ), authentication());

        assertThat(primera.numero()).matches("DEV-[0-9]{6,}");
        assertThat(segunda.numero()).matches("DEV-[0-9]{6,}");
        assertThat(correlativoMovimiento(segunda.numero())).isEqualTo(correlativoMovimiento(primera.numero()) + 1);
    }

    @Test
    void crearMovimientoCreaBorradorSinAfectarStockNiKardex() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearEntradaAjusteRequest(
                data,
                false,
                "DOC-BORRADOR",
                new BigDecimal("5.0000"),
                new BigDecimal("10.0000")
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication);

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.BORRADOR);
        assertThat(response.detalles()).hasSize(1);
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        )).isEmpty();
        assertThat(kardexRepository.findByMovimientoId(response.id())).isEmpty();
    }

    @Test
    void crearEntradaCompraBorradorConDocumentosNoAfectaStockNiKardex() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearEntradaCompraRequest(
                data,
                false,
                "DOC-COMPRA-BORRADOR",
                new BigDecimal("7.0000"),
                new BigDecimal("15.987654")
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.BORRADOR);
        assertThat(response.proveedorId()).isEqualTo(data.proveedor().getId());
        assertThat(response.guiaSerie()).isEqualTo("T001");
        assertThat(response.guiaNumero()).isEqualTo("000123");
        assertThat(response.comprobanteTipo()).isEqualTo("FACTURA");
        assertThat(response.comprobanteSerie()).isEqualTo("F001");
        assertThat(response.comprobanteNumero()).isEqualTo("000456");
        assertThat(response.ordenCompraNumero()).isEqualTo("OC-0001");
        assertThat(response.flete()).isEqualByComparingTo("12.345678");
        assertThat(response.movilidad()).isEqualByComparingTo("3.210000");
        assertThat(response.otrosGastos()).isEqualByComparingTo("1.000001");
        assertThat(response.observacionDocumentaria()).isEqualTo("Documentos de prueba");
        assertThat(response.detalles().get(0).costoUnitario()).isEqualByComparingTo("15.987654");
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        )).isEmpty();
        assertThat(kardexRepository.findByMovimientoId(response.id())).isEmpty();
    }

    @Test
    void crearEntradaCompraConfirmadaDirectaAumentaStockYGeneraKardexConSeisDecimales() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearEntradaCompraRequest(
                data,
                true,
                "DOC-COMPRA-CONFIRMADA",
                new BigDecimal("10.0000"),
                new BigDecimal("0.123456")
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        assertThat(response.confirmadoEn()).isNotNull();
        assertThat(response.detalles().get(0).costoUnitario()).isEqualByComparingTo("0.123456");
        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("0.123456");
        assertThat(kardexRepository.findByMovimientoId(response.id())).hasSize(1);
    }

    @Test
    void entradaCompraConIgvRecuperableNoIncluyeIgvEnStockNiKardex() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearEntradaCompraConIgvRequest(
                data,
                true,
                "DOC-IGV-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                true,
                new BigDecimal("118.000000")
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        var detalle = response.detalles().get(0);
        assertThat(detalle.valorUnitarioSinIgv()).isEqualByComparingTo("100.00");
        assertThat(detalle.tasaIgv()).isEqualByComparingTo("18.00");
        assertThat(detalle.igvRecuperable()).isTrue();
        assertThat(detalle.montoIgvUnitario()).isEqualByComparingTo("18.000000");
        assertThat(detalle.precioUnitarioConIgv()).isEqualByComparingTo("118.000000");
        assertThat(detalle.costoUnitario()).isEqualByComparingTo("100.00");
        assertThat(detalle.costoUnitarioInventariable()).isEqualByComparingTo("100.00");
        assertThat(detalle.totalInventariable()).isEqualByComparingTo("1000.000000");
        assertThat(detalle.totalConIgv()).isEqualByComparingTo("1180.000000");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("100.000000");
        assertThat(kardexRepository.findByMovimientoId(response.id()).get(0).getCostoUnitario())
                .isEqualByComparingTo("100.000000");
    }

    @Test
    void entradaCompraConIgvNoRecuperableIncluyeIgvEnCostoInventariable() {
        TestData data = crearDatosBase();

        var response = movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "DOC-IGV-NO-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                false,
                null
        ), authentication());

        var detalle = response.detalles().get(0);
        assertThat(detalle.costoUnitarioInventariable()).isEqualByComparingTo("118.000000");
        assertThat(detalle.totalInventariable()).isEqualByComparingTo("1180.000000");
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow().getCostoPromedio()).isEqualByComparingTo("118.000000");
    }

    @Test
    void costoPromedioUsaValorSinIgvCuandoIgvEsRecuperable() {
        TestData data = crearDatosBase();
        movimientoInventarioService.crearMovimiento(crearEntradaAjusteRequest(
                data,
                true,
                "STOCK-INICIAL-IGV-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("100.000000")
        ), authentication());

        movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "PROMEDIO-IGV-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("120.00"),
                new BigDecimal("18.00"),
                true,
                null
        ), authentication());

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("20.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("110.000000");
    }

    @Test
    void costoPromedioIncluyeIgvCuandoNoEsRecuperable() {
        TestData data = crearDatosBase();
        movimientoInventarioService.crearMovimiento(crearEntradaAjusteRequest(
                data,
                true,
                "STOCK-INICIAL-IGV-NO-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("100.000000")
        ), authentication());

        movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "PROMEDIO-IGV-NO-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("120.00"),
                new BigDecimal("18.00"),
                false,
                null
        ), authentication());

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("20.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("120.800000");
    }

    @Test
    void variasEntradasConIgvRecuperableNoValorizanElIgv() {
        TestData data = crearDatosBase();
        movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "VARIAS-IGV-1",
                new BigDecimal("10.0000"),
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                true,
                null
        ), authentication());
        movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "VARIAS-IGV-2",
                new BigDecimal("5.0000"),
                new BigDecimal("120.00"),
                new BigDecimal("18.00"),
                true,
                null
        ), authentication());
        movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "VARIAS-IGV-3",
                new BigDecimal("20.0000"),
                new BigDecimal("90.00"),
                new BigDecimal("18.00"),
                true,
                null
        ), authentication());

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("35.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("97.142857");
    }

    @Test
    void stockInicialUsaCostoDirectoInventariableSinCalcularIgv() {
        TestData data = crearDatosBase();

        var response = movimientoInventarioService.crearMovimiento(crearEntradaAjusteRequest(
                data,
                true,
                "STOCK-INICIAL-DIRECTO",
                new BigDecimal("100.0000"),
                new BigDecimal("25.500000")
        ), authentication());

        var detalle = response.detalles().get(0);
        assertThat(detalle.valorUnitarioSinIgv()).isNull();
        assertThat(detalle.tasaIgv()).isNull();
        assertThat(detalle.igvRecuperable()).isNull();
        assertThat(detalle.costoUnitarioInventariable()).isEqualByComparingTo("25.500000");
        assertThat(detalle.totalInventariable()).isEqualByComparingTo("2550.000000");
        assertThat(detalle.totalConIgv()).isNull();
        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("100.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("25.500000");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void guardarYConfirmarSalidaSinStockNoDejaMovimientoCreadoAMedias() {
        TestData data = crearDatosBase();
        String documentoReferencia = "SALIDA-SIN-STOCK-" + UUID.randomUUID();
        MovimientoInventarioCreateRequest request = crearSalidaConsumoRequest(
                data,
                true,
                documentoReferencia,
                new BigDecimal("5.0000")
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No existe stock");

        boolean existeMovimiento = movimientoInventarioRepository.findAll()
                .stream()
                .anyMatch(movimiento -> documentoReferencia.equals(movimiento.getDocumentoReferencia()));
        assertThat(existeMovimiento).isFalse();
    }

    @Test
    void crearSalidaConsumoConfirmadaDirectaDisminuyeStockYGeneraKardex() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("10.0000"));
        MovimientoInventarioCreateRequest request = crearSalidaConsumoRequest(
                data,
                true,
                "SALIDA-DIRECTA",
                new BigDecimal("4.0000")
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("6.0000");
        assertThat(kardexRepository.findByMovimientoId(response.id())).hasSize(1);
    }

    @Test
    void crearTransferenciaConfirmadaDirectaMueveStockEntreAlmacenes() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("10.0000"));
        MovimientoInventarioCreateRequest request = crearTransferenciaRequest(
                data,
                true,
                "TRANSFERENCIA-DIRECTA",
                new BigDecimal("3.0000")
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        var stockOrigen = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        var stockDestino = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stockOrigen.getCantidadActual()).isEqualByComparingTo("7.0000");
        assertThat(stockDestino.getCantidadActual()).isEqualByComparingTo("3.0000");
        assertThat(kardexRepository.findByMovimientoId(response.id())).hasSize(2);
    }

    @Test
    void entradaCompraSinProveedorFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest baseRequest = crearEntradaCompraRequest(
                data,
                false,
                "DOC-SIN-PROVEEDOR",
                new BigDecimal("1.0000"),
                new BigDecimal("1.000000")
        );
        MovimientoInventarioCreateRequest request = new MovimientoInventarioCreateRequest(
                baseRequest.tipoMovimiento(),
                baseRequest.fechaMovimiento(),
                null,
                baseRequest.almacenOrigenId(),
                baseRequest.ubicacionOrigenId(),
                baseRequest.almacenDestinoId(),
                baseRequest.ubicacionDestinoId(),
                baseRequest.documentoReferencia(),
                baseRequest.observacion(),
                baseRequest.motivoMovimiento(),
                baseRequest.ordenTrabajo(),
                baseRequest.areaSolicitante(),
                baseRequest.solicitante(),
                baseRequest.responsableEntrega(),
                baseRequest.responsableRecepcion(),
                baseRequest.confirmar(),
                baseRequest.guiaSerie(),
                baseRequest.guiaNumero(),
                baseRequest.guiaFecha(),
                baseRequest.comprobanteTipo(),
                baseRequest.comprobanteSerie(),
                baseRequest.comprobanteNumero(),
                baseRequest.comprobanteFechaEmision(),
                baseRequest.ordenCompraNumero(),
                baseRequest.tipoDocumento(),
                baseRequest.serieDocumento(),
                baseRequest.numeroDocumento(),
                baseRequest.ordenCompra(),
                baseRequest.fechaPedido(),
                baseRequest.fechaRecepcion(),
                baseRequest.flete(),
                baseRequest.movilidad(),
                baseRequest.otrosGastos(),
                baseRequest.observacionDocumentaria(),
                baseRequest.detalles()
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("proveedor");
    }

    @Test
    void entradaCompraSinFechaRecepcionFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearEntradaCompraRequestConDocumentos(
                data,
                false,
                "DOC-SIN-RECEPCION",
                null,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fecha de recepcion");
    }

    @Test
    void entradaCompraSinAlmacenDestinoFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.ENTRADA_COMPRA,
                data,
                false,
                data.proveedor().getId(),
                null,
                null,
                "Entrada compra",
                null,
                new BigDecimal("1.0000"),
                new BigDecimal("1.000000")
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("almacen destino");
    }

    @Test
    void entradaCompraSinDetalleFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_COMPRA,
                LocalDateTime.now(),
                data.proveedor().getId(),
                null,
                null,
                data.almacenDestino().getId(),
                null,
                "DOC-SIN-DETALLE",
                "Entrada compra sin detalle",
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "FACTURA",
                "F001",
                "000001",
                LocalDate.now(),
                "OC-SIN-DETALLE",
                null,
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                List.of()
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("detalle");
    }

    @Test
    void entradaCompraSinCostoUnitarioFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearEntradaCompraRequest(
                data,
                false,
                "DOC-SIN-COSTO",
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("costo unitario");
    }

    @Test
    void entradaCompraSinCostoPeroConValorSinIgvEsValidaYCalculaCosto() {
        TestData data = crearDatosBase();

        var response = movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "DOC-SIN-COSTO-CON-VALOR-IGV",
                new BigDecimal("10.0000"),
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                null,
                null
        ), authentication());

        assertThat(response.detalles().get(0).igvRecuperable()).isTrue();
        assertThat(response.detalles().get(0).costoUnitarioInventariable()).isEqualByComparingTo("100.00");
    }

    @Test
    void entradaConValorSinIgvOTasaNegativaFalla() {
        TestData data = crearDatosBase();

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "DOC-VALOR-NEGATIVO",
                new BigDecimal("1.0000"),
                new BigDecimal("-1.00"),
                BigDecimal.ZERO,
                true,
                null
        ), authentication())).isInstanceOf(BusinessException.class)
                .hasMessageContaining("valor unitario");

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "DOC-TASA-NEGATIVA",
                new BigDecimal("1.0000"),
                BigDecimal.ONE,
                new BigDecimal("-18.00"),
                true,
                null
        ), authentication())).isInstanceOf(BusinessException.class)
                .hasMessageContaining("tasa IGV");
    }

    @Test
    void entradaAjusteSinMotivoFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                data,
                false,
                null,
                null,
                data.almacenDestino().getId(),
                "Observacion requerida",
                null,
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void entradaAjusteSinObservacionFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                data,
                false,
                null,
                null,
                data.almacenDestino().getId(),
                null,
                "Regularizacion",
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("observacion");
    }

    @Test
    void salidaConsumoSinObservacionFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data,
                false,
                null,
                data.almacenOrigen().getId(),
                null,
                null,
                "Consumo interno",
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("observacion");
    }

    @Test
    void salidaAjusteSinMotivoFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.SALIDA_AJUSTE,
                data,
                false,
                null,
                data.almacenOrigen().getId(),
                null,
                "Observacion requerida",
                null,
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void salidaVentaSinAlmacenOrigenFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.SALIDA_VENTA,
                data,
                false,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("almacen origen");
    }

    @Test
    void salidaAjusteConfirmadaDisminuyeStockYGeneraKardex() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("9.0000"));
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.SALIDA_AJUSTE,
                data,
                true,
                null,
                data.almacenOrigen().getId(),
                null,
                "Salida ajuste validada",
                "Regularizacion negativa",
                new BigDecimal("4.0000"),
                null
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("5.0000");
        assertThat(kardexRepository.findByMovimientoId(response.id())).hasSize(1);
    }

    @Test
    void ajustePositivoSinObservacionFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.AJUSTE_POSITIVO,
                data,
                false,
                null,
                null,
                data.almacenDestino().getId(),
                null,
                "Sobrante",
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("observacion");
    }

    @Test
    void ajusteNegativoSinMotivoFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.AJUSTE_NEGATIVO,
                data,
                false,
                null,
                data.almacenOrigen().getId(),
                null,
                "Ajuste negativo",
                null,
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void transferenciaConMismoAlmacenFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.TRANSFERENCIA,
                data,
                false,
                null,
                data.almacenOrigen().getId(),
                data.almacenOrigen().getId(),
                null,
                null,
                new BigDecimal("1.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("origen no puede ser igual");
    }

    @Test
    void ajustePositivoConfirmadoAumentaStock() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.AJUSTE_POSITIVO,
                data,
                true,
                null,
                null,
                data.almacenDestino().getId(),
                "Ajuste positivo validado",
                "Conteo fisico",
                new BigDecimal("5.0000"),
                new BigDecimal("2.000000")
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("5.0000");
    }

    @Test
    void ajusteNegativoConfirmadoDisminuyeStock() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("8.0000"));
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.AJUSTE_NEGATIVO,
                data,
                true,
                null,
                data.almacenOrigen().getId(),
                null,
                "Ajuste negativo validado",
                "Merma",
                new BigDecimal("3.0000"),
                null
        );

        var response = movimientoInventarioService.crearMovimiento(request, authentication());

        assertThat(response.estado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("5.0000");
    }

    @Test
    void ajusteNegativoSinStockFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearRequestOperativo(
                TipoMovimientoInventario.AJUSTE_NEGATIVO,
                data,
                true,
                null,
                data.almacenOrigen().getId(),
                null,
                "Ajuste negativo sin stock",
                "Merma",
                new BigDecimal("3.0000"),
                null
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(request, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No existe stock");
    }

    @Test
    void validacionesDocumentariasLanzanBusinessException() {
        TestData data = crearDatosBase();

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequestConDocumentos(
                        data,
                        false,
                        "DOC-FLETE-NEGATIVO",
                        "T001",
                        "000123",
                        "FACTURA",
                        "F001",
                        "000456",
                        LocalDate.now(),
                        LocalDate.now().minusDays(1),
                        new BigDecimal("-1.000000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                authentication()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("flete");

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequestConDocumentos(
                        data,
                        false,
                        "DOC-COMP-INVALIDO",
                        "T001",
                        "000123",
                        "RECIBO",
                        "F001",
                        "000456",
                        LocalDate.now(),
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                authentication()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("tipo de comprobante");

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequestConDocumentos(
                        data,
                        false,
                        "DOC-GUIA-INCOMPLETA",
                        "T001",
                        null,
                        "FACTURA",
                        "F001",
                        "000456",
                        LocalDate.now(),
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                authentication()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("guia");

        assertThatThrownBy(() -> movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequestConDocumentos(
                        data,
                        false,
                        "DOC-COMP-INCOMPLETO",
                        "T001",
                        "000123",
                        "FACTURA",
                        null,
                        "000456",
                        LocalDate.now(),
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                authentication()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("comprobante");
    }

    @Test
    void listarMovimientosAplicaOrdenDescendentePorDefecto() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest primerRequest = crearEntradaCompraRequest(
                data,
                false,
                "ORDEN-PRIMERO",
                new BigDecimal("1.0000"),
                new BigDecimal("1.000000")
        );
        MovimientoInventarioCreateRequest segundoRequest = crearEntradaCompraRequest(
                data,
                false,
                "ORDEN-SEGUNDO",
                new BigDecimal("1.0000"),
                new BigDecimal("1.000000")
        );

        var primero = movimientoInventarioService.crearMovimiento(primerRequest, authentication());
        var segundo = movimientoInventarioService.crearMovimiento(segundoRequest, authentication());

        var page = movimientoInventarioService.listar(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).id()).isEqualTo(segundo.id());
        assertThat(segundo.id()).isGreaterThan(primero.id());

        var movimientosPorCodigoProducto = movimientoInventarioService.listar(
                null,
                null,
                null,
                null,
                data.producto().getCodigo(),
                PageRequest.of(0, 10)
        );
        assertThat(movimientosPorCodigoProducto.getContent())
                .anyMatch(movimiento -> movimiento.id().equals(primero.id()));
    }

    @Test
    void consultarMovimientoHistoricoSinCamposIgvNoGeneraNullPointerYCostoSeMantiene() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity movimiento = crearMovimiento(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                data.almacenOrigen(),
                data.usuario()
        );
        MovimientoDetalleEntity detalle = crearDetalle(
                movimiento,
                data.producto(),
                new BigDecimal("2.0000"),
                null,
                null,
                new BigDecimal("7.500000")
        );

        var response = movimientoInventarioService.obtener(movimiento.getId());

        assertThat(response.detalles()).hasSize(1);
        var detalleResponse = response.detalles().get(0);
        assertThat(detalleResponse.id()).isEqualTo(detalle.getId());
        assertThat(detalleResponse.valorUnitarioSinIgv()).isNull();
        assertThat(detalleResponse.tasaIgv()).isNull();
        assertThat(detalleResponse.igvRecuperable()).isNull();
        assertThat(detalleResponse.costoUnitario()).isEqualByComparingTo("7.500000");
        assertThat(detalleResponse.costoUnitarioInventariable()).isEqualByComparingTo("7.500000");
    }

    @Test
    void buscarProductosYProveedoresPorTextoFuncionaParaCombos() {
        TestData data = crearDatosBase();
        data.producto().setDescripcion("Descripcion especial autocomplete " + data.producto().getCodigo());
        productoRepository.save(data.producto());

        var productos = productoService.listar(
                data.producto().getCodigo(),
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        var proveedores = proveedorService.listar(
                data.proveedor().getNumeroDocumento(),
                null,
                PageRequest.of(0, 10)
        );

        assertThat(productos.getContent())
                .anySatisfy(producto -> assertThat(producto.id()).isEqualTo(data.producto().getId()));
        assertThat(proveedores.getContent())
                .anySatisfy(proveedor -> assertThat(proveedor.id()).isEqualTo(data.proveedor().getId()));
    }

    @Test
    void confirmarEntradaAumentaStockYGeneraKardex() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity movimiento = crearMovimiento(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                data.almacenOrigen(),
                data.usuario()
        );
        crearDetalle(movimiento, data.producto(), new BigDecimal("10.0000"), null, null, new BigDecimal("5.0000"));

        movimientoInventarioService.confirmarMovimiento(movimiento.getId());

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("5.0000");
        assertThat(kardexRepository.findByMovimientoId(movimiento.getId())).hasSize(1);
        assertThat(movimiento.getEstado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
    }

    @Test
    void confirmarSalidaConStockSuficienteDescuentaStockYGeneraKardex() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("10.0000"));
        MovimientoInventarioEntity salida = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenOrigen(),
                null,
                data.usuario()
        );
        crearDetalle(salida, data.producto(), new BigDecimal("4.0000"), null, null, null);

        movimientoInventarioService.confirmarMovimiento(salida.getId());

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("6.0000");
        assertThat(kardexRepository.findByMovimientoId(salida.getId())).hasSize(1);
    }

    @Test
    void confirmarSalidaSinStockSuficienteLanzaBusinessException() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity salida = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenOrigen(),
                null,
                data.usuario()
        );
        crearDetalle(salida, data.producto(), new BigDecimal("4.0000"), null, null, null);

        assertThatThrownBy(() -> movimientoInventarioService.confirmarMovimiento(salida.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No existe stock");
    }

    @Test
    void confirmarTransferenciaMueveStockEntreAlmacenes() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("10.0000"));
        MovimientoInventarioEntity transferencia = crearMovimiento(
                TipoMovimientoInventario.TRANSFERENCIA,
                data.almacenOrigen(),
                data.almacenDestino(),
                data.usuario()
        );
        crearDetalle(transferencia, data.producto(), new BigDecimal("3.0000"), null, null, null);

        movimientoInventarioService.confirmarMovimiento(transferencia.getId());

        var stockOrigen = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        var stockDestino = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();

        assertThat(stockOrigen.getCantidadActual()).isEqualByComparingTo("7.0000");
        assertThat(stockDestino.getCantidadActual()).isEqualByComparingTo("3.0000");
        assertThat(kardexRepository.findByMovimientoId(transferencia.getId())).hasSize(2);
    }

    @Test
    void confirmarMovimientoDosVecesLanzaBusinessException() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity movimiento = confirmarEntrada(data, new BigDecimal("10.0000"));

        assertThatThrownBy(() -> movimientoInventarioService.confirmarMovimiento(movimiento.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue confirmado");
    }

    @Test
    void cancelarMovimientoBorradorCambiaEstadoSinAfectarStockNiKardex() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = crearEntradaCompraRequest(
                data,
                false,
                "DOC-CANCELAR-BORRADOR",
                new BigDecimal("5.0000"),
                new BigDecimal("2.000000")
        );
        var borrador = movimientoInventarioService.crearMovimiento(request, authentication());

        var cancelado = movimientoInventarioService.cancelarMovimiento(
                borrador.id(),
                new MovimientoCancelacionRequest("Error de digitacion"),
                authentication()
        );

        assertThat(cancelado.estado()).isEqualTo(EstadoMovimientoInventario.CANCELADO);
        assertThat(cancelado.motivoCancelacion()).isEqualTo("Error de digitacion");
        assertThat(cancelado.canceladoPor()).isEqualTo("admin");
        assertThat(cancelado.canceladoEn()).isNotNull();
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        )).isEmpty();
        assertThat(kardexRepository.findByMovimientoId(borrador.id())).isEmpty();
    }

    @Test
    void cancelarSinMotivoFalla() {
        TestData data = crearDatosBase();
        var borrador = movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequest(data, false, "DOC-CANCELAR-SIN-MOTIVO", new BigDecimal("1.0000"), new BigDecimal("1.000000")),
                authentication()
        );

        assertThatThrownBy(() -> movimientoInventarioService.cancelarMovimiento(
                borrador.id(),
                new MovimientoCancelacionRequest("   "),
                authentication()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo de cancelacion");
    }

    @Test
    void cancelarMovimientoConfirmadoFallaYNoCambiaStock() {
        TestData data = crearDatosBase();
        var confirmado = movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequest(data, true, "DOC-CANCELAR-CONFIRMADO", new BigDecimal("5.0000"), new BigDecimal("2.000000")),
                authentication()
        );
        var stockAntes = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow().getCantidadActual();

        assertThatThrownBy(() -> movimientoInventarioService.cancelarMovimiento(
                confirmado.id(),
                new MovimientoCancelacionRequest("No procede"),
                authentication()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Solo se pueden cancelar movimientos en estado BORRADOR");

        var stockDespues = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow().getCantidadActual();
        assertThat(stockDespues).isEqualByComparingTo(stockAntes);
    }

    @Test
    void cancelarMovimientoAnuladoFalla() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = confirmarEntrada(data, new BigDecimal("5.0000"));
        movimientoInventarioService.anularMovimiento(entrada.getId(), "Error operativo");

        assertThatThrownBy(() -> movimientoInventarioService.cancelarMovimiento(
                entrada.getId(),
                new MovimientoCancelacionRequest("No procede"),
                authentication()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Solo se pueden cancelar movimientos en estado BORRADOR");
    }

    @Test
    void confirmarMovimientoCanceladoFallaSinStockNiKardex() {
        TestData data = crearDatosBase();
        var borrador = movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequest(data, false, "DOC-CONFIRMAR-CANCELADO", new BigDecimal("5.0000"), new BigDecimal("2.000000")),
                authentication()
        );
        movimientoInventarioService.cancelarMovimiento(
                borrador.id(),
                new MovimientoCancelacionRequest("Cancelar antes de confirmar"),
                authentication()
        );

        assertThatThrownBy(() -> movimientoInventarioService.confirmarMovimiento(borrador.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede confirmar un movimiento cancelado");
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        )).isEmpty();
        assertThat(kardexRepository.findByMovimientoId(borrador.id())).isEmpty();
    }

    @Test
    void anularMovimientoCanceladoFalla() {
        TestData data = crearDatosBase();
        var borrador = movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequest(data, false, "DOC-ANULAR-CANCELADO", new BigDecimal("5.0000"), new BigDecimal("2.000000")),
                authentication()
        );
        movimientoInventarioService.cancelarMovimiento(
                borrador.id(),
                new MovimientoCancelacionRequest("Cancelar antes de anular"),
                authentication()
        );

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(borrador.id(), "No procede"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede anular un movimiento cancelado");
    }

    @Test
    void listarYFiltrarMovimientosCanceladosFunciona() {
        TestData data = crearDatosBase();
        var borrador = movimientoInventarioService.crearMovimiento(
                crearEntradaCompraRequest(data, false, "DOC-LISTAR-CANCELADO", new BigDecimal("1.0000"), new BigDecimal("1.000000")),
                authentication()
        );
        movimientoInventarioService.cancelarMovimiento(
                borrador.id(),
                new MovimientoCancelacionRequest("Cancelar para listado"),
                authentication()
        );

        var listado = movimientoInventarioService.listar(
                null,
                EstadoMovimientoInventario.CANCELADO,
                null,
                null,
                "DOC-LISTAR-CANCELADO",
                PageRequest.of(0, 10)
        );

        assertThat(listado.getContent()).anyMatch(m -> m.id().equals(borrador.id())
                && m.estado() == EstadoMovimientoInventario.CANCELADO);
    }

    @Test
    void anularEntradaConfirmadaReduceStockYGeneraKardexReverso() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = confirmarEntrada(data, new BigDecimal("10.0000"));

        movimientoInventarioService.anularMovimiento(entrada.getId(), "Error en entrada");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("0.0000");
        assertThat(stock.getCostoPromedio()).isNull();
        assertThat(kardexRepository.findByMovimientoId(entrada.getId())).hasSize(2);
        assertThat(entrada.getEstado()).isEqualTo(EstadoMovimientoInventario.ANULADO);
        assertThat(entrada.getAnuladoEn()).isNotNull();
    }

    @Test
    void anularEntradaSinMovimientosPosterioresRestauraCostoPromedioAnterior() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("10.0000"), new BigDecimal("3.000000"));
        MovimientoInventarioEntity entradaPosteriorSinUso = confirmarEntrada(
                data,
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        );

        var stockAntes = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stockAntes.getCantidadActual()).isEqualByComparingTo("20.0000");
        assertThat(stockAntes.getCostoPromedio()).isEqualByComparingTo("4.000000");

        movimientoInventarioService.anularMovimiento(entradaPosteriorSinUso.getId(), "Error en costo de entrada");

        var stockDespues = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stockDespues.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stockDespues.getCostoPromedio()).isEqualByComparingTo("3.000000");
        assertThat(kardexRepository.findByMovimientoId(entradaPosteriorSinUso.getId())).hasSize(2);
    }

    @Test
    void anularEntradaConIgvRecuperableRestauraCostoPromedioAnterior() {
        TestData data = crearDatosBase();
        movimientoInventarioService.crearMovimiento(crearEntradaAjusteRequest(
                data,
                true,
                "ANULAR-IGV-RECUPERABLE-INICIAL",
                new BigDecimal("10.0000"),
                new BigDecimal("100.000000")
        ), authentication());
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "ANULAR-IGV-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("120.00"),
                new BigDecimal("18.00"),
                true,
                null
        ), authentication());

        movimientoInventarioService.anularMovimiento(entrada.id(), "Anular entrada con IGV recuperable");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("100.000000");
        assertThat(kardexRepository.findByMovimientoId(entrada.id())).hasSize(2);
    }

    @Test
    void anularEntradaConIgvNoRecuperableUsaCostoInventariableOriginal() {
        TestData data = crearDatosBase();
        movimientoInventarioService.crearMovimiento(crearEntradaAjusteRequest(
                data,
                true,
                "ANULAR-IGV-NO-RECUPERABLE-INICIAL",
                new BigDecimal("10.0000"),
                new BigDecimal("100.000000")
        ), authentication());
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraConIgvRequest(
                data,
                true,
                "ANULAR-IGV-NO-RECUPERABLE",
                new BigDecimal("10.0000"),
                new BigDecimal("120.00"),
                new BigDecimal("18.00"),
                false,
                null
        ), authentication());
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow().getCostoPromedio()).isEqualByComparingTo("120.800000");

        movimientoInventarioService.anularMovimiento(entrada.id(), "Anular entrada con IGV no recuperable");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("100.000000");
        assertThat(kardexRepository.findByMovimientoId(entrada.id()))
                .anySatisfy(kardex -> assertThat(kardex.getCostoUnitario()).isEqualByComparingTo("141.600000"));
    }

    @Test
    void anularPrimeraEntradaDeProductoDejaStockEnCeroSinCostoResidual() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = confirmarEntrada(data, new BigDecimal("8.0000"), new BigDecimal("9.876543"));

        movimientoInventarioService.anularMovimiento(entrada.getId(), "Primera entrada incorrecta");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("0.0000");
        assertThat(stock.getCostoPromedio()).isNull();
        assertThat(kardexRepository.findByMovimientoId(entrada.getId()))
                .extracting(kardex -> kardex.getObservacion())
                .anyMatch(observacion -> observacion != null && observacion.contains("ANULACION MOVIMIENTO"));
    }

    @Test
    void anularSalidaConfirmadaAumentaStockYGeneraKardexReverso() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("10.0000"));
        MovimientoInventarioEntity salida = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenOrigen(),
                null,
                data.usuario()
        );
        crearDetalle(salida, data.producto(), new BigDecimal("4.0000"), null, null, null);
        movimientoInventarioService.confirmarMovimiento(salida.getId());

        movimientoInventarioService.anularMovimiento(salida.getId(), "Error en salida");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(kardexRepository.findByMovimientoId(salida.getId())).hasSize(2);
        assertThat(salida.getEstado()).isEqualTo(EstadoMovimientoInventario.ANULADO);
    }

    @Test
    void anularTransferenciaRevierteOrigenYDestino() {
        TestData data = crearDatosBase();
        confirmarEntrada(data, new BigDecimal("10.0000"));
        MovimientoInventarioEntity transferencia = crearMovimiento(
                TipoMovimientoInventario.TRANSFERENCIA,
                data.almacenOrigen(),
                data.almacenDestino(),
                data.usuario()
        );
        crearDetalle(transferencia, data.producto(), new BigDecimal("3.0000"), null, null, null);
        movimientoInventarioService.confirmarMovimiento(transferencia.getId());

        movimientoInventarioService.anularMovimiento(transferencia.getId(), "Error en transferencia");

        var stockOrigen = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        var stockDestino = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();

        assertThat(stockOrigen.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stockDestino.getCantidadActual()).isEqualByComparingTo("0.0000");
        assertThat(kardexRepository.findByMovimientoId(transferencia.getId())).hasSize(4);
        assertThat(transferencia.getEstado()).isEqualTo(EstadoMovimientoInventario.ANULADO);
    }

    @Test
    void anularMovimientoBorradorLanzaBusinessException() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity movimiento = crearMovimiento(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                data.almacenOrigen(),
                data.usuario()
        );
        crearDetalle(movimiento, data.producto(), new BigDecimal("4.0000"), null, null, new BigDecimal("5.0000"));

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(movimiento.getId(), "Error"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BORRADOR");
    }

    @Test
    void anularMovimientoYaAnuladoLanzaBusinessException() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = confirmarEntrada(data, new BigDecimal("10.0000"));
        movimientoInventarioService.anularMovimiento(entrada.getId(), "Primera anulacion");

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(entrada.getId(), "Segunda anulacion"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue anulado");
    }

    @Test
    void anularEntradaConSalidaPosteriorLanzaBusinessException() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = confirmarEntrada(data, new BigDecimal("10.0000"));
        MovimientoInventarioEntity salida = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenOrigen(),
                null,
                data.usuario()
        );
        crearDetalle(salida, data.producto(), new BigDecimal("10.0000"), null, null, null);
        movimientoInventarioService.confirmarMovimiento(salida.getId());

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(entrada.getId(), "Sin stock para reversa"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("movimientos posteriores");
    }

    @Test
    void anularEntradaConOtraEntradaPosteriorLanzaBusinessException() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity primeraEntrada = confirmarEntrada(data, new BigDecimal("10.0000"), new BigDecimal("3.000000"));
        confirmarEntrada(data, new BigDecimal("5.0000"), new BigDecimal("4.000000"));

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(primeraEntrada.getId(), "Entrada historica"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("movimientos posteriores");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("15.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("3.333333");
    }

    @Test
    void anularEntradaConTransferenciaPosteriorLanzaBusinessException() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = confirmarEntrada(data, new BigDecimal("10.0000"), new BigDecimal("3.000000"));
        MovimientoInventarioEntity transferencia = crearMovimiento(
                TipoMovimientoInventario.TRANSFERENCIA,
                data.almacenOrigen(),
                data.almacenDestino(),
                data.usuario()
        );
        crearDetalle(transferencia, data.producto(), new BigDecimal("2.0000"), null, null, null);
        movimientoInventarioService.confirmarMovimiento(transferencia.getId());

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(entrada.getId(), "Entrada historica"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("movimientos posteriores");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void anularEntradaMultilineaConPosteriorEnUnaLineaRechazaTodoDocumento() {
        TestData data = crearDatosBase();
        ProductoEntity segundoProducto = crearProducto(
                data.producto().getUnidadMedida(),
                data.producto().getFamilia(),
                data.producto().getTipoArticulo()
        );
        MovimientoInventarioEntity entrada = crearMovimiento(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                data.almacenOrigen(),
                data.usuario()
        );
        crearDetalle(entrada, data.producto(), new BigDecimal("10.0000"), null, null, new BigDecimal("3.000000"));
        crearDetalle(entrada, segundoProducto, new BigDecimal("7.0000"), null, null, new BigDecimal("2.000000"));
        movimientoInventarioService.confirmarMovimiento(entrada.getId());

        MovimientoInventarioEntity salidaPosterior = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenOrigen(),
                null,
                data.usuario()
        );
        crearDetalle(salidaPosterior, segundoProducto, new BigDecimal("1.0000"), null, null, null);
        movimientoInventarioService.confirmarMovimiento(salidaPosterior.getId());

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(entrada.getId(), "Una linea ya fue usada"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("movimientos posteriores");

        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow().getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                segundoProducto.getId(),
                data.almacenOrigen().getId()
        ).orElseThrow().getCantidadActual()).isEqualByComparingTo("6.0000");
    }

    @Test
    void crearDevolucionProveedorBorradorNoAfectaStockNiKardex() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-BORRADOR",
                new BigDecimal("20.0000"),
                new BigDecimal("8.000000")
        ), authentication());
        var detalleOrigen = entrada.detalles().get(0);

        var devolucion = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigen.id(),
                data.producto().getId(),
                new BigDecimal("5.0000"),
                false
        ), authentication());

        assertThat(devolucion.estado()).isEqualTo(EstadoMovimientoInventario.BORRADOR);
        assertThat(devolucion.tipoMovimiento()).isEqualTo(TipoMovimientoInventario.DEVOLUCION_PROVEEDOR);
        assertThat(devolucion.numero()).startsWith("DEV-");
        assertThat(devolucion.movimientoOrigenId()).isEqualTo(entrada.id());
        assertThat(devolucion.proveedorId()).isEqualTo(data.proveedor().getId());
        assertThat(devolucion.almacenOrigenId()).isEqualTo(data.almacenDestino().getId());
        assertThat(devolucion.detalles().get(0).movimientoDetalleOrigenId()).isEqualTo(detalleOrigen.id());
        assertThat(devolucion.detalles().get(0).costoUnitario()).isNull();
        assertThat(devolucion.detalles().get(0).costoDocumentoUnitario()).isNull();
        assertThat(kardexRepository.findByMovimientoId(devolucion.id())).isEmpty();
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow().getCantidadActual()).isEqualByComparingTo("20.0000");
    }

    @Test
    void crearDevolucionProveedorSinFechaMovimientoFalla() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-SIN-FECHA",
                new BigDecimal("10.0000"),
                new BigDecimal("8.000000")
        ), authentication());
        var detalleOrigen = entrada.detalles().get(0);
        var requestBase = crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigen.id(),
                data.producto().getId(),
                new BigDecimal("2.0000"),
                false
        );
        var requestSinFecha = new DevolucionProveedorCreateRequest(
                requestBase.movimientoOrigenId(),
                requestBase.proveedorId(),
                null,
                requestBase.fechaDocumento(),
                requestBase.numeroNotaCredito(),
                requestBase.motivo(),
                requestBase.observacion(),
                requestBase.confirmar(),
                requestBase.detalles()
        );

        assertThatThrownBy(() -> movimientoInventarioService.crearDevolucionProveedor(requestSinFecha, authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("La fecha del movimiento es obligatoria");
    }

    @Test
    void confirmarDevolucionProveedorParcialReduceStockGeneraKardexYConservaCostoDocumental() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-VAL",
                new BigDecimal("20.0000"),
                new BigDecimal("20.000000")
        ), authentication());
        movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-PROM",
                new BigDecimal("10.0000"),
                new BigDecimal("10.000000")
        ), authentication());
        var detalleOrigen = entrada.detalles().get(0);

        var devolucion = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigen.id(),
                data.producto().getId(),
                new BigDecimal("4.0000"),
                true
        ), authentication());

        assertThat(devolucion.estado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        assertThat(devolucion.confirmadoEn()).isNotNull();
        var detalle = devolucion.detalles().get(0);
        assertThat(detalle.costoUnitario()).isEqualByComparingTo("16.666667");
        assertThat(detalle.totalLinea()).isEqualByComparingTo("66.666668");
        assertThat(detalle.costoDocumentoUnitario()).isEqualByComparingTo("20.000000");
        assertThat(detalle.valorStock()).isEqualByComparingTo("66.666668");
        assertThat(detalle.valorDocumento()).isEqualByComparingTo("80.000000");
        assertThat(detalle.diferenciaValorizacion()).isEqualByComparingTo("13.333332");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("26.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("16.666667");
        assertThat(kardexRepository.findByMovimientoId(devolucion.id()))
                .hasSize(1)
                .allSatisfy(kardex -> {
                    assertThat(kardex.getTipoMovimiento()).isEqualTo(TipoMovimientoInventario.DEVOLUCION_PROVEEDOR);
                    assertThat(kardex.getEntrada()).isEqualByComparingTo("0.0000");
                    assertThat(kardex.getSalida()).isEqualByComparingTo("4.0000");
                    assertThat(kardex.getCostoUnitario()).isEqualByComparingTo("16.666667");
                });
        assertThat(movimientoInventarioRepository.findById(entrada.id()).orElseThrow().getEstado())
                .isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
    }

    @Test
    void confirmarDevolucionProveedorTotalDejaStockEnCeroYCostoPromedioNull() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-TOTAL",
                new BigDecimal("5.0000"),
                new BigDecimal("11.000000")
        ), authentication());
        var detalleOrigen = entrada.detalles().get(0);

        movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigen.id(),
                data.producto().getId(),
                new BigDecimal("5.0000"),
                true
        ), authentication());

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("0.0000");
        assertThat(stock.getCostoPromedio()).isNull();
    }

    @Test
    void crearDevolucionProveedorRechazaProductoQueNoPerteneceAlDetalleOrigen() {
        TestData data = crearDatosBase();
        ProductoEntity otroProducto = crearProducto(
                data.producto().getUnidadMedida(),
                data.producto().getFamilia(),
                data.producto().getTipoArticulo()
        );
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-PROD-AJENO",
                new BigDecimal("5.0000"),
                new BigDecimal("9.000000")
        ), authentication());

        assertThatThrownBy(() -> movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                entrada.detalles().get(0).id(),
                otroProducto.getId(),
                new BigDecimal("1.0000"),
                false
        ), authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("producto");
    }

    @Test
    void cantidadesDevolucionSoloConsideranConfirmadas() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-PEND",
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());
        Long detalleOrigenId = entrada.detalles().get(0).id();

        var borrador = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigenId,
                data.producto().getId(),
                new BigDecimal("7.0000"),
                false
        ), authentication());
        assertThat(movimientoInventarioService.obtenerCantidadesDevolucion(entrada.id()).get(0).cantidadPendienteDevolver())
                .isEqualByComparingTo("10.0000");

        movimientoInventarioService.cancelarMovimiento(
                borrador.id(),
                new MovimientoCancelacionRequest("No se enviara al proveedor"),
                authentication()
        );
        assertThat(movimientoInventarioService.obtenerCantidadesDevolucion(entrada.id()).get(0).cantidadPendienteDevolver())
                .isEqualByComparingTo("10.0000");

        var confirmada = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigenId,
                data.producto().getId(),
                new BigDecimal("4.0000"),
                true
        ), authentication());
        assertThat(movimientoInventarioService.obtenerCantidadesDevolucion(entrada.id()).get(0).cantidadPendienteDevolver())
                .isEqualByComparingTo("6.0000");

        movimientoInventarioService.anularMovimiento(confirmada.id(), "Devolucion anulada");
        assertThat(movimientoInventarioService.obtenerCantidadesDevolucion(entrada.id()).get(0).cantidadPendienteDevolver())
                .isEqualByComparingTo("10.0000");
    }

    @Test
    void confirmarDevolucionProveedorRechazaCantidadMayorAPendiente() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-EXCESO",
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());
        Long detalleOrigenId = entrada.detalles().get(0).id();
        movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigenId,
                data.producto().getId(),
                new BigDecimal("4.0000"),
                true
        ), authentication());

        assertThatThrownBy(() -> movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                detalleOrigenId,
                data.producto().getId(),
                new BigDecimal("7.0000"),
                true
        ), authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pendiente");
    }

    @Test
    void confirmarDevolucionProveedorRechazaCantidadMayorAlStockFisico() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-STOCK",
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());
        MovimientoInventarioEntity salidaPosterior = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenDestino(),
                null,
                data.usuario()
        );
        crearDetalle(salidaPosterior, data.producto(), new BigDecimal("6.0000"), null, null, null);
        movimientoInventarioService.confirmarMovimiento(salidaPosterior.getId());

        assertThatThrownBy(() -> movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                entrada.detalles().get(0).id(),
                data.producto().getId(),
                new BigDecimal("5.0000"),
                true
        ), authentication()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void anularDevolucionProveedorSinPosterioresRestauraStockYCosto() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-ANULAR",
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());
        var devolucion = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                entrada.detalles().get(0).id(),
                data.producto().getId(),
                new BigDecimal("4.0000"),
                true
        ), authentication());

        movimientoInventarioService.anularMovimiento(devolucion.id(), "Error en devolucion");

        MovimientoInventarioEntity anulada = movimientoInventarioRepository.findById(devolucion.id()).orElseThrow();
        assertThat(anulada.getEstado()).isEqualTo(EstadoMovimientoInventario.ANULADO);
        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenDestino().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("10.0000");
        assertThat(stock.getCostoPromedio()).isEqualByComparingTo("5.000000");
        assertThat(kardexRepository.findByMovimientoId(devolucion.id())).hasSize(2);
    }

    @Test
    void anularDevolucionProveedorConPosteriorFalla() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-POSTERIOR",
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());
        var devolucion = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                entrada.detalles().get(0).id(),
                data.producto().getId(),
                new BigDecimal("2.0000"),
                true
        ), authentication());
        MovimientoInventarioEntity salidaPosterior = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenDestino(),
                null,
                data.usuario()
        );
        crearDetalle(salidaPosterior, data.producto(), new BigDecimal("1.0000"), null, null, null);
        movimientoInventarioService.confirmarMovimiento(salidaPosterior.getId());

        assertThatThrownBy(() -> movimientoInventarioService.anularMovimiento(devolucion.id(), "No procede"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("movimientos posteriores");
    }

    @Test
    void listarDevolucionesPorEntradaDevuelveHistorialRelacionado() {
        TestData data = crearDatosBase();
        var entrada = movimientoInventarioService.crearMovimiento(crearEntradaCompraRequest(
                data,
                true,
                "ENT-DEV-HIST",
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        ), authentication());
        var devolucion = movimientoInventarioService.crearDevolucionProveedor(crearDevolucionProveedorRequest(
                entrada.id(),
                entrada.detalles().get(0).id(),
                data.producto().getId(),
                new BigDecimal("1.0000"),
                false
        ), authentication());

        assertThat(movimientoInventarioService.listarDevolucionesPorEntrada(entrada.id()))
                .extracting("id")
                .contains(devolucion.id());
    }

    private MovimientoInventarioCreateRequest crearEntradaCompraRequest(
            TestData data,
            Boolean confirmar,
            String documentoReferencia,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        return crearEntradaCompraRequestConDocumentos(
                data,
                confirmar,
                documentoReferencia,
                "T001",
                "000123",
                "FACTURA",
                "F001",
                "000456",
                LocalDate.now().minusDays(2),
                LocalDate.now(),
                new BigDecimal("12.345678"),
                new BigDecimal("3.210000"),
                new BigDecimal("1.000001"),
                cantidad,
                costoUnitario
        );
    }

    private MovimientoInventarioCreateRequest crearEntradaCompraRequestConDocumentos(
            TestData data,
            Boolean confirmar,
            String documentoReferencia,
            String guiaSerie,
            String guiaNumero,
            String comprobanteTipo,
            String comprobanteSerie,
            String comprobanteNumero,
            LocalDate fechaPedido,
            LocalDate fechaRecepcion,
            BigDecimal flete,
            BigDecimal movilidad,
            BigDecimal otrosGastos
    ) {
        return crearEntradaCompraRequestConDocumentos(
                data,
                confirmar,
                documentoReferencia,
                guiaSerie,
                guiaNumero,
                comprobanteTipo,
                comprobanteSerie,
                comprobanteNumero,
                fechaPedido,
                fechaRecepcion,
                flete,
                movilidad,
                otrosGastos,
                new BigDecimal("7.0000"),
                new BigDecimal("15.987654")
        );
    }

    private MovimientoInventarioCreateRequest crearEntradaCompraRequestConDocumentos(
            TestData data,
            Boolean confirmar,
            String documentoReferencia,
            String guiaSerie,
            String guiaNumero,
            String comprobanteTipo,
            String comprobanteSerie,
            String comprobanteNumero,
            LocalDate fechaPedido,
            LocalDate fechaRecepcion,
            BigDecimal flete,
            BigDecimal movilidad,
            BigDecimal otrosGastos,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_COMPRA,
                LocalDateTime.now(),
                data.proveedor().getId(),
                null,
                null,
                data.almacenDestino().getId(),
                null,
                documentoReferencia,
                "Entrada compra de prueba",
                null,
                null,
                null,
                null,
                null,
                null,
                confirmar,
                guiaSerie,
                guiaNumero,
                LocalDate.now(),
                comprobanteTipo,
                comprobanteSerie,
                comprobanteNumero,
                LocalDate.now(),
                "OC-0001",
                null,
                null,
                null,
                null,
                fechaPedido,
                fechaRecepcion,
                flete,
                movilidad,
                otrosGastos,
                "Documentos de prueba",
                List.of(new MovimientoDetalleCreateRequest(
                        data.producto().getId(),
                        null,
                        null,
                        cantidad,
                        costoUnitario,
                        "Detalle entrada compra"
                ))
        );
    }

    private MovimientoInventarioCreateRequest crearEntradaCompraConIgvRequest(
            TestData data,
            Boolean confirmar,
            String documentoReferencia,
            BigDecimal cantidad,
            BigDecimal valorUnitarioSinIgv,
            BigDecimal tasaIgv,
            Boolean igvRecuperable,
            BigDecimal costoUnitarioDirecto
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_COMPRA,
                LocalDateTime.now(),
                data.proveedor().getId(),
                null,
                null,
                data.almacenDestino().getId(),
                null,
                documentoReferencia,
                "Entrada compra con IGV",
                null,
                null,
                null,
                null,
                null,
                null,
                confirmar,
                "T001",
                "000123",
                LocalDate.now(),
                "FACTURA",
                "F001",
                "000456",
                LocalDate.now(),
                "OC-IGV",
                null,
                null,
                null,
                null,
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Documentos de prueba IGV",
                List.of(new MovimientoDetalleCreateRequest(
                        data.producto().getId(),
                        null,
                        null,
                        cantidad,
                        costoUnitarioDirecto,
                        valorUnitarioSinIgv,
                        tasaIgv,
                        igvRecuperable,
                        "Detalle entrada compra IGV"
                ))
        );
    }

    private MovimientoInventarioCreateRequest crearSalidaConsumoRequest(
            TestData data,
            Boolean confirmar,
            String documentoReferencia,
            BigDecimal cantidad
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                LocalDateTime.now(),
                null,
                data.almacenOrigen().getId(),
                null,
                null,
                null,
                documentoReferencia,
                "Salida consumo de prueba",
                "Consumo interno",
                "OT-001",
                "Mantenimiento",
                "Usuario solicitante",
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
                        "Detalle salida"
                ))
        );
    }

    private MovimientoInventarioCreateRequest crearEntradaAjusteRequest(
            TestData data,
            Boolean confirmar,
            String documentoReferencia,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                LocalDateTime.now(),
                null,
                null,
                null,
                data.almacenDestino().getId(),
                null,
                documentoReferencia,
                "Entrada ajuste de prueba",
                "Ajuste positivo por validacion",
                null,
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
                        costoUnitario,
                        "Detalle entrada ajuste"
                ))
        );
    }

    private MovimientoInventarioCreateRequest crearRequestOperativo(
            TipoMovimientoInventario tipoMovimiento,
            TestData data,
            Boolean confirmar,
            Long proveedorId,
            Long almacenOrigenId,
            Long almacenDestinoId,
            String observacion,
            String motivoMovimiento,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        return new MovimientoInventarioCreateRequest(
                tipoMovimiento,
                LocalDateTime.now(),
                proveedorId,
                almacenOrigenId,
                null,
                almacenDestinoId,
                null,
                "DOC-" + UUID.randomUUID(),
                observacion,
                motivoMovimiento,
                null,
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
                        costoUnitario,
                        "Detalle operativo"
                ))
        );
    }

    private MovimientoInventarioCreateRequest crearTransferenciaRequest(
            TestData data,
            Boolean confirmar,
            String documentoReferencia,
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
                documentoReferencia,
                "Transferencia de prueba",
                "Reposicion interna",
                null,
                null,
                null,
                "Responsable origen",
                "Responsable destino",
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

    private DevolucionProveedorCreateRequest crearDevolucionProveedorRequest(
            Long entradaOrigenId,
            Long detalleOrigenId,
            Long productoId,
            BigDecimal cantidad,
            Boolean confirmar
    ) {
        return new DevolucionProveedorCreateRequest(
                entradaOrigenId,
                null,
                LocalDateTime.now(),
                LocalDate.now(),
                "NC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Devolucion por control de calidad",
                "Devolucion proveedor de prueba",
                confirmar,
                List.of(new DevolucionProveedorDetalleCreateRequest(
                        detalleOrigenId,
                        productoId,
                        cantidad,
                        "Detalle devolucion"
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

    private long correlativoMovimiento(String numero) {
        return Long.parseLong(numero.substring(numero.indexOf('-') + 1));
    }

    private MovimientoInventarioEntity confirmarEntrada(TestData data, BigDecimal cantidad) {
        return confirmarEntrada(data, cantidad, new BigDecimal("5.0000"));
    }

    private MovimientoInventarioEntity confirmarEntrada(
            TestData data,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        MovimientoInventarioEntity entrada = crearMovimiento(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                data.almacenOrigen(),
                data.usuario()
        );
        crearDetalle(entrada, data.producto(), cantidad, null, null, costoUnitario);
        movimientoInventarioService.confirmarMovimiento(entrada.getId());
        return entrada;
    }

    private MovimientoInventarioEntity crearMovimiento(
            TipoMovimientoInventario tipoMovimiento,
            AlmacenEntity almacenOrigen,
            AlmacenEntity almacenDestino,
            UsuarioEntity usuario
    ) {
        MovimientoInventarioEntity movimiento = new MovimientoInventarioEntity();
        movimiento.setNumero("MOV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setEstado(EstadoMovimientoInventario.BORRADOR);
        movimiento.setAlmacenOrigen(almacenOrigen);
        movimiento.setAlmacenDestino(almacenDestino);
        movimiento.setUsuario(usuario);
        movimiento.setObservacion("Movimiento de prueba");
        if (tipoMovimiento == TipoMovimientoInventario.ENTRADA_AJUSTE
                || tipoMovimiento == TipoMovimientoInventario.SALIDA_CONSUMO
                || tipoMovimiento == TipoMovimientoInventario.SALIDA_AJUSTE
                || tipoMovimiento == TipoMovimientoInventario.AJUSTE_POSITIVO
                || tipoMovimiento == TipoMovimientoInventario.AJUSTE_NEGATIVO) {
            movimiento.setMotivoMovimiento("Motivo de prueba");
        }
        return movimientoInventarioRepository.save(movimiento);
    }

    private MovimientoDetalleEntity crearDetalle(
            MovimientoInventarioEntity movimiento,
            ProductoEntity producto,
            BigDecimal cantidad,
            pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity ubicacionOrigen,
            pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity ubicacionDestino,
            BigDecimal costoUnitario
    ) {
        MovimientoDetalleEntity detalle = new MovimientoDetalleEntity();
        detalle.setMovimiento(movimiento);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setUbicacionOrigen(ubicacionOrigen);
        detalle.setUbicacionDestino(ubicacionDestino);
        detalle.setCostoUnitario(costoUnitario);
        return movimientoDetalleRepository.save(detalle);
    }

    private TestData crearDatosBase() {
        UsuarioEntity usuario = usuarioRepository.findByUsername("admin").orElseThrow();
        UnidadMedidaEntity unidad = crearUnidadMedida();
        FamiliaEntity familia = crearFamilia();
        TipoArticuloEntity tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();
        ProductoEntity producto = crearProducto(unidad, familia, tipoArticulo);
        ProveedorEntity proveedor = crearProveedor();
        AlmacenEntity almacenOrigen = crearAlmacen();
        AlmacenEntity almacenDestino = crearAlmacen();
        return new TestData(usuario, producto, proveedor, almacenOrigen, almacenDestino);
    }

    private UnidadMedidaEntity crearUnidadMedida() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UnidadMedidaEntity unidad = new UnidadMedidaEntity();
        unidad.setCodigo("U" + suffix);
        unidad.setNombre("Unidad Test " + suffix);
        unidad.setAbreviatura("UT" + suffix.substring(0, 4));
        unidad.setActivo(true);
        return unidadMedidaRepository.save(unidad);
    }

    private FamiliaEntity crearFamilia() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        FamiliaEntity familia = new FamiliaEntity();
        familia.setNombre("Familia Test " + suffix);
        familia.setPrefijo("FT" + suffix.substring(0, 8));
        familia.setActivo(true);
        return familiaRepository.save(familia);
    }

    private ProductoEntity crearProducto(
            UnidadMedidaEntity unidad,
            FamiliaEntity familia,
            TipoArticuloEntity tipoArticulo
    ) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo("P" + suffix);
        producto.setNombre("Producto Test " + suffix);
        producto.setTipoArticulo(tipoArticulo);
        producto.setFamilia(familia);
        producto.setUnidadMedida(unidad);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private ProveedorEntity crearProveedor() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProveedorEntity proveedor = new ProveedorEntity();
        proveedor.setTipoDocumento("RUC");
        proveedor.setNumeroDocumento("20" + suffix.substring(0, 8));
        proveedor.setRazonSocial("Proveedor Test " + suffix);
        proveedor.setNombreComercial("Proveedor Comercial " + suffix);
        proveedor.setActivo(true);
        return proveedorRepository.save(proveedor);
    }

    private AlmacenEntity crearAlmacen() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        AlmacenEntity almacen = new AlmacenEntity();
        almacen.setCodigo("A" + suffix);
        almacen.setNombre("Almacen Test " + suffix);
        almacen.setActivo(true);
        return almacenRepository.save(almacen);
    }

    private record TestData(
            UsuarioEntity usuario,
            ProductoEntity producto,
            ProveedorEntity proveedor,
            AlmacenEntity almacenOrigen,
            AlmacenEntity almacenDestino
    ) {
    }
}
