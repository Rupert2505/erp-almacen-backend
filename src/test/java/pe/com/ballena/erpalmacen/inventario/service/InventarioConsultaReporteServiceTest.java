package pe.com.ballena.erpalmacen.inventario.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.repository.UbicacionAlmacenRepository;
import pe.com.ballena.erpalmacen.inventario.kardex.repository.KardexRepository;
import pe.com.ballena.erpalmacen.inventario.kardex.service.KardexConsultaService;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.stock.repository.StockActualRepository;
import pe.com.ballena.erpalmacen.inventario.stock.service.StockConsultaService;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.maestros.proveedores.repository.ProveedorRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.reportes.inventario.service.ReporteInventarioService;
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
    private TipoArticuloRepository tipoArticuloRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private UbicacionAlmacenRepository ubicacionAlmacenRepository;

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
        ).getContent()).anyMatch(r -> r.productoId().equals(data.producto().getId()));
        assertThat(reporteInventarioService.obtenerResumen().totalStockActual()).isNotNull();
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

    private MovimientoInventarioCreateRequest entradaCompra(
            TestData data,
            Boolean confirmar,
            Long almacenDestinoId,
            Long ubicacionDestinoId,
            String documento,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        return new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_COMPRA,
                LocalDateTime.now(),
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
        return new MovimientoInventarioCreateRequest(
                tipo,
                LocalDateTime.now(),
                null,
                data.almacenOrigen().getId(),
                null,
                null,
                null,
                documento,
                "Salida de prueba",
                tipo == TipoMovimientoInventario.SALIDA_VENTA ? null : "Motivo salida",
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
                        null,
                        "Detalle salida"
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
        AlmacenEntity almacenOrigen = crearAlmacen("O" + suffix);
        AlmacenEntity almacenDestino = crearAlmacen("D" + suffix);
        UbicacionAlmacenEntity ubicacionOrigen = crearUbicacion("UO" + suffix, almacenOrigen);
        UbicacionAlmacenEntity ubicacionDestino = crearUbicacion("UD" + suffix, almacenDestino);
        return new TestData(suffix, usuario, producto, proveedor, almacenOrigen, almacenDestino, ubicacionOrigen, ubicacionDestino);
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
            AlmacenEntity almacenOrigen,
            AlmacenEntity almacenDestino,
            UbicacionAlmacenEntity ubicacionOrigen,
            UbicacionAlmacenEntity ubicacionDestino
    ) {
    }
}
