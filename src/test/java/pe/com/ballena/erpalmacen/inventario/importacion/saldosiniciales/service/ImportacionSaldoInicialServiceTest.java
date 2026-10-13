package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.repository.UbicacionAlmacenRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity.SaldoInicialImportacionEntity;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository.SaldoInicialImportacionErrorRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository.SaldoInicialImportacionRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository.SaldoInicialImportacionTrazaRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.shared.EstadoImportacionSaldoInicial;
import pe.com.ballena.erpalmacen.inventario.kardex.repository.KardexRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoDetalleRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.stock.repository.StockActualRepository;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ImportacionSaldoInicialServiceTest {

    private static final LocalDateTime FECHA_MOVIMIENTO = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired
    private ImportacionSaldoInicialService importacionSaldoInicialService;

    @Autowired
    private SaldoInicialImportacionRepository importacionRepository;

    @Autowired
    private SaldoInicialImportacionErrorRepository errorRepository;

    @Autowired
    private SaldoInicialImportacionTrazaRepository trazaRepository;

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
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private TipoArticuloRepository tipoArticuloRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private UbicacionAlmacenRepository ubicacionAlmacenRepository;

    @Test
    void procesarLoteConfirmadoGeneraMovimientoStockKardexYTrazabilidad() {
        TestData data = crearDatosBase();
        String lote = "MIG-SI-" + suffix();
        String codigoMovimientoExterno = "SI-" + suffix();

        SaldoInicialImportacionEntity fila1 = crearFila(
                lote,
                codigoMovimientoExterno,
                "1",
                data.producto1().getCodigo(),
                data.almacen().getCodigo(),
                data.ubicacion().getCodigo(),
                new BigDecimal("100.0000"),
                new BigDecimal("12.500000")
        );
        SaldoInicialImportacionEntity fila2 = crearFila(
                lote,
                codigoMovimientoExterno,
                "2",
                data.producto2().getCodigo(),
                data.almacen().getCodigo(),
                data.ubicacion().getCodigo(),
                new BigDecimal("25.0000"),
                new BigDecimal("8.750000")
        );

        var resultado = importacionSaldoInicialService.procesar(lote, authentication());

        assertThat(resultado.estado())
                .as("errores=%s", errorRepository.findByLoteMigracionOrderByIdAsc(lote)
                        .stream()
                        .map(error -> error.getCodigoError() + ":" + error.getMensaje())
                        .toList())
                .isEqualTo("PROCESADO");
        assertThat(resultado.movimientosProcesadosEnEjecucion()).isEqualTo(1);
        assertThat(resultado.lineasProcesadas()).isEqualTo(2);
        assertThat(resultado.errores()).isZero();

        List<SaldoInicialImportacionEntity> filas = importacionRepository
                .findByLoteMigracionOrderByCodigoMovimientoExternoAscIdAsc(lote);
        assertThat(filas).extracting(SaldoInicialImportacionEntity::getEstado)
                .containsOnly(EstadoImportacionSaldoInicial.PROCESADO);
        assertThat(filas).allSatisfy(fila -> {
            assertThat(fila.getMovimientoId()).isNotNull();
            assertThat(fila.getMovimientoDetalleId()).isNotNull();
            assertThat(fila.getNumeroMovimiento()).startsWith("MOV-");
            assertThat(fila.getProcesadoEn()).isNotNull();
        });

        Long movimientoId = filas.get(0).getMovimientoId();
        var movimiento = movimientoInventarioRepository.findById(movimientoId).orElseThrow();
        assertThat(movimiento.getTipoMovimiento()).isEqualTo(TipoMovimientoInventario.ENTRADA_AJUSTE);
        assertThat(movimiento.getEstado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        assertThat(movimiento.getDocumentoReferencia()).isEqualTo(codigoMovimientoExterno);
        assertThat(movimiento.getObservacionDocumentaria()).contains(lote, codigoMovimientoExterno);
        assertThat(movimientoDetalleRepository.findByMovimientoIdOrderByIdAsc(movimientoId)).hasSize(2);
        assertThat(kardexRepository.findByMovimientoId(movimientoId)).hasSize(2);

        var stock1 = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionId(
                data.producto1().getId(),
                data.almacen().getId(),
                data.ubicacion().getId()
        ).orElseThrow();
        assertThat(stock1.getCantidadActual()).isEqualByComparingTo("100.0000");
        assertThat(stock1.getCostoPromedio()).isEqualByComparingTo("12.500000");

        var stock2 = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionId(
                data.producto2().getId(),
                data.almacen().getId(),
                data.ubicacion().getId()
        ).orElseThrow();
        assertThat(stock2.getCantidadActual()).isEqualByComparingTo("25.0000");
        assertThat(stock2.getCostoPromedio()).isEqualByComparingTo("8.750000");

        var trazas = trazaRepository.findByLoteMigracionOrderByIdAsc(lote);
        assertThat(trazas).hasSize(2);
        assertThat(trazas).allSatisfy(traza -> {
            assertThat(traza.getMovimiento().getId()).isEqualTo(movimientoId);
            assertThat(traza.getNumeroMovimiento()).isEqualTo(movimiento.getNumero());
            assertThat(traza.getStaging().getId()).isIn(fila1.getId(), fila2.getId());
        });
    }

    @Test
    void validarLoteConErroresNoCreaMovimientosNiTrazas() {
        TestData data = crearDatosBase();
        String lote = "MIG-ERR-" + suffix();
        crearFila(
                lote,
                "SI-ERR-" + suffix(),
                "1",
                "PRODUCTO-INEXISTENTE",
                data.almacen().getCodigo(),
                data.ubicacion().getCodigo(),
                new BigDecimal("10.0000"),
                new BigDecimal("5.000000")
        );

        var validacion = importacionSaldoInicialService.validar(lote, authentication());
        var proceso = importacionSaldoInicialService.procesar(lote, authentication());

        assertThat(validacion.estado()).isEqualTo("CON_ERRORES");
        assertThat(validacion.lineasConError()).isEqualTo(1);
        assertThat(errorRepository.findByLoteMigracionOrderByIdAsc(lote))
                .extracting(error -> error.getCodigoError())
                .contains("PRODUCTO_INVALIDO");
        assertThat(proceso.movimientosProcesadosEnEjecucion()).isZero();
        assertThat(proceso.lineasProcesadas()).isZero();
        assertThat(trazaRepository.findByLoteMigracionOrderByIdAsc(lote)).isEmpty();
        assertThat(movimientoInventarioRepository.findAll()).noneMatch(movimiento ->
                lote.equals(movimiento.getDocumentoReferencia())
        );
    }

    @Test
    void reprocesarLoteYaProcesadoNoDuplicaMovimientos() {
        TestData data = crearDatosBase();
        String lote = "MIG-REPROC-" + suffix();
        String codigoMovimientoExterno = "SI-REPROC-" + suffix();
        crearFila(
                lote,
                codigoMovimientoExterno,
                "1",
                data.producto1().getCodigo(),
                data.almacen().getCodigo(),
                null,
                new BigDecimal("15.0000"),
                new BigDecimal("3.250000")
        );

        var primerProceso = importacionSaldoInicialService.procesar(lote, authentication());
        var segundoProceso = importacionSaldoInicialService.procesar(lote, authentication());

        assertThat(primerProceso.movimientosProcesadosEnEjecucion()).isEqualTo(1);
        assertThat(segundoProceso.movimientosProcesadosEnEjecucion()).isZero();
        assertThat(segundoProceso.lineasProcesadas()).isEqualTo(1);
        assertThat(trazaRepository.findByLoteMigracionOrderByIdAsc(lote)).hasSize(1);
        assertThat(importacionRepository.findByLoteMigracionOrderByCodigoMovimientoExternoAscIdAsc(lote))
                .extracting(SaldoInicialImportacionEntity::getMovimientoId)
                .doesNotContainNull();
    }

    private SaldoInicialImportacionEntity crearFila(
            String lote,
            String codigoMovimientoExterno,
            String lineaOrigen,
            String codigoProducto,
            String codigoAlmacen,
            String codigoUbicacion,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        SaldoInicialImportacionEntity fila = new SaldoInicialImportacionEntity();
        fila.setLoteMigracion(lote);
        fila.setSistemaOrigen("ERP-LEGACY");
        fila.setIdOrigenCabecera("CAB-" + codigoMovimientoExterno);
        fila.setCodigoMovimientoExterno(codigoMovimientoExterno);
        fila.setTipoMovimiento("ENTRADA_AJUSTE");
        fila.setConfirmar("SI");
        fila.setFechaMovimiento(FECHA_MOVIMIENTO);
        fila.setUsuarioUsername("admin");
        fila.setCodigoAlmacenDestino(codigoAlmacen);
        fila.setCodigoUbicacionDestino(codigoUbicacion);
        fila.setMotivoMovimiento("SALDO INICIAL MIGRACION");
        fila.setDocumentoReferencia(codigoMovimientoExterno);
        fila.setObservacionCabecera("Carga inicial de prueba");
        fila.setObservacionDocumentaria("Documento legacy de prueba");
        fila.setIdOrigenDetalle("DET-" + lineaOrigen);
        fila.setLineaOrigen(lineaOrigen);
        fila.setCodigoProducto(codigoProducto);
        fila.setCantidad(cantidad);
        fila.setCostoUnitarioInventariable(costoUnitario);
        fila.setTotalInventariableRef(cantidad.multiply(costoUnitario));
        fila.setObservacionDetalle("Linea importada " + lineaOrigen);
        return importacionRepository.save(fila);
    }

    private TestData crearDatosBase() {
        UnidadMedidaEntity unidad = crearUnidadMedida();
        FamiliaEntity familia = crearFamilia();
        TipoArticuloEntity tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();
        ProductoEntity producto1 = crearProducto(unidad, familia, tipoArticulo);
        ProductoEntity producto2 = crearProducto(unidad, familia, tipoArticulo);
        AlmacenEntity almacen = crearAlmacen();
        UbicacionAlmacenEntity ubicacion = crearUbicacion(almacen);
        return new TestData(producto1, producto2, almacen, ubicacion);
    }

    private ProductoEntity crearProducto(
            UnidadMedidaEntity unidad,
            FamiliaEntity familia,
            TipoArticuloEntity tipoArticulo
    ) {
        String suffix = suffix();
        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo("P" + suffix);
        producto.setNombre("Producto Importacion " + suffix);
        producto.setTipoArticulo(tipoArticulo);
        producto.setFamilia(familia);
        producto.setUnidadMedida(unidad);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private UnidadMedidaEntity crearUnidadMedida() {
        String suffix = suffix();
        UnidadMedidaEntity unidad = new UnidadMedidaEntity();
        unidad.setCodigo("U" + suffix);
        unidad.setNombre("Unidad Importacion " + suffix);
        unidad.setAbreviatura("UI" + suffix.substring(0, 4));
        unidad.setActivo(true);
        return unidadMedidaRepository.save(unidad);
    }

    private FamiliaEntity crearFamilia() {
        String suffix = suffix();
        FamiliaEntity familia = new FamiliaEntity();
        familia.setNombre("Familia Importacion " + suffix);
        familia.setPrefijo("FI" + suffix.substring(0, 8));
        familia.setActivo(true);
        return familiaRepository.save(familia);
    }

    private AlmacenEntity crearAlmacen() {
        String suffix = suffix();
        AlmacenEntity almacen = new AlmacenEntity();
        almacen.setCodigo("A" + suffix);
        almacen.setNombre("Almacen Importacion " + suffix);
        almacen.setActivo(true);
        return almacenRepository.save(almacen);
    }

    private UbicacionAlmacenEntity crearUbicacion(AlmacenEntity almacen) {
        String suffix = suffix();
        UbicacionAlmacenEntity ubicacion = new UbicacionAlmacenEntity();
        ubicacion.setAlmacen(almacen);
        ubicacion.setCodigo("U" + suffix.substring(0, 8));
        ubicacion.setNombre("Ubicacion Importacion " + suffix);
        ubicacion.setActivo(true);
        return ubicacionAlmacenRepository.save(ubicacion);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private record TestData(
            ProductoEntity producto1,
            ProductoEntity producto2,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion
    ) {
    }
}
