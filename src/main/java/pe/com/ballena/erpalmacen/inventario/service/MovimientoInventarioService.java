package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.repository.UbicacionAlmacenRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoAnulacionRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleResponse;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioResponse;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoDetalleRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.maestros.proveedores.repository.ProveedorRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MovimientoInventarioService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String INVENTARIO_ENTRADA = "INVENTARIO_ENTRADA";
    private static final String INVENTARIO_SALIDA = "INVENTARIO_SALIDA";
    private static final String INVENTARIO_TRANSFERENCIA = "INVENTARIO_TRANSFERENCIA";
    private static final String INVENTARIO_AJUSTE = "INVENTARIO_AJUSTE";
    private static final Set<String> COMPROBANTE_TIPOS_PERMITIDOS = Set.of("FACTURA", "BOLETA", "OTRO");

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoDetalleRepository movimientoDetalleRepository;
    private final StockService stockService;
    private final KardexService kardexService;
    private final NumeroMovimientoService numeroMovimientoService;
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final AlmacenRepository almacenRepository;
    private final UbicacionAlmacenRepository ubicacionAlmacenRepository;
    private final ProductoRepository productoRepository;

    public MovimientoInventarioService(
            MovimientoInventarioRepository movimientoInventarioRepository,
            MovimientoDetalleRepository movimientoDetalleRepository,
            StockService stockService,
            KardexService kardexService,
            NumeroMovimientoService numeroMovimientoService,
            UsuarioRepository usuarioRepository,
            ProveedorRepository proveedorRepository,
            AlmacenRepository almacenRepository,
            UbicacionAlmacenRepository ubicacionAlmacenRepository,
            ProductoRepository productoRepository
    ) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.movimientoDetalleRepository = movimientoDetalleRepository;
        this.stockService = stockService;
        this.kardexService = kardexService;
        this.numeroMovimientoService = numeroMovimientoService;
        this.usuarioRepository = usuarioRepository;
        this.proveedorRepository = proveedorRepository;
        this.almacenRepository = almacenRepository;
        this.ubicacionAlmacenRepository = ubicacionAlmacenRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public MovimientoInventarioResponse crearMovimiento(
            MovimientoInventarioCreateRequest request,
            Authentication authentication
    ) {
        validarPermisoPorTipo(authentication, request.tipoMovimiento());
        validarRequestCabecera(request);

        UsuarioEntity usuario = obtenerUsuarioAutenticado(authentication);
        ProveedorEntity proveedor = obtenerProveedorActivoSiAplica(request.proveedorId());
        AlmacenEntity almacenOrigen = obtenerAlmacenActivoSiAplica(request.almacenOrigenId(), "almacen origen");
        AlmacenEntity almacenDestino = obtenerAlmacenActivoSiAplica(request.almacenDestinoId(), "almacen destino");

        MovimientoInventarioEntity movimiento = new MovimientoInventarioEntity();
        movimiento.setNumero(numeroMovimientoService.generar());
        movimiento.setTipoMovimiento(request.tipoMovimiento());
        movimiento.setEstado(EstadoMovimientoInventario.BORRADOR);
        movimiento.setFechaMovimiento(request.fechaMovimiento());
        movimiento.setProveedor(proveedor);
        movimiento.setAlmacenOrigen(almacenOrigen);
        movimiento.setAlmacenDestino(almacenDestino);
        movimiento.setDocumentoReferencia(clean(request.documentoReferencia()));
        movimiento.setObservacion(clean(request.observacion()));
        movimiento.setMotivoMovimiento(clean(request.motivoMovimiento()));
        movimiento.setOrdenTrabajo(clean(request.ordenTrabajo()));
        movimiento.setAreaSolicitante(clean(request.areaSolicitante()));
        movimiento.setSolicitante(clean(request.solicitante()));
        movimiento.setResponsableEntrega(clean(request.responsableEntrega()));
        movimiento.setResponsableRecepcion(clean(request.responsableRecepcion()));
        movimiento.setGuiaSerie(clean(request.guiaSerie()));
        movimiento.setGuiaNumero(clean(request.guiaNumero()));
        movimiento.setGuiaFecha(request.guiaFecha());
        movimiento.setComprobanteTipo(normalizeUpper(request.comprobanteTipo()));
        movimiento.setComprobanteSerie(clean(request.comprobanteSerie()));
        movimiento.setComprobanteNumero(clean(request.comprobanteNumero()));
        movimiento.setComprobanteFechaEmision(request.comprobanteFechaEmision());
        movimiento.setOrdenCompraNumero(clean(request.ordenCompraNumero()));
        movimiento.setTipoDocumento(clean(request.tipoDocumento()));
        movimiento.setSerieDocumento(clean(request.serieDocumento()));
        movimiento.setNumeroDocumento(clean(request.numeroDocumento()));
        movimiento.setOrdenCompra(clean(request.ordenCompra()));
        movimiento.setFechaPedido(request.fechaPedido());
        movimiento.setFechaRecepcion(request.fechaRecepcion());
        movimiento.setFlete(request.flete());
        movimiento.setMovilidad(request.movilidad());
        movimiento.setOtrosGastos(request.otrosGastos());
        movimiento.setObservacionDocumentaria(clean(request.observacionDocumentaria()));
        movimiento.setUsuario(usuario);
        MovimientoInventarioEntity guardado = movimientoInventarioRepository.save(movimiento);

        for (MovimientoDetalleCreateRequest detalleRequest : request.detalles()) {
            MovimientoDetalleEntity detalle = construirDetalle(
                    guardado,
                    detalleRequest,
                    almacenOrigen,
                    almacenDestino,
                    request.ubicacionOrigenId(),
                    request.ubicacionDestinoId()
            );
            movimientoDetalleRepository.save(detalle);
        }

        if (Boolean.TRUE.equals(request.confirmar())) {
            MovimientoInventarioEntity confirmado = confirmarMovimiento(guardado.getId());
            return toResponse(confirmado);
        }

        return toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public MovimientoInventarioResponse obtener(Long movimientoId) {
        MovimientoInventarioEntity movimiento = movimientoInventarioRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));
        return toResponse(movimiento);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoInventarioResponse> listar(
            TipoMovimientoInventario tipoMovimiento,
            EstadoMovimientoInventario estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String texto,
            Pageable pageable
    ) {
        return movimientoInventarioRepository.findAll(
                buildSpecification(tipoMovimiento, estado, fechaDesde, fechaHasta, texto),
                aplicarOrdenPorDefecto(pageable)
        ).map(this::toResponse);
    }

    @Transactional
    public MovimientoInventarioResponse confirmarMovimiento(Long movimientoId, Authentication authentication) {
        MovimientoInventarioEntity movimiento = movimientoInventarioRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));
        validarPermisoPorTipo(authentication, movimiento.getTipoMovimiento());
        MovimientoInventarioEntity confirmado = confirmarMovimiento(movimientoId);
        return toResponse(confirmado);
    }

    @Transactional
    public MovimientoInventarioEntity confirmarMovimiento(Long movimientoId) {
        MovimientoInventarioEntity movimiento = movimientoInventarioRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));

        validarEstadoConfirmable(movimiento);
        List<MovimientoDetalleEntity> detalles = movimientoDetalleRepository.findByMovimientoIdOrderByIdAsc(movimientoId);
        if (detalles.isEmpty()) {
            throw new BusinessException("El movimiento debe tener al menos un detalle");
        }

        validarCabeceraConfirmacion(movimiento);
        for (MovimientoDetalleEntity detalle : detalles) {
            confirmarDetalle(movimiento, detalle);
        }

        movimiento.setEstado(EstadoMovimientoInventario.CONFIRMADO);
        movimiento.setConfirmadoEn(LocalDateTime.now());
        return movimiento;
    }

    @Transactional
    public MovimientoInventarioResponse anularMovimiento(
            Long movimientoId,
            MovimientoAnulacionRequest request,
            Authentication authentication
    ) {
        validarPermisoAnulacion(authentication);
        MovimientoInventarioEntity movimiento = movimientoInventarioRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));
        MovimientoInventarioEntity anulado = anularMovimiento(movimiento, request.motivo());
        return toResponse(anulado);
    }

    @Transactional
    public MovimientoInventarioEntity anularMovimiento(Long movimientoId, String motivo) {
        MovimientoInventarioEntity movimiento = movimientoInventarioRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));
        return anularMovimiento(movimiento, motivo);
    }

    private MovimientoInventarioEntity anularMovimiento(MovimientoInventarioEntity movimiento, String motivo) {
        validarEstadoAnulable(movimiento);
        String motivoLimpio = clean(motivo);
        if (motivoLimpio == null || motivoLimpio.isBlank()) {
            throw new BusinessException("El motivo de anulacion es obligatorio");
        }

        List<MovimientoDetalleEntity> detalles = movimientoDetalleRepository.findByMovimientoIdOrderByIdAsc(movimiento.getId());
        if (detalles.isEmpty()) {
            throw new BusinessException("El movimiento debe tener al menos un detalle");
        }

        validarCabeceraBase(movimiento);
        LocalDateTime fechaAnulacion = LocalDateTime.now();
        for (MovimientoDetalleEntity detalle : detalles) {
            anularDetalle(movimiento, detalle, motivoLimpio, fechaAnulacion);
        }

        movimiento.setEstado(EstadoMovimientoInventario.ANULADO);
        movimiento.setAnuladoEn(fechaAnulacion);
        return movimiento;
    }

    private MovimientoDetalleEntity construirDetalle(
            MovimientoInventarioEntity movimiento,
            MovimientoDetalleCreateRequest request,
            AlmacenEntity almacenOrigen,
            AlmacenEntity almacenDestino,
            Long ubicacionOrigenCabeceraId,
            Long ubicacionDestinoCabeceraId
    ) {
        ProductoEntity producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (!producto.isActivo()) {
            throw new BusinessException("No se puede usar un producto inactivo");
        }

        UbicacionAlmacenEntity ubicacionOrigen = obtenerUbicacionActivaSiAplica(
                request.ubicacionOrigenId() != null ? request.ubicacionOrigenId() : ubicacionOrigenCabeceraId,
                almacenOrigen,
                "ubicacion origen"
        );
        UbicacionAlmacenEntity ubicacionDestino = obtenerUbicacionActivaSiAplica(
                request.ubicacionDestinoId() != null ? request.ubicacionDestinoId() : ubicacionDestinoCabeceraId,
                almacenDestino,
                "ubicacion destino"
        );

        MovimientoDetalleEntity detalle = new MovimientoDetalleEntity();
        detalle.setMovimiento(movimiento);
        detalle.setProducto(producto);
        detalle.setUbicacionOrigen(ubicacionOrigen);
        detalle.setUbicacionDestino(ubicacionDestino);
        detalle.setCantidad(request.cantidad());
        detalle.setCostoUnitario(request.costoUnitario());
        detalle.setTotalLinea(calcularTotalLinea(request.cantidad(), request.costoUnitario()));
        detalle.setObservacion(clean(request.observacion()));
        return detalle;
    }

    private void validarRequestCabecera(MovimientoInventarioCreateRequest request) {
        TipoMovimientoInventario tipoMovimiento = request.tipoMovimiento();
        if (tipoMovimiento == null) {
            throw new BusinessException("El tipo de movimiento es obligatorio");
        }
        if (request.fechaMovimiento() == null) {
            throw new BusinessException("La fecha del movimiento es obligatoria");
        }
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessException("El movimiento debe tener al menos un detalle");
        }
        validarDatosDocumentales(request);

        switch (tipoMovimiento) {
            case ENTRADA_COMPRA -> {
                requerir(request.proveedorId(), "La entrada por compra requiere proveedor");
                requerir(request.almacenDestinoId(), "La entrada por compra requiere almacen destino");
                requerir(request.fechaRecepcion(), "La entrada por compra requiere fecha de recepcion");
            }
            case ENTRADA_AJUSTE -> {
                requerir(request.almacenDestinoId(), "La entrada por ajuste requiere almacen destino");
                requerirTexto(request.motivoMovimiento(), "La entrada por ajuste requiere motivo");
                requerirTexto(request.observacion(), "La entrada por ajuste requiere observacion");
            }
            case SALIDA_CONSUMO -> {
                requerir(request.almacenOrigenId(), "La salida por consumo requiere almacen origen");
                requerirTexto(request.motivoMovimiento(), "La salida por consumo requiere motivo");
                requerirTexto(request.observacion(), "La salida por consumo requiere observacion");
            }
            case SALIDA_VENTA -> requerir(request.almacenOrigenId(), "La salida por venta requiere almacen origen");
            case SALIDA_AJUSTE -> {
                requerir(request.almacenOrigenId(), "La salida por ajuste requiere almacen origen");
                requerirTexto(request.motivoMovimiento(), "La salida por ajuste requiere motivo");
                requerirTexto(request.observacion(), "La salida por ajuste requiere observacion");
            }
            case TRANSFERENCIA -> {
                requerir(request.almacenOrigenId(), "La transferencia requiere almacen origen");
                requerir(request.almacenDestinoId(), "La transferencia requiere almacen destino");
                if (request.almacenOrigenId().equals(request.almacenDestinoId())) {
                    throw new BusinessException("El almacen origen no puede ser igual al almacen destino");
                }
            }
            case AJUSTE_POSITIVO -> {
                requerir(request.almacenDestinoId(), "El ajuste positivo requiere almacen destino");
                requerirTexto(request.motivoMovimiento(), "El ajuste positivo requiere motivo");
                requerirTexto(request.observacion(), "El ajuste positivo requiere observacion");
            }
            case AJUSTE_NEGATIVO -> {
                requerir(request.almacenOrigenId(), "El ajuste negativo requiere almacen origen");
                requerirTexto(request.motivoMovimiento(), "El ajuste negativo requiere motivo");
                requerirTexto(request.observacion(), "El ajuste negativo requiere observacion");
            }
        }

        for (MovimientoDetalleCreateRequest detalle : request.detalles()) {
            validarDetalleRequest(tipoMovimiento, detalle);
        }
    }

    private void validarDetalleRequest(TipoMovimientoInventario tipoMovimiento, MovimientoDetalleCreateRequest detalle) {
        if (detalle == null) {
            throw new BusinessException("El detalle del movimiento es obligatorio");
        }
        requerir(detalle.productoId(), "El producto del detalle es obligatorio");
        if (detalle.cantidad() == null || detalle.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La cantidad del detalle debe ser mayor que cero");
        }
        if (detalle.costoUnitario() != null && detalle.costoUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El costo unitario no puede ser negativo");
        }
        if (tipoMovimiento == TipoMovimientoInventario.ENTRADA_COMPRA && detalle.costoUnitario() == null) {
            throw new BusinessException("La entrada por compra requiere costo unitario por linea");
        }
    }

    private void validarDatosDocumentales(MovimientoInventarioCreateRequest request) {
        validarMontoNoNegativo(request.flete(), "El flete no puede ser negativo");
        validarMontoNoNegativo(request.movilidad(), "La movilidad no puede ser negativa");
        validarMontoNoNegativo(request.otrosGastos(), "Otros gastos no puede ser negativo");
        validarSerieNumero(
                request.guiaSerie(),
                request.guiaNumero(),
                "La serie y numero de guia deben informarse juntos"
        );
        validarSerieNumero(
                request.comprobanteSerie(),
                request.comprobanteNumero(),
                "La serie y numero de comprobante deben informarse juntos"
        );
        String comprobanteTipo = normalizeUpper(request.comprobanteTipo());
        if (comprobanteTipo != null && !COMPROBANTE_TIPOS_PERMITIDOS.contains(comprobanteTipo)) {
            throw new BusinessException("El tipo de comprobante debe ser FACTURA, BOLETA u OTRO");
        }
        if (request.fechaPedido() != null
                && request.fechaRecepcion() != null
                && request.fechaRecepcion().isBefore(request.fechaPedido())) {
            throw new BusinessException("La fecha de recepcion no puede ser menor que la fecha de pedido");
        }
    }

    private void validarSerieNumero(String serie, String numero, String mensaje) {
        boolean tieneSerie = hasText(serie);
        boolean tieneNumero = hasText(numero);
        if (tieneSerie != tieneNumero) {
            throw new BusinessException(mensaje);
        }
    }

    private void validarMontoNoNegativo(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(message);
        }
    }

    private void requerir(Object value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
    }

    private void requerirTexto(String value, String message) {
        if (!hasText(value)) {
            throw new BusinessException(message);
        }
    }

    private UsuarioEntity obtenerUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Usuario no autenticado");
        }
        UsuarioEntity usuario = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
        if (!usuario.isActivo()) {
            throw new BusinessException("Usuario inactivo");
        }
        return usuario;
    }

    private ProveedorEntity obtenerProveedorActivoSiAplica(Long proveedorId) {
        if (proveedorId == null) {
            return null;
        }
        ProveedorEntity proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        if (!proveedor.isActivo()) {
            throw new BusinessException("No se puede usar un proveedor inactivo");
        }
        return proveedor;
    }

    private AlmacenEntity obtenerAlmacenActivoSiAplica(Long almacenId, String etiqueta) {
        if (almacenId == null) {
            return null;
        }
        AlmacenEntity almacen = almacenRepository.findById(almacenId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro el " + etiqueta));
        if (!almacen.isActivo()) {
            throw new BusinessException("No se puede usar un " + etiqueta + " inactivo");
        }
        return almacen;
    }

    private UbicacionAlmacenEntity obtenerUbicacionActivaSiAplica(
            Long ubicacionId,
            AlmacenEntity almacen,
            String etiqueta
    ) {
        if (ubicacionId == null) {
            return null;
        }
        if (almacen == null) {
            throw new BusinessException("No se puede informar " + etiqueta + " sin almacen asociado");
        }
        UbicacionAlmacenEntity ubicacion = ubicacionAlmacenRepository.findById(ubicacionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro la " + etiqueta));
        validarUbicacionActivaYCoherente(ubicacion, almacen, etiqueta);
        return ubicacion;
    }

    private void confirmarDetalle(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        validarDetalle(movimiento, detalle);

        TipoMovimientoInventario tipoMovimiento = movimiento.getTipoMovimiento();
        if (esEntrada(tipoMovimiento)) {
            confirmarEntrada(movimiento, detalle);
            return;
        }
        if (esSalida(tipoMovimiento)) {
            confirmarSalida(movimiento, detalle);
            return;
        }
        if (tipoMovimiento == TipoMovimientoInventario.TRANSFERENCIA) {
            confirmarTransferencia(movimiento, detalle);
            return;
        }

        throw new BusinessException("Tipo de movimiento no soportado");
    }

    private void confirmarEntrada(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        UbicacionAlmacenEntity ubicacionDestino = detalle.getUbicacionDestino();
        validarAlmacenActivo(almacenDestino, "almacen destino");
        validarUbicacionActivaYCoherente(ubicacionDestino, almacenDestino, "ubicacion destino");

        StockService.StockResultado stockResultado = stockService.registrarEntrada(
                detalle.getProducto(),
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                BigDecimal.ZERO,
                stockResultado,
                resolverObservacion(movimiento, detalle)
        );
    }

    private void confirmarSalida(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        validarAlmacenActivo(almacenOrigen, "almacen origen");
        validarUbicacionActivaYCoherente(ubicacionOrigen, almacenOrigen, "ubicacion origen");

        StockService.StockResultado stockResultado = stockService.registrarSalida(
                detalle.getProducto(),
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenOrigen,
                ubicacionOrigen,
                BigDecimal.ZERO,
                detalle.getCantidad(),
                stockResultado,
                resolverObservacion(movimiento, detalle)
        );
    }

    private void confirmarTransferencia(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        UbicacionAlmacenEntity ubicacionDestino = detalle.getUbicacionDestino();

        validarAlmacenActivo(almacenOrigen, "almacen origen");
        validarAlmacenActivo(almacenDestino, "almacen destino");
        if (almacenOrigen.getId().equals(almacenDestino.getId())) {
            throw new BusinessException("El almacen origen no puede ser igual al almacen destino");
        }
        validarUbicacionActivaYCoherente(ubicacionOrigen, almacenOrigen, "ubicacion origen");
        validarUbicacionActivaYCoherente(ubicacionDestino, almacenDestino, "ubicacion destino");

        StockService.StockResultado salida = stockService.registrarSalida(
                detalle.getProducto(),
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenOrigen,
                ubicacionOrigen,
                BigDecimal.ZERO,
                detalle.getCantidad(),
                salida,
                resolverObservacion(movimiento, detalle)
        );

        BigDecimal costoTransferencia = detalle.getCostoUnitario() != null
                ? detalle.getCostoUnitario()
                : salida.costoPromedio();
        StockService.StockResultado entrada = stockService.registrarEntrada(
                detalle.getProducto(),
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                costoTransferencia,
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                BigDecimal.ZERO,
                entrada,
                resolverObservacion(movimiento, detalle)
        );
    }

    private void anularDetalle(
            MovimientoInventarioEntity movimiento,
            MovimientoDetalleEntity detalle,
            String motivo,
            LocalDateTime fechaAnulacion
    ) {
        validarDetalle(movimiento, detalle);

        TipoMovimientoInventario tipoMovimiento = movimiento.getTipoMovimiento();
        if (esEntrada(tipoMovimiento)) {
            anularEntrada(movimiento, detalle, motivo, fechaAnulacion);
            return;
        }
        if (esSalida(tipoMovimiento)) {
            anularSalida(movimiento, detalle, motivo, fechaAnulacion);
            return;
        }
        if (tipoMovimiento == TipoMovimientoInventario.TRANSFERENCIA) {
            anularTransferencia(movimiento, detalle, motivo, fechaAnulacion);
            return;
        }

        throw new BusinessException("Tipo de movimiento no soportado");
    }

    private void anularEntrada(
            MovimientoInventarioEntity movimiento,
            MovimientoDetalleEntity detalle,
            String motivo,
            LocalDateTime fechaAnulacion
    ) {
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        UbicacionAlmacenEntity ubicacionDestino = detalle.getUbicacionDestino();
        validarAlmacenActivo(almacenDestino, "almacen destino");
        validarUbicacionActivaYCoherente(ubicacionDestino, almacenDestino, "ubicacion destino");

        StockService.StockResultado stockResultado = stockService.registrarSalida(
                detalle.getProducto(),
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                fechaAnulacion
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenDestino,
                ubicacionDestino,
                BigDecimal.ZERO,
                detalle.getCantidad(),
                stockResultado,
                observacionAnulacion(movimiento, motivo)
        );
    }

    private void anularSalida(
            MovimientoInventarioEntity movimiento,
            MovimientoDetalleEntity detalle,
            String motivo,
            LocalDateTime fechaAnulacion
    ) {
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        validarAlmacenActivo(almacenOrigen, "almacen origen");
        validarUbicacionActivaYCoherente(ubicacionOrigen, almacenOrigen, "ubicacion origen");

        StockService.StockResultado stockResultado = stockService.registrarEntrada(
                detalle.getProducto(),
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                fechaAnulacion
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                BigDecimal.ZERO,
                stockResultado,
                observacionAnulacion(movimiento, motivo)
        );
    }

    private void anularTransferencia(
            MovimientoInventarioEntity movimiento,
            MovimientoDetalleEntity detalle,
            String motivo,
            LocalDateTime fechaAnulacion
    ) {
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        UbicacionAlmacenEntity ubicacionDestino = detalle.getUbicacionDestino();

        validarAlmacenActivo(almacenOrigen, "almacen origen");
        validarAlmacenActivo(almacenDestino, "almacen destino");
        if (almacenOrigen.getId().equals(almacenDestino.getId())) {
            throw new BusinessException("El almacen origen no puede ser igual al almacen destino");
        }
        validarUbicacionActivaYCoherente(ubicacionOrigen, almacenOrigen, "ubicacion origen");
        validarUbicacionActivaYCoherente(ubicacionDestino, almacenDestino, "ubicacion destino");

        StockService.StockResultado entradaOrigen = stockService.registrarEntrada(
                detalle.getProducto(),
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                fechaAnulacion
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                BigDecimal.ZERO,
                entradaOrigen,
                observacionAnulacion(movimiento, motivo)
        );

        StockService.StockResultado salidaDestino = stockService.registrarSalida(
                detalle.getProducto(),
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                fechaAnulacion
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenDestino,
                ubicacionDestino,
                BigDecimal.ZERO,
                detalle.getCantidad(),
                salidaDestino,
                observacionAnulacion(movimiento, motivo)
        );
    }

    private void validarEstadoConfirmable(MovimientoInventarioEntity movimiento) {
        if (movimiento.getEstado() == EstadoMovimientoInventario.CONFIRMADO) {
            throw new BusinessException("El movimiento ya fue confirmado");
        }
        if (movimiento.getEstado() == EstadoMovimientoInventario.ANULADO) {
            throw new BusinessException("No se puede confirmar un movimiento anulado");
        }
        if (movimiento.getEstado() != EstadoMovimientoInventario.BORRADOR) {
            throw new BusinessException("Solo se pueden confirmar movimientos en estado BORRADOR");
        }
    }

    private void validarEstadoAnulable(MovimientoInventarioEntity movimiento) {
        if (movimiento.getEstado() == EstadoMovimientoInventario.BORRADOR) {
            throw new BusinessException("No se puede anular un movimiento en estado BORRADOR");
        }
        if (movimiento.getEstado() == EstadoMovimientoInventario.ANULADO) {
            throw new BusinessException("El movimiento ya fue anulado");
        }
        if (movimiento.getEstado() != EstadoMovimientoInventario.CONFIRMADO) {
            throw new BusinessException("Solo se pueden anular movimientos en estado CONFIRMADO");
        }
    }

    private void validarCabeceraBase(MovimientoInventarioEntity movimiento) {
        if (movimiento.getTipoMovimiento() == null) {
            throw new BusinessException("El tipo de movimiento es obligatorio");
        }
        if (movimiento.getUsuario() == null || !movimiento.getUsuario().isActivo()) {
            throw new BusinessException("El usuario del movimiento es obligatorio y debe estar activo");
        }
        if (movimiento.getTipoMovimiento() == TipoMovimientoInventario.ENTRADA_COMPRA && movimiento.getProveedor() == null) {
            throw new BusinessException("La entrada por compra requiere proveedor");
        }
        if (esEntrada(movimiento.getTipoMovimiento()) && movimiento.getAlmacenDestino() == null) {
            throw new BusinessException("El movimiento requiere almacen destino");
        }
        if (esSalida(movimiento.getTipoMovimiento()) && movimiento.getAlmacenOrigen() == null) {
            throw new BusinessException("El movimiento requiere almacen origen");
        }
        if (movimiento.getTipoMovimiento() == TipoMovimientoInventario.TRANSFERENCIA
                && (movimiento.getAlmacenOrigen() == null || movimiento.getAlmacenDestino() == null)) {
            throw new BusinessException("La transferencia requiere almacen origen y destino");
        }
    }

    private void validarCabeceraConfirmacion(MovimientoInventarioEntity movimiento) {
        validarCabeceraBase(movimiento);
        if (movimiento.getFechaMovimiento() == null) {
            throw new BusinessException("La fecha del movimiento es obligatoria");
        }

        switch (movimiento.getTipoMovimiento()) {
            case ENTRADA_COMPRA -> {
                if (movimiento.getFechaRecepcion() == null) {
                    throw new BusinessException("La entrada por compra requiere fecha de recepcion");
                }
            }
            case ENTRADA_AJUSTE -> validarMotivoYObservacion(
                    movimiento,
                    "La entrada por ajuste requiere motivo",
                    "La entrada por ajuste requiere observacion"
            );
            case SALIDA_CONSUMO -> validarMotivoYObservacion(
                    movimiento,
                    "La salida por consumo requiere motivo",
                    "La salida por consumo requiere observacion"
            );
            case SALIDA_VENTA -> {
            }
            case SALIDA_AJUSTE -> validarMotivoYObservacion(
                    movimiento,
                    "La salida por ajuste requiere motivo",
                    "La salida por ajuste requiere observacion"
            );
            case TRANSFERENCIA -> {
                if (movimiento.getAlmacenOrigen().getId().equals(movimiento.getAlmacenDestino().getId())) {
                    throw new BusinessException("El almacen origen no puede ser igual al almacen destino");
                }
            }
            case AJUSTE_POSITIVO -> validarMotivoYObservacion(
                    movimiento,
                    "El ajuste positivo requiere motivo",
                    "El ajuste positivo requiere observacion"
            );
            case AJUSTE_NEGATIVO -> validarMotivoYObservacion(
                    movimiento,
                    "El ajuste negativo requiere motivo",
                    "El ajuste negativo requiere observacion"
            );
        }
    }

    private void validarMotivoYObservacion(
            MovimientoInventarioEntity movimiento,
            String mensajeMotivo,
            String mensajeObservacion
    ) {
        if (!hasText(movimiento.getMotivoMovimiento())) {
            throw new BusinessException(mensajeMotivo);
        }
        if (!hasText(movimiento.getObservacion())) {
            throw new BusinessException(mensajeObservacion);
        }
    }

    private void validarDetalle(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        ProductoEntity producto = detalle.getProducto();
        if (producto == null || !producto.isActivo()) {
            throw new BusinessException("El producto del detalle es obligatorio y debe estar activo");
        }
        if (detalle.getCantidad() == null || detalle.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La cantidad del detalle debe ser mayor que cero");
        }
        if (detalle.getCostoUnitario() != null && detalle.getCostoUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El costo unitario no puede ser negativo");
        }
        if (movimiento.getTipoMovimiento() == TipoMovimientoInventario.ENTRADA_COMPRA && detalle.getCostoUnitario() == null) {
            throw new BusinessException("La entrada por compra requiere costo unitario por linea");
        }
    }

    private void validarAlmacenActivo(AlmacenEntity almacen, String etiqueta) {
        if (almacen == null || !almacen.isActivo()) {
            throw new BusinessException("El " + etiqueta + " es obligatorio y debe estar activo");
        }
    }

    private void validarUbicacionActivaYCoherente(
            UbicacionAlmacenEntity ubicacion,
            AlmacenEntity almacen,
            String etiqueta
    ) {
        if (ubicacion == null) {
            return;
        }
        if (!ubicacion.isActivo()) {
            throw new BusinessException("La " + etiqueta + " debe estar activa");
        }
        if (!ubicacion.getAlmacen().getId().equals(almacen.getId())) {
            throw new BusinessException("La " + etiqueta + " no pertenece al almacen indicado");
        }
    }

    private void validarPermisoPorTipo(Authentication authentication, TipoMovimientoInventario tipoMovimiento) {
        Set<String> authorities = obtenerAuthorities(authentication);
        if (authorities.contains(ROLE_ADMIN)) {
            return;
        }

        String permisoRequerido = switch (tipoMovimiento) {
            case ENTRADA_COMPRA, ENTRADA_AJUSTE -> INVENTARIO_ENTRADA;
            case SALIDA_CONSUMO, SALIDA_VENTA, SALIDA_AJUSTE -> INVENTARIO_SALIDA;
            case TRANSFERENCIA -> INVENTARIO_TRANSFERENCIA;
            case AJUSTE_POSITIVO, AJUSTE_NEGATIVO -> INVENTARIO_AJUSTE;
        };

        if (!authorities.contains(permisoRequerido)) {
            throw new BusinessException("No tiene permisos para el tipo de movimiento solicitado");
        }
    }

    private void validarPermisoAnulacion(Authentication authentication) {
        Set<String> authorities = obtenerAuthorities(authentication);
        if (authorities.contains(ROLE_ADMIN) || authorities.contains(INVENTARIO_AJUSTE)) {
            return;
        }
        throw new BusinessException("No tiene permisos para anular movimientos");
    }

    private Set<String> obtenerAuthorities(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Usuario no autenticado");
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean esEntrada(TipoMovimientoInventario tipoMovimiento) {
        return tipoMovimiento == TipoMovimientoInventario.ENTRADA_COMPRA
                || tipoMovimiento == TipoMovimientoInventario.ENTRADA_AJUSTE
                || tipoMovimiento == TipoMovimientoInventario.AJUSTE_POSITIVO;
    }

    private boolean esSalida(TipoMovimientoInventario tipoMovimiento) {
        return tipoMovimiento == TipoMovimientoInventario.SALIDA_CONSUMO
                || tipoMovimiento == TipoMovimientoInventario.SALIDA_VENTA
                || tipoMovimiento == TipoMovimientoInventario.SALIDA_AJUSTE
                || tipoMovimiento == TipoMovimientoInventario.AJUSTE_NEGATIVO;
    }

    private Specification<MovimientoInventarioEntity> buildSpecification(
            TipoMovimientoInventario tipoMovimiento,
            EstadoMovimientoInventario estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String texto
    ) {
        String textoNormalizado = normalizeSearch(texto);
        return (root, query, criteriaBuilder) -> {
            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (tipoMovimiento != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoMovimiento"), tipoMovimiento));
            }
            if (estado != null) {
                predicates.add(criteriaBuilder.equal(root.get("estado"), estado));
            }
            if (fechaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaMovimiento"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaMovimiento"), fechaHasta));
            }
            if (textoNormalizado != null) {
                String pattern = "%" + textoNormalizado.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("numero")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("documentoReferencia")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("motivoMovimiento")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ordenTrabajo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("areaSolicitante")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("solicitante")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("responsableEntrega")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("responsableRecepcion")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("guiaSerie")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("guiaNumero")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("comprobanteTipo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("comprobanteSerie")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("comprobanteNumero")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ordenCompraNumero")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tipoDocumento")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("serieDocumento")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroDocumento")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ordenCompra")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("observacionDocumentaria")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("observacion")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Pageable aplicarOrdenPorDefecto(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "creadoEn", "id"));
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "creadoEn", "id")
        );
    }

    private MovimientoInventarioResponse toResponse(MovimientoInventarioEntity movimiento) {
        List<MovimientoDetalleEntity> detalleEntities = movimientoDetalleRepository
                .findByMovimientoIdOrderByIdAsc(movimiento.getId());
        List<MovimientoDetalleResponse> detalles = detalleEntities.stream()
                .map(this::toDetalleResponse)
                .toList();

        ProveedorEntity proveedor = movimiento.getProveedor();
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        UsuarioEntity usuario = movimiento.getUsuario();

        return new MovimientoInventarioResponse(
                movimiento.getId(),
                movimiento.getNumero(),
                movimiento.getTipoMovimiento(),
                movimiento.getEstado(),
                movimiento.getFechaMovimiento(),
                proveedor == null ? null : proveedor.getId(),
                proveedor == null ? null : proveedor.getNumeroDocumento(),
                proveedor == null ? null : proveedor.getRazonSocial(),
                almacenOrigen == null ? null : almacenOrigen.getId(),
                almacenOrigen == null ? null : almacenOrigen.getCodigo(),
                almacenOrigen == null ? null : almacenOrigen.getNombre(),
                obtenerUbicacionOrigenCabeceraId(detalleEntities),
                almacenDestino == null ? null : almacenDestino.getId(),
                almacenDestino == null ? null : almacenDestino.getCodigo(),
                almacenDestino == null ? null : almacenDestino.getNombre(),
                obtenerUbicacionDestinoCabeceraId(detalleEntities),
                movimiento.getDocumentoReferencia(),
                movimiento.getObservacion(),
                movimiento.getMotivoMovimiento(),
                movimiento.getOrdenTrabajo(),
                movimiento.getAreaSolicitante(),
                movimiento.getSolicitante(),
                movimiento.getResponsableEntrega(),
                movimiento.getResponsableRecepcion(),
                movimiento.getGuiaSerie(),
                movimiento.getGuiaNumero(),
                movimiento.getGuiaFecha(),
                movimiento.getComprobanteTipo(),
                movimiento.getComprobanteSerie(),
                movimiento.getComprobanteNumero(),
                movimiento.getComprobanteFechaEmision(),
                movimiento.getOrdenCompraNumero(),
                movimiento.getTipoDocumento(),
                movimiento.getSerieDocumento(),
                movimiento.getNumeroDocumento(),
                movimiento.getOrdenCompra(),
                movimiento.getFechaPedido(),
                movimiento.getFechaRecepcion(),
                movimiento.getFlete(),
                movimiento.getMovilidad(),
                movimiento.getOtrosGastos(),
                movimiento.getObservacionDocumentaria(),
                usuario == null ? null : usuario.getId(),
                usuario == null ? null : usuario.getUsername(),
                movimiento.getConfirmadoEn(),
                movimiento.getAnuladoEn(),
                movimiento.getCreadoEn(),
                movimiento.getActualizadoEn(),
                detalles
        );
    }

    private MovimientoDetalleResponse toDetalleResponse(MovimientoDetalleEntity detalle) {
        ProductoEntity producto = detalle.getProducto();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        UbicacionAlmacenEntity ubicacionDestino = detalle.getUbicacionDestino();

        return new MovimientoDetalleResponse(
                detalle.getId(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                ubicacionOrigen == null ? null : ubicacionOrigen.getId(),
                ubicacionOrigen == null ? null : ubicacionOrigen.getCodigo(),
                ubicacionOrigen == null ? null : ubicacionOrigen.getNombre(),
                ubicacionDestino == null ? null : ubicacionDestino.getId(),
                ubicacionDestino == null ? null : ubicacionDestino.getCodigo(),
                ubicacionDestino == null ? null : ubicacionDestino.getNombre(),
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                detalle.getTotalLinea(),
                detalle.getObservacion(),
                detalle.getCreadoEn(),
                detalle.getActualizadoEn()
        );
    }

    private Long obtenerUbicacionOrigenCabeceraId(List<MovimientoDetalleEntity> detalles) {
        return detalles.stream()
                .map(MovimientoDetalleEntity::getUbicacionOrigen)
                .filter(java.util.Objects::nonNull)
                .map(UbicacionAlmacenEntity::getId)
                .findFirst()
                .orElse(null);
    }

    private Long obtenerUbicacionDestinoCabeceraId(List<MovimientoDetalleEntity> detalles) {
        return detalles.stream()
                .map(MovimientoDetalleEntity::getUbicacionDestino)
                .filter(java.util.Objects::nonNull)
                .map(UbicacionAlmacenEntity::getId)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal calcularTotalLinea(BigDecimal cantidad, BigDecimal costoUnitario) {
        if (cantidad == null || costoUnitario == null) {
            return null;
        }
        return cantidad.multiply(costoUnitario);
    }

    private String resolverObservacion(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        return detalle.getObservacion() != null ? detalle.getObservacion() : movimiento.getObservacion();
    }

    private String observacionAnulacion(MovimientoInventarioEntity movimiento, String motivo) {
        String observacion = "ANULACION MOVIMIENTO " + movimiento.getNumero() + ": " + motivo;
        return observacion.length() <= 500 ? observacion : observacion.substring(0, 500);
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private boolean hasText(String value) {
        return clean(value) != null;
    }

    private String normalizeUpper(String value) {
        String cleanValue = clean(value);
        return cleanValue == null ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleanValue = value.trim();
        return cleanValue.isEmpty() ? null : cleanValue;
    }
}
