package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto.ImportacionSaldoInicialErrorResponse;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto.ImportacionSaldoInicialResultadoResponse;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto.ImportacionSaldoInicialTrazaResponse;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity.SaldoInicialImportacionEntity;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity.SaldoInicialImportacionErrorEntity;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity.SaldoInicialImportacionTrazaEntity;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository.SaldoInicialImportacionErrorRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository.SaldoInicialImportacionRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository.SaldoInicialImportacionTrazaRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.shared.EstadoImportacionSaldoInicial;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleResponse;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioResponse;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoDetalleRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.service.CostoInventariableService;
import pe.com.ballena.erpalmacen.inventario.service.MovimientoInventarioService;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ImportacionSaldoInicialService {

    private static final String TIPO_ENTRADA_AJUSTE = "ENTRADA_AJUSTE";
    private static final BigDecimal TOTAL_TOLERANCE = new BigDecimal("0.010000");
    private static final int COST_SCALE = 6;

    private final SaldoInicialImportacionRepository importacionRepository;
    private final SaldoInicialImportacionErrorRepository errorRepository;
    private final SaldoInicialImportacionTrazaRepository trazaRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoDetalleRepository movimientoDetalleRepository;
    private final CostoInventariableService costoInventariableService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final String productosTable;
    private final String almacenesTable;
    private final String ubicacionesTable;
    private final String usuariosTable;
    private final String importacionStagingTable;
    private final String stagingEntradaAjusteCabeceraTable;
    private final String stagingEntradaAjusteDetalleTable;

    public ImportacionSaldoInicialService(
            SaldoInicialImportacionRepository importacionRepository,
            SaldoInicialImportacionErrorRepository errorRepository,
            SaldoInicialImportacionTrazaRepository trazaRepository,
            MovimientoInventarioService movimientoInventarioService,
            MovimientoInventarioRepository movimientoInventarioRepository,
            MovimientoDetalleRepository movimientoDetalleRepository,
            CostoInventariableService costoInventariableService,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            @Value("${spring.jpa.properties.hibernate.default_schema:}") String defaultSchema
    ) {
        this.importacionRepository = importacionRepository;
        this.errorRepository = errorRepository;
        this.trazaRepository = trazaRepository;
        this.movimientoInventarioService = movimientoInventarioService;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.movimientoDetalleRepository = movimientoDetalleRepository;
        this.costoInventariableService = costoInventariableService;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.productosTable = resolverTabla(defaultSchema, "productos");
        this.almacenesTable = resolverTabla(defaultSchema, "almacenes");
        this.ubicacionesTable = resolverTabla(defaultSchema, "ubicaciones_almacen");
        this.usuariosTable = resolverTabla(defaultSchema, "usuarios");
        this.importacionStagingTable = resolverTabla(defaultSchema, "importacion_saldos_iniciales_staging");
        this.stagingEntradaAjusteCabeceraTable = resolverTabla(defaultSchema, "stg_mov_entrada_ajuste_cabecera");
        this.stagingEntradaAjusteDetalleTable = resolverTabla(defaultSchema, "stg_mov_entrada_ajuste_detalle");
    }

    public ImportacionSaldoInicialResultadoResponse cargarDesdeStagingEntradaAjuste(String loteMigracion) {
        String lote = requerirLote(loteMigracion);
        return transactionTemplate.execute(status -> {
            requerirTablasStagingEntradaAjuste();
            errorRepository.deleteByLoteMigracion(lote);
            sincronizarDesdeStagingEntradaAjuste(lote);
            return construirResultado(lote, 0, 0, resolverEstado(lote));
        });
    }

    public ImportacionSaldoInicialResultadoResponse validar(String loteMigracion, Authentication authentication) {
        String lote = requerirLote(loteMigracion);
        String username = requerirUsuarioAutenticado(authentication);
        return transactionTemplate.execute(status -> {
            cargarDesdeStagingEntradaAjusteSiStagingPlanoVacio(lote);
            List<SaldoInicialImportacionEntity> filas = importacionRepository
                    .findByLoteMigracionOrderByCodigoMovimientoExternoAscIdAsc(lote);
            if (filas.isEmpty()) {
                throw new BusinessException("No existen filas de staging para el lote " + lote);
            }

            errorRepository.deleteByLoteMigracion(lote);
            Map<Long, List<ErrorValidacion>> errores = new LinkedHashMap<>();
            ResolverCache resolver = new ResolverCache();

            for (SaldoInicialImportacionEntity fila : filas) {
                if (fila.getEstado() == EstadoImportacionSaldoInicial.PROCESADO) {
                    fila.setMensajeError(null);
                    continue;
                }
                validarFila(fila, username, resolver, errores);
            }
            validarGrupos(filas, errores);
            aplicarResultadoValidacion(filas, errores);
            guardarErrores(filas, errores);

            return construirResultado(lote, 0, 0, resolverEstado(lote));
        });
    }

    public ImportacionSaldoInicialResultadoResponse procesar(String loteMigracion, Authentication authentication) {
        String lote = requerirLote(loteMigracion);
        ImportacionSaldoInicialResultadoResponse validacion = validar(lote, authentication);
        if (validacion.lineasConError() > 0 || validacion.errores() > 0) {
            return validacion;
        }

        List<String> grupos = transactionTemplate.execute(status -> importacionRepository
                .findByLoteMigracionAndEstadoOrderByCodigoMovimientoExternoAscIdAsc(
                        lote,
                        EstadoImportacionSaldoInicial.VALIDADO
                )
                .stream()
                .map(fila -> clean(fila.getCodigoMovimientoExterno()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList());

        int procesados = 0;
        int omitidos = 0;
        for (String codigoMovimientoExterno : grupos) {
            try {
                boolean procesado = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                        procesarGrupo(lote, codigoMovimientoExterno, authentication)
                ));
                if (procesado) {
                    procesados++;
                } else {
                    omitidos++;
                }
            } catch (RuntimeException exception) {
                registrarErrorGrupo(lote, codigoMovimientoExterno, exception);
                omitidos++;
            }
        }

        int procesadosFinal = procesados;
        int omitidosFinal = omitidos;
        return transactionTemplate.execute(status -> construirResultado(
                lote,
                procesadosFinal,
                omitidosFinal,
                resolverEstado(lote)
        ));
    }

    public ImportacionSaldoInicialResultadoResponse resumen(String loteMigracion) {
        String lote = requerirLote(loteMigracion);
        return transactionTemplate.execute(status -> construirResultado(lote, 0, 0, resolverEstado(lote)));
    }

    public List<ImportacionSaldoInicialErrorResponse> listarErrores(String loteMigracion) {
        String lote = requerirLote(loteMigracion);
        return errorRepository.findByLoteMigracionOrderByIdAsc(lote)
                .stream()
                .map(this::toErrorResponse)
                .toList();
    }

    public List<ImportacionSaldoInicialTrazaResponse> listarTrazabilidad(String loteMigracion) {
        String lote = requerirLote(loteMigracion);
        return trazaRepository.findByLoteMigracionOrderByIdAsc(lote)
                .stream()
                .map(this::toTrazaResponse)
                .toList();
    }

    private void cargarDesdeStagingEntradaAjusteSiStagingPlanoVacio(String lote) {
        if (importacionRepository.countByLoteMigracion(lote) > 0 || !existenTablasStagingEntradaAjuste()) {
            return;
        }
        sincronizarDesdeStagingEntradaAjuste(lote);
    }

    private void requerirTablasStagingEntradaAjuste() {
        if (!existenTablasStagingEntradaAjuste()) {
            throw new BusinessException(
                    "No existen las tablas staging "
                            + stagingEntradaAjusteCabeceraTable
                            + " y "
                            + stagingEntradaAjusteDetalleTable
            );
        }
    }

    private boolean existenTablasStagingEntradaAjuste() {
        Boolean existen = jdbcTemplate.queryForObject(
                "select to_regclass(?) is not null and to_regclass(?) is not null",
                Boolean.class,
                stagingEntradaAjusteCabeceraTable,
                stagingEntradaAjusteDetalleTable
        );
        return Boolean.TRUE.equals(existen);
    }

    private int sincronizarDesdeStagingEntradaAjuste(String lote) {
        String sql = """
                insert into %s as destino (
                    lote_migracion,
                    sistema_origen,
                    id_origen_cabecera,
                    codigo_movimiento_externo,
                    tipo_movimiento,
                    confirmar,
                    fecha_movimiento,
                    usuario_username,
                    codigo_almacen_destino,
                    codigo_ubicacion_destino,
                    motivo_movimiento,
                    documento_referencia,
                    observacion_cabecera,
                    observacion_documentaria,
                    id_origen_detalle,
                    linea_origen,
                    codigo_producto,
                    cantidad,
                    costo_unitario_inventariable,
                    valor_unitario_sin_igv,
                    tasa_igv_pct,
                    igv_recuperable,
                    total_inventariable_ref,
                    observacion_detalle,
                    estado,
                    creado_en
                )
                select
                    nullif(btrim(c.lote_migracion), ''),
                    nullif(btrim(c.sistema_origen), ''),
                    nullif(btrim(c.id_origen_cabecera), ''),
                    nullif(btrim(c.codigo_movimiento_externo), ''),
                    nullif(btrim(c.tipo_movimiento), ''),
                    nullif(btrim(c.confirmar), ''),
                    c.fecha_movimiento,
                    nullif(btrim(c.usuario_username), ''),
                    nullif(btrim(c.codigo_almacen_destino), ''),
                    coalesce(
                        nullif(btrim(d.codigo_ubicacion_destino), ''),
                        nullif(btrim(c.codigo_ubicacion_destino_default), '')
                    ),
                    nullif(btrim(c.motivo_movimiento), ''),
                    nullif(btrim(c.documento_referencia), ''),
                    nullif(btrim(c.observacion), ''),
                    nullif(btrim(c.observacion_documentaria), ''),
                    nullif(btrim(d.id_origen_detalle), ''),
                    nullif(btrim(d.linea_origen), ''),
                    nullif(btrim(d.codigo_producto), ''),
                    d.cantidad,
                    d.costo_unitario_inventariable,
                    d.valor_unitario_sin_igv,
                    d.tasa_igv_pct,
                    nullif(btrim(d.igv_recuperable), ''),
                    d.total_inventariable_ref,
                    nullif(btrim(d.observacion_detalle), ''),
                    'PENDIENTE',
                    current_timestamp
                from %s c
                join %s d
                  on nullif(btrim(d.codigo_movimiento_externo), '') = nullif(btrim(c.codigo_movimiento_externo), '')
                where nullif(btrim(c.lote_migracion), '') = ?
                  and d.cantidad > 0
                on conflict (lote_migracion, codigo_movimiento_externo, linea_origen)
                do update set
                    sistema_origen = excluded.sistema_origen,
                    id_origen_cabecera = excluded.id_origen_cabecera,
                    tipo_movimiento = excluded.tipo_movimiento,
                    confirmar = excluded.confirmar,
                    fecha_movimiento = excluded.fecha_movimiento,
                    usuario_username = excluded.usuario_username,
                    codigo_almacen_destino = excluded.codigo_almacen_destino,
                    codigo_ubicacion_destino = excluded.codigo_ubicacion_destino,
                    motivo_movimiento = excluded.motivo_movimiento,
                    documento_referencia = excluded.documento_referencia,
                    observacion_cabecera = excluded.observacion_cabecera,
                    observacion_documentaria = excluded.observacion_documentaria,
                    id_origen_detalle = excluded.id_origen_detalle,
                    codigo_producto = excluded.codigo_producto,
                    cantidad = excluded.cantidad,
                    costo_unitario_inventariable = excluded.costo_unitario_inventariable,
                    valor_unitario_sin_igv = excluded.valor_unitario_sin_igv,
                    tasa_igv_pct = excluded.tasa_igv_pct,
                    igv_recuperable = excluded.igv_recuperable,
                    total_inventariable_ref = excluded.total_inventariable_ref,
                    observacion_detalle = excluded.observacion_detalle,
                    estado = 'PENDIENTE',
                    mensaje_error = null,
                    movimiento_id = null,
                    movimiento_detalle_id = null,
                    numero_movimiento = null,
                    actualizado_en = current_timestamp,
                    procesado_en = null
                where destino.estado <> 'PROCESADO'
                """.formatted(
                importacionStagingTable,
                stagingEntradaAjusteCabeceraTable,
                stagingEntradaAjusteDetalleTable
        );
        try {
            return jdbcTemplate.update(sql, lote);
        } catch (DataAccessException exception) {
            throw new BusinessException(
                    "No se pudo leer el staging de entrada por ajuste. Verifique permisos SELECT para "
                            + stagingEntradaAjusteCabeceraTable
                            + " y "
                            + stagingEntradaAjusteDetalleTable
                            + " con el usuario de la aplicacion: "
                            + mensajeRaiz(exception)
            );
        }
    }

    private void validarFila(
            SaldoInicialImportacionEntity fila,
            String username,
            ResolverCache resolver,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        String tipoMovimiento = normalizeUpper(fila.getTipoMovimiento());
        if (tipoMovimiento == null) {
            agregarError(errores, fila, "TIPO_REQUERIDO", "El tipo de movimiento es obligatorio");
        } else if (!TIPO_ENTRADA_AJUSTE.equals(tipoMovimiento)) {
            agregarError(errores, fila, "TIPO_INVALIDO", "El tipo de movimiento debe ser ENTRADA_AJUSTE");
        }

        Boolean confirmar = parseBoolean(fila.getConfirmar());
        if (confirmar == null) {
            agregarError(errores, fila, "CONFIRMAR_INVALIDO", "El campo confirmar debe ser SI o NO");
        }

        if (fila.getFechaMovimiento() == null) {
            agregarError(errores, fila, "FECHA_REQUERIDA", "La fecha del movimiento es obligatoria");
        }

        String usuarioFila = clean(fila.getUsuarioUsername());
        if (usuarioFila != null && !usuarioFila.equals(username)) {
            agregarError(
                    errores,
                    fila,
                    "USUARIO_NO_COINCIDE",
                    "El usuario_username de staging debe coincidir con el usuario autenticado que procesa"
            );
        }
        if (!resolver.existeUsuarioActivo(username)) {
            agregarError(errores, fila, "USUARIO_INVALIDO", "El usuario autenticado no existe o no esta activo");
        }

        String codigoMovimientoExterno = clean(fila.getCodigoMovimientoExterno());
        if (codigoMovimientoExterno == null) {
            agregarError(errores, fila, "MOVIMIENTO_EXTERNO_REQUERIDO", "El codigo_movimiento_externo es obligatorio");
        } else if (codigoMovimientoExterno.length() > 100) {
            agregarError(errores, fila, "MOVIMIENTO_EXTERNO_LARGO", "El codigo_movimiento_externo no puede superar 100 caracteres");
        }

        String documentoReferencia = resolverDocumentoReferencia(fila);
        if (documentoReferencia != null && documentoReferencia.length() > 100) {
            agregarError(errores, fila, "DOCUMENTO_REFERENCIA_LARGO", "El documento_referencia no puede superar 100 caracteres");
        }

        String codigoAlmacen = clean(fila.getCodigoAlmacenDestino());
        Long almacenId = null;
        if (codigoAlmacen == null) {
            agregarError(errores, fila, "ALMACEN_REQUERIDO", "El codigo_almacen_destino es obligatorio");
        } else {
            almacenId = resolver.almacenId(codigoAlmacen);
            if (almacenId == null) {
                agregarError(errores, fila, "ALMACEN_INVALIDO", "El almacen destino no existe o no esta activo");
            }
        }

        String ubicacion = clean(fila.getCodigoUbicacionDestino());
        if (ubicacion != null && almacenId != null && resolver.ubicacionId(codigoAlmacen, ubicacion) == null) {
            agregarError(
                    errores,
                    fila,
                    "UBICACION_INVALIDA",
                    "La ubicacion destino no existe, no esta activa o no pertenece al almacen destino"
            );
        }

        if (clean(fila.getMotivoMovimiento()) == null) {
            agregarError(errores, fila, "MOTIVO_REQUERIDO", "La entrada por ajuste requiere motivo");
        }
        validarLongitud(errores, fila, fila.getMotivoMovimiento(), 150, "MOTIVO_LARGO", "El motivo no puede superar 150 caracteres");
        validarLongitud(errores, fila, fila.getObservacionCabecera(), 500, "OBSERVACION_CABECERA_LARGA", "La observacion de cabecera no puede superar 500 caracteres");
        validarLongitud(errores, fila, fila.getObservacionDetalle(), 255, "OBSERVACION_DETALLE_LARGA", "La observacion de detalle no puede superar 255 caracteres");

        String lineaOrigen = clean(fila.getLineaOrigen());
        if (lineaOrigen == null) {
            agregarError(errores, fila, "LINEA_REQUERIDA", "La linea_origen es obligatoria");
        }

        String codigoProducto = clean(fila.getCodigoProducto());
        if (codigoProducto == null) {
            agregarError(errores, fila, "PRODUCTO_REQUERIDO", "El codigo_producto es obligatorio");
        } else if (resolver.productoId(codigoProducto) == null) {
            agregarError(errores, fila, "PRODUCTO_INVALIDO", "El producto no existe o no esta activo");
        }

        if (fila.getCantidad() == null || fila.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            agregarError(errores, fila, "CANTIDAD_INVALIDA", "La cantidad debe ser mayor que cero");
        }
        validarNoNegativo(errores, fila, fila.getCostoUnitarioInventariable(), "COSTO_INVALIDO", "El costo unitario inventariable no puede ser negativo");
        validarNoNegativo(errores, fila, fila.getValorUnitarioSinIgv(), "VALOR_SIN_IGV_INVALIDO", "El valor unitario sin IGV no puede ser negativo");
        validarNoNegativo(errores, fila, fila.getTasaIgvPct(), "TASA_IGV_INVALIDA", "La tasa IGV no puede ser negativa");
        validarNoNegativo(errores, fila, fila.getTotalInventariableRef(), "TOTAL_REF_INVALIDO", "El total inventariable referencial no puede ser negativo");

        Boolean igvRecuperable = parseOptionalBoolean(fila.getIgvRecuperable());
        if (hasText(fila.getIgvRecuperable()) && igvRecuperable == null) {
            agregarError(errores, fila, "IGV_RECUPERABLE_INVALIDO", "El campo igv_recuperable debe ser SI, NO o vacio");
        }

        if (fila.getCostoUnitarioInventariable() == null && fila.getValorUnitarioSinIgv() == null) {
            agregarError(
                    errores,
                    fila,
                    "COSTO_REQUERIDO",
                    "El saldo inicial requiere costo_unitario_inventariable o valor_unitario_sin_igv"
            );
        }
        validarTotalReferencial(fila, igvRecuperable, errores);
    }

    private void validarGrupos(
            List<SaldoInicialImportacionEntity> filas,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        Map<String, List<SaldoInicialImportacionEntity>> grupos = filas.stream()
                .filter(fila -> clean(fila.getCodigoMovimientoExterno()) != null)
                .collect(Collectors.groupingBy(
                        fila -> clean(fila.getCodigoMovimientoExterno()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (List<SaldoInicialImportacionEntity> grupo : grupos.values()) {
            validarGrupoParcialmenteProcesado(grupo, errores);
            validarLineasDuplicadas(grupo, errores);
            validarCabeceraConsistente(grupo, errores);
        }
    }

    private void validarGrupoParcialmenteProcesado(
            List<SaldoInicialImportacionEntity> grupo,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        boolean tieneProcesados = grupo.stream()
                .anyMatch(fila -> fila.getEstado() == EstadoImportacionSaldoInicial.PROCESADO);
        boolean tieneNoProcesados = grupo.stream()
                .anyMatch(fila -> fila.getEstado() != EstadoImportacionSaldoInicial.PROCESADO);
        if (!tieneProcesados || !tieneNoProcesados) {
            return;
        }
        grupo.stream()
                .filter(fila -> fila.getEstado() != EstadoImportacionSaldoInicial.PROCESADO)
                .forEach(fila -> agregarError(
                        errores,
                        fila,
                        "GRUPO_PARCIALMENTE_PROCESADO",
                        "El codigo_movimiento_externo ya tiene lineas procesadas; revise trazabilidad antes de continuar"
                ));
    }

    private void validarLineasDuplicadas(
            List<SaldoInicialImportacionEntity> grupo,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        Set<String> lineas = new LinkedHashSet<>();
        for (SaldoInicialImportacionEntity fila : grupo) {
            String linea = clean(fila.getLineaOrigen());
            if (linea != null && !lineas.add(linea)) {
                agregarError(errores, fila, "LINEA_DUPLICADA", "La linea_origen esta duplicada dentro del movimiento externo");
            }
        }
    }

    private void validarCabeceraConsistente(
            List<SaldoInicialImportacionEntity> grupo,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        SaldoInicialImportacionEntity base = grupo.get(0);
        for (SaldoInicialImportacionEntity fila : grupo) {
            compararCampoGrupo(base, fila, "tipo_movimiento", base.getTipoMovimiento(), fila.getTipoMovimiento(), errores);
            compararCampoGrupo(base, fila, "confirmar", base.getConfirmar(), fila.getConfirmar(), errores);
            compararCampoGrupo(base, fila, "fecha_movimiento", base.getFechaMovimiento(), fila.getFechaMovimiento(), errores);
            compararCampoGrupo(base, fila, "usuario_username", base.getUsuarioUsername(), fila.getUsuarioUsername(), errores);
            compararCampoGrupo(base, fila, "codigo_almacen_destino", base.getCodigoAlmacenDestino(), fila.getCodigoAlmacenDestino(), errores);
            compararCampoGrupo(base, fila, "motivo_movimiento", base.getMotivoMovimiento(), fila.getMotivoMovimiento(), errores);
            compararCampoGrupo(base, fila, "documento_referencia", base.getDocumentoReferencia(), fila.getDocumentoReferencia(), errores);
            compararCampoGrupo(base, fila, "observacion_cabecera", base.getObservacionCabecera(), fila.getObservacionCabecera(), errores);
            compararCampoGrupo(base, fila, "observacion_documentaria", base.getObservacionDocumentaria(), fila.getObservacionDocumentaria(), errores);
        }
    }

    private void compararCampoGrupo(
            SaldoInicialImportacionEntity base,
            SaldoInicialImportacionEntity fila,
            String campo,
            Object valorBase,
            Object valorFila,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        Object baseNormalizado = normalizarParaComparacion(valorBase);
        Object filaNormalizada = normalizarParaComparacion(valorFila);
        if (!Objects.equals(baseNormalizado, filaNormalizada)) {
            agregarError(
                    errores,
                    fila,
                    "CABECERA_INCONSISTENTE",
                    "El campo " + campo + " debe repetirse igual para el mismo codigo_movimiento_externo"
            );
        }
    }

    private Object normalizarParaComparacion(Object value) {
        if (value instanceof String text) {
            return clean(text);
        }
        return value;
    }

    private void aplicarResultadoValidacion(
            List<SaldoInicialImportacionEntity> filas,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        for (SaldoInicialImportacionEntity fila : filas) {
            if (fila.getEstado() == EstadoImportacionSaldoInicial.PROCESADO) {
                continue;
            }
            List<ErrorValidacion> erroresFila = errores.getOrDefault(fila.getId(), List.of());
            if (erroresFila.isEmpty()) {
                fila.setEstado(EstadoImportacionSaldoInicial.VALIDADO);
                fila.setMensajeError(null);
            } else {
                fila.setEstado(EstadoImportacionSaldoInicial.ERROR);
                fila.setMensajeError(erroresFila.stream()
                        .map(ErrorValidacion::mensaje)
                        .distinct()
                        .collect(Collectors.joining(" | ")));
            }
        }
    }

    private void guardarErrores(
            List<SaldoInicialImportacionEntity> filas,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        Map<Long, SaldoInicialImportacionEntity> filasPorId = filas.stream()
                .collect(Collectors.toMap(SaldoInicialImportacionEntity::getId, fila -> fila));
        List<SaldoInicialImportacionErrorEntity> entities = new ArrayList<>();
        for (Map.Entry<Long, List<ErrorValidacion>> entry : errores.entrySet()) {
            SaldoInicialImportacionEntity fila = filasPorId.get(entry.getKey());
            for (ErrorValidacion error : entry.getValue()) {
                entities.add(crearError(fila, error.codigo(), error.mensaje()));
            }
        }
        errorRepository.saveAll(entities);
    }

    private boolean procesarGrupo(
            String lote,
            String codigoMovimientoExterno,
            Authentication authentication
    ) {
        List<SaldoInicialImportacionEntity> grupo = importacionRepository
                .findGrupoForUpdate(lote, codigoMovimientoExterno)
                .stream()
                .sorted(Comparator.comparing(SaldoInicialImportacionEntity::getId))
                .toList();
        List<SaldoInicialImportacionEntity> filas = grupo.stream()
                .filter(fila -> fila.getEstado() == EstadoImportacionSaldoInicial.VALIDADO)
                .toList();
        if (filas.isEmpty()) {
            return false;
        }
        boolean tieneEstadoNoProcesable = grupo.stream().anyMatch(fila ->
                fila.getEstado() != EstadoImportacionSaldoInicial.VALIDADO
                        && fila.getEstado() != EstadoImportacionSaldoInicial.PROCESADO
        );
        if (tieneEstadoNoProcesable) {
            throw new BusinessException("El grupo tiene lineas no validadas");
        }

        SaldoInicialImportacionEntity cabecera = filas.get(0);
        Long almacenDestinoId = obtenerAlmacenId(clean(cabecera.getCodigoAlmacenDestino()));
        List<MovimientoDetalleCreateRequest> detalles = new ArrayList<>();
        for (SaldoInicialImportacionEntity fila : filas) {
            Long productoId = obtenerProductoId(clean(fila.getCodigoProducto()));
            Long ubicacionDestinoId = clean(fila.getCodigoUbicacionDestino()) == null
                    ? null
                    : obtenerUbicacionId(clean(cabecera.getCodigoAlmacenDestino()), clean(fila.getCodigoUbicacionDestino()));
            detalles.add(new MovimientoDetalleCreateRequest(
                    productoId,
                    null,
                    ubicacionDestinoId,
                    fila.getCantidad(),
                    fila.getCostoUnitarioInventariable(),
                    fila.getValorUnitarioSinIgv(),
                    fila.getTasaIgvPct(),
                    parseOptionalBoolean(fila.getIgvRecuperable()),
                    null,
                    observacionDetalleConOrigen(fila)
            ));
        }

        MovimientoInventarioResponse movimiento = movimientoInventarioService.crearMovimiento(
                new MovimientoInventarioCreateRequest(
                        TipoMovimientoInventario.ENTRADA_AJUSTE,
                        cabecera.getFechaMovimiento(),
                        null,
                        null,
                        null,
                        almacenDestinoId,
                        null,
                        resolverDocumentoReferencia(cabecera),
                        clean(cabecera.getObservacionCabecera()),
                        clean(cabecera.getMotivoMovimiento()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        parseBoolean(cabecera.getConfirmar()),
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
                        observacionDocumentariaConTrazabilidad(cabecera),
                        null,
                        detalles
                ),
                authentication
        );

        if (movimiento.detalles().size() != filas.size()) {
            throw new BusinessException("La cantidad de detalles generados no coincide con el staging");
        }

        MovimientoInventarioEntity movimientoEntity = movimientoInventarioRepository.findById(movimiento.id())
                .orElseThrow(() -> new BusinessException("No se encontro el movimiento generado"));
        LocalDateTime procesadoEn = LocalDateTime.now();
        for (int i = 0; i < filas.size(); i++) {
            SaldoInicialImportacionEntity fila = filas.get(i);
            MovimientoDetalleResponse detalleResponse = movimiento.detalles().get(i);
            MovimientoDetalleEntity detalleEntity = movimientoDetalleRepository.findById(detalleResponse.id())
                    .orElseThrow(() -> new BusinessException("No se encontro el detalle generado"));

            fila.setEstado(EstadoImportacionSaldoInicial.PROCESADO);
            fila.setMovimientoId(movimiento.id());
            fila.setMovimientoDetalleId(detalleResponse.id());
            fila.setNumeroMovimiento(movimiento.numero());
            fila.setProcesadoEn(procesadoEn);
            fila.setMensajeError(null);

            SaldoInicialImportacionTrazaEntity traza = new SaldoInicialImportacionTrazaEntity();
            traza.setStaging(fila);
            traza.setLoteMigracion(fila.getLoteMigracion());
            traza.setSistemaOrigen(clean(fila.getSistemaOrigen()));
            traza.setIdOrigenCabecera(clean(fila.getIdOrigenCabecera()));
            traza.setIdOrigenDetalle(clean(fila.getIdOrigenDetalle()));
            traza.setCodigoMovimientoExterno(clean(fila.getCodigoMovimientoExterno()));
            traza.setLineaOrigen(clean(fila.getLineaOrigen()));
            traza.setCodigoProducto(clean(fila.getCodigoProducto()));
            traza.setCodigoAlmacenDestino(clean(fila.getCodigoAlmacenDestino()));
            traza.setCodigoUbicacionDestino(clean(fila.getCodigoUbicacionDestino()));
            traza.setCantidad(fila.getCantidad());
            traza.setCostoUnitarioInventariable(detalleResponse.costoUnitarioInventariable());
            traza.setMovimiento(movimientoEntity);
            traza.setMovimientoDetalle(detalleEntity);
            traza.setNumeroMovimiento(movimiento.numero());
            trazaRepository.save(traza);
        }
        return true;
    }

    private void registrarErrorGrupo(String lote, String codigoMovimientoExterno, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(status -> {
            List<SaldoInicialImportacionEntity> filas = importacionRepository
                    .findGrupoForUpdate(lote, codigoMovimientoExterno);
            String mensaje = "No se pudo procesar el grupo: " + mensajeRaiz(exception);
            if (filas.isEmpty()) {
                SaldoInicialImportacionErrorEntity error = new SaldoInicialImportacionErrorEntity();
                error.setLoteMigracion(lote);
                error.setCodigoMovimientoExterno(codigoMovimientoExterno);
                error.setCodigoError("PROCESO_FALLIDO");
                error.setMensaje(truncate(mensaje, 500));
                errorRepository.save(error);
                return;
            }

            for (SaldoInicialImportacionEntity fila : filas) {
                if (fila.getEstado() == EstadoImportacionSaldoInicial.PROCESADO) {
                    continue;
                }
                fila.setEstado(EstadoImportacionSaldoInicial.ERROR);
                fila.setMensajeError(mensaje);
                errorRepository.save(crearError(fila, "PROCESO_FALLIDO", mensaje));
            }
        });
    }

    private void validarTotalReferencial(
            SaldoInicialImportacionEntity fila,
            Boolean igvRecuperable,
            Map<Long, List<ErrorValidacion>> errores
    ) {
        if (fila.getTotalInventariableRef() == null
                || fila.getCantidad() == null
                || fila.getCantidad().compareTo(BigDecimal.ZERO) <= 0
                || (fila.getCostoUnitarioInventariable() == null && fila.getValorUnitarioSinIgv() == null)
                || esNegativo(fila.getCostoUnitarioInventariable())
                || esNegativo(fila.getValorUnitarioSinIgv())
                || esNegativo(fila.getTasaIgvPct())) {
            return;
        }

        BigDecimal totalCalculado;
        if (fila.getValorUnitarioSinIgv() != null) {
            totalCalculado = costoInventariableService.calcularDetalleEntrada(
                    fila.getCantidad(),
                    fila.getCostoUnitarioInventariable(),
                    fila.getValorUnitarioSinIgv(),
                    fila.getTasaIgvPct(),
                    igvRecuperable
            ).totalInventariable();
        } else {
            totalCalculado = fila.getCantidad()
                    .multiply(fila.getCostoUnitarioInventariable())
                    .setScale(COST_SCALE, RoundingMode.HALF_UP);
        }

        if (fila.getTotalInventariableRef().subtract(totalCalculado).abs().compareTo(TOTAL_TOLERANCE) > 0) {
            agregarError(
                    errores,
                    fila,
                    "TOTAL_REF_NO_CUADRA",
                    "El total inventariable referencial no coincide con cantidad por costo"
            );
        }
    }

    private void validarLongitud(
            Map<Long, List<ErrorValidacion>> errores,
            SaldoInicialImportacionEntity fila,
            String value,
            int max,
            String codigo,
            String mensaje
    ) {
        String limpio = clean(value);
        if (limpio != null && limpio.length() > max) {
            agregarError(errores, fila, codigo, mensaje);
        }
    }

    private void validarNoNegativo(
            Map<Long, List<ErrorValidacion>> errores,
            SaldoInicialImportacionEntity fila,
            BigDecimal value,
            String codigo,
            String mensaje
    ) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            agregarError(errores, fila, codigo, mensaje);
        }
    }

    private boolean esNegativo(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private void agregarError(
            Map<Long, List<ErrorValidacion>> errores,
            SaldoInicialImportacionEntity fila,
            String codigo,
            String mensaje
    ) {
        errores.computeIfAbsent(fila.getId(), ignored -> new ArrayList<>())
                .add(new ErrorValidacion(codigo, mensaje));
    }

    private SaldoInicialImportacionErrorEntity crearError(
            SaldoInicialImportacionEntity fila,
            String codigo,
            String mensaje
    ) {
        SaldoInicialImportacionErrorEntity error = new SaldoInicialImportacionErrorEntity();
        error.setStaging(fila);
        error.setLoteMigracion(fila.getLoteMigracion());
        error.setCodigoMovimientoExterno(clean(fila.getCodigoMovimientoExterno()));
        error.setLineaOrigen(clean(fila.getLineaOrigen()));
        error.setCodigoError(codigo);
        error.setMensaje(truncate(mensaje, 500));
        return error;
    }

    private ImportacionSaldoInicialResultadoResponse construirResultado(
            String lote,
            int movimientosProcesados,
            int movimientosOmitidos,
            String estado
    ) {
        List<SaldoInicialImportacionEntity> filas = importacionRepository
                .findByLoteMigracionOrderByCodigoMovimientoExternoAscIdAsc(lote);
        long movimientosExternos = filas.stream()
                .map(fila -> clean(fila.getCodigoMovimientoExterno()))
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long pendientes = contarEstado(filas, EstadoImportacionSaldoInicial.PENDIENTE);
        long validadas = contarEstado(filas, EstadoImportacionSaldoInicial.VALIDADO);
        long conError = contarEstado(filas, EstadoImportacionSaldoInicial.ERROR);
        long procesadas = contarEstado(filas, EstadoImportacionSaldoInicial.PROCESADO);
        return new ImportacionSaldoInicialResultadoResponse(
                lote,
                filas.size(),
                movimientosExternos,
                pendientes,
                validadas,
                conError,
                procesadas,
                errorRepository.countByLoteMigracion(lote),
                trazaRepository.countByLoteMigracion(lote),
                movimientosProcesados,
                movimientosOmitidos,
                estado
        );
    }

    private long contarEstado(
            List<SaldoInicialImportacionEntity> filas,
            EstadoImportacionSaldoInicial estado
    ) {
        return filas.stream().filter(fila -> fila.getEstado() == estado).count();
    }

    private String resolverEstado(String lote) {
        long total = importacionRepository.countByLoteMigracion(lote);
        if (total == 0) {
            return "SIN_DATOS";
        }
        long errores = importacionRepository.countByLoteMigracionAndEstado(lote, EstadoImportacionSaldoInicial.ERROR);
        if (errores > 0) {
            return "CON_ERRORES";
        }
        long pendientes = importacionRepository.countByLoteMigracionAndEstado(lote, EstadoImportacionSaldoInicial.PENDIENTE);
        if (pendientes > 0) {
            return "PENDIENTE";
        }
        long validadas = importacionRepository.countByLoteMigracionAndEstado(lote, EstadoImportacionSaldoInicial.VALIDADO);
        if (validadas > 0) {
            return "VALIDADO";
        }
        return "PROCESADO";
    }

    private ImportacionSaldoInicialErrorResponse toErrorResponse(SaldoInicialImportacionErrorEntity error) {
        return new ImportacionSaldoInicialErrorResponse(
                error.getId(),
                error.getStaging() == null ? null : error.getStaging().getId(),
                error.getLoteMigracion(),
                error.getCodigoMovimientoExterno(),
                error.getLineaOrigen(),
                error.getCodigoError(),
                error.getMensaje(),
                error.getCreadoEn()
        );
    }

    private ImportacionSaldoInicialTrazaResponse toTrazaResponse(SaldoInicialImportacionTrazaEntity traza) {
        return new ImportacionSaldoInicialTrazaResponse(
                traza.getId(),
                traza.getStaging().getId(),
                traza.getLoteMigracion(),
                traza.getSistemaOrigen(),
                traza.getIdOrigenCabecera(),
                traza.getIdOrigenDetalle(),
                traza.getCodigoMovimientoExterno(),
                traza.getLineaOrigen(),
                traza.getCodigoProducto(),
                traza.getCodigoAlmacenDestino(),
                traza.getCodigoUbicacionDestino(),
                traza.getCantidad(),
                traza.getCostoUnitarioInventariable(),
                traza.getMovimiento().getId(),
                traza.getMovimientoDetalle().getId(),
                traza.getNumeroMovimiento(),
                traza.getCreadoEn()
        );
    }

    private String observacionDetalleConOrigen(SaldoInicialImportacionEntity fila) {
        String observacion = clean(fila.getObservacionDetalle());
        String origen = "origen linea=" + clean(fila.getLineaOrigen());
        String idDetalle = clean(fila.getIdOrigenDetalle());
        if (idDetalle != null) {
            origen += "; id_origen_detalle=" + idDetalle;
        }
        String completa = observacion == null ? origen : observacion + " | " + origen;
        return truncate(completa, 255);
    }

    private String observacionDocumentariaConTrazabilidad(SaldoInicialImportacionEntity fila) {
        List<String> partes = new ArrayList<>();
        String observacionDocumentaria = clean(fila.getObservacionDocumentaria());
        if (observacionDocumentaria != null) {
            partes.add(observacionDocumentaria);
        }
        partes.add("lote=" + fila.getLoteMigracion());
        partes.add("codigo_movimiento_externo=" + clean(fila.getCodigoMovimientoExterno()));
        agregarParte(partes, "sistema_origen", fila.getSistemaOrigen());
        agregarParte(partes, "id_origen_cabecera", fila.getIdOrigenCabecera());
        return String.join("; ", partes);
    }

    private void agregarParte(List<String> partes, String key, String value) {
        String limpio = clean(value);
        if (limpio != null) {
            partes.add(key + "=" + limpio);
        }
    }

    private String resolverDocumentoReferencia(SaldoInicialImportacionEntity fila) {
        String documentoReferencia = clean(fila.getDocumentoReferencia());
        return documentoReferencia != null ? documentoReferencia : clean(fila.getCodigoMovimientoExterno());
    }

    private Long obtenerProductoId(String codigoProducto) {
        Long id = queryOptionalLong(
                "select id from " + productosTable + " where codigo = ? and activo = true",
                codigoProducto
        );
        if (id == null) {
            throw new BusinessException("Producto no encontrado para codigo " + codigoProducto);
        }
        return id;
    }

    private Long obtenerAlmacenId(String codigoAlmacen) {
        Long id = queryOptionalLong(
                "select id from " + almacenesTable + " where codigo = ? and activo = true",
                codigoAlmacen
        );
        if (id == null) {
            throw new BusinessException("Almacen no encontrado para codigo " + codigoAlmacen);
        }
        return id;
    }

    private Long obtenerUbicacionId(String codigoAlmacen, String codigoUbicacion) {
        Long id = queryOptionalLong(
                "select u.id "
                        + "from " + ubicacionesTable + " u "
                        + "join " + almacenesTable + " a on a.id = u.almacen_id "
                        + "where a.codigo = ? and a.activo = true and u.codigo = ? and u.activo = true",
                codigoAlmacen,
                codigoUbicacion
        );
        if (id == null) {
            throw new BusinessException("Ubicacion no encontrada para almacen " + codigoAlmacen);
        }
        return id;
    }

    private Long queryOptionalLong(String sql, Object... args) {
        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, args);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String requerirLote(String loteMigracion) {
        String lote = clean(loteMigracion);
        if (lote == null) {
            throw new BusinessException("El lote de migracion es obligatorio");
        }
        return lote;
    }

    private String requerirUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Usuario no autenticado");
        }
        String username = clean(authentication.getName());
        if (username == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        return username;
    }

    private Boolean parseBoolean(String value) {
        String cleanValue = normalizeUpper(value);
        if (cleanValue == null) {
            return null;
        }
        return switch (cleanValue) {
            case "SI", "S", "TRUE", "T", "1", "YES", "Y" -> true;
            case "NO", "N", "FALSE", "F", "0" -> false;
            default -> null;
        };
    }

    private Boolean parseOptionalBoolean(String value) {
        if (!hasText(value)) {
            return null;
        }
        return parseBoolean(value);
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
        return cleanValue.isBlank() ? null : cleanValue;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String mensajeRaiz(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = clean(current.getMessage());
        return message == null ? current.getClass().getSimpleName() : message;
    }

    private String resolverTabla(String schema, String table) {
        String cleanSchema = schema == null ? "" : schema.trim();
        if (cleanSchema.isBlank()) {
            return table;
        }
        if (!cleanSchema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Nombre de esquema invalido para consultas de importacion");
        }
        return cleanSchema + "." + table;
    }

    private record ErrorValidacion(String codigo, String mensaje) {
    }

    private class ResolverCache {
        private final Map<String, Long> productos = new LinkedHashMap<>();
        private final Map<String, Long> almacenes = new LinkedHashMap<>();
        private final Map<String, Long> ubicaciones = new LinkedHashMap<>();
        private final Map<String, Boolean> usuarios = new LinkedHashMap<>();

        private Long productoId(String codigo) {
            return productos.computeIfAbsent(codigo, key -> queryOptionalLong(
                    "select id from " + productosTable + " where codigo = ? and activo = true",
                    key
            ));
        }

        private Long almacenId(String codigo) {
            return almacenes.computeIfAbsent(codigo, key -> queryOptionalLong(
                    "select id from " + almacenesTable + " where codigo = ? and activo = true",
                    key
            ));
        }

        private Long ubicacionId(String codigoAlmacen, String codigoUbicacion) {
            String key = codigoAlmacen + "|" + codigoUbicacion;
            return ubicaciones.computeIfAbsent(key, ignored -> queryOptionalLong(
                    "select u.id "
                            + "from " + ubicacionesTable + " u "
                            + "join " + almacenesTable + " a on a.id = u.almacen_id "
                            + "where a.codigo = ? and a.activo = true and u.codigo = ? and u.activo = true",
                    codigoAlmacen,
                    codigoUbicacion
            ));
        }

        private boolean existeUsuarioActivo(String username) {
            return usuarios.computeIfAbsent(username, key -> queryOptionalLong(
                    "select id from " + usuariosTable + " where username = ? and activo = true",
                    key
            ) != null);
        }
    }
}
