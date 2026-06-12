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
    }

    @Test
    void buscarProductosYProveedoresPorTextoFuncionaParaCombos() {
        TestData data = crearDatosBase();
        data.producto().setDescripcion("Descripcion especial autocomplete " + data.producto().getCodigo());
        productoRepository.save(data.producto());

        var productos = productoService.listar(
                "autocomplete",
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        var proveedores = proveedorService.listar(
                data.proveedor().getRazonSocial().substring(0, 12),
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
    void anularEntradaConfirmadaReduceStockYGeneraKardexReverso() {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = confirmarEntrada(data, new BigDecimal("10.0000"));

        movimientoInventarioService.anularMovimiento(entrada.getId(), "Error en entrada");

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow();
        assertThat(stock.getCantidadActual()).isEqualByComparingTo("0.0000");
        assertThat(kardexRepository.findByMovimientoId(entrada.getId())).hasSize(2);
        assertThat(entrada.getEstado()).isEqualTo(EstadoMovimientoInventario.ANULADO);
        assertThat(entrada.getAnuladoEn()).isNotNull();
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
    void anularEntradaSinStockSuficienteParaRevertirLanzaBusinessException() {
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
                .hasMessageContaining("Stock insuficiente");
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

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private MovimientoInventarioEntity confirmarEntrada(TestData data, BigDecimal cantidad) {
        MovimientoInventarioEntity entrada = crearMovimiento(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                data.almacenOrigen(),
                data.usuario()
        );
        crearDetalle(entrada, data.producto(), cantidad, null, null, new BigDecimal("5.0000"));
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
