package pe.com.ballena.erpalmacen.inventario.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.math.BigDecimal;
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
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void crearMovimientoCreaBorradorSinAfectarStockNiKardex() {
        TestData data = crearDatosBase();
        MovimientoInventarioCreateRequest request = new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                null,
                data.almacenOrigen().getId(),
                "DOC-BORRADOR",
                "Movimiento en borrador",
                List.of(new MovimientoDetalleCreateRequest(
                        data.producto().getId(),
                        null,
                        null,
                        new BigDecimal("5.0000"),
                        new BigDecimal("10.0000"),
                        "Detalle borrador"
                ))
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
        ProductoEntity producto = crearProducto(unidad);
        AlmacenEntity almacenOrigen = crearAlmacen();
        AlmacenEntity almacenDestino = crearAlmacen();
        return new TestData(usuario, producto, almacenOrigen, almacenDestino);
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

    private ProductoEntity crearProducto(UnidadMedidaEntity unidad) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo("P" + suffix);
        producto.setNombre("Producto Test " + suffix);
        producto.setUnidadMedida(unidad);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setActivo(true);
        return productoRepository.save(producto);
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
            AlmacenEntity almacenOrigen,
            AlmacenEntity almacenDestino
    ) {
    }
}
