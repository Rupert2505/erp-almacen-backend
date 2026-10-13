package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.shared.EstadoImportacionSaldoInicial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "importacion_saldos_iniciales_staging")
public class SaldoInicialImportacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lote_migracion", nullable = false, length = 80)
    private String loteMigracion;

    @Column(name = "sistema_origen", length = 80)
    private String sistemaOrigen;

    @Column(name = "id_origen_cabecera", length = 120)
    private String idOrigenCabecera;

    @Column(name = "codigo_movimiento_externo", length = 100)
    private String codigoMovimientoExterno;

    @Column(name = "tipo_movimiento", length = 40)
    private String tipoMovimiento;

    @Column(length = 10)
    private String confirmar;

    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento;

    @Column(name = "usuario_username", length = 50)
    private String usuarioUsername;

    @Column(name = "codigo_almacen_destino", length = 30)
    private String codigoAlmacenDestino;

    @Column(name = "codigo_ubicacion_destino", length = 30)
    private String codigoUbicacionDestino;

    @Column(name = "motivo_movimiento", length = 150)
    private String motivoMovimiento;

    @Column(name = "documento_referencia", length = 100)
    private String documentoReferencia;

    @Column(name = "observacion_cabecera", length = 500)
    private String observacionCabecera;

    @Column(name = "observacion_documentaria", columnDefinition = "TEXT")
    private String observacionDocumentaria;

    @Column(name = "id_origen_detalle", length = 120)
    private String idOrigenDetalle;

    @Column(name = "linea_origen", length = 80)
    private String lineaOrigen;

    @Column(name = "codigo_producto", length = 50)
    private String codigoProducto;

    @Column(precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario_inventariable", precision = 18, scale = 6)
    private BigDecimal costoUnitarioInventariable;

    @Column(name = "valor_unitario_sin_igv", precision = 18, scale = 6)
    private BigDecimal valorUnitarioSinIgv;

    @Column(name = "tasa_igv_pct", precision = 7, scale = 4)
    private BigDecimal tasaIgvPct;

    @Column(name = "igv_recuperable", length = 10)
    private String igvRecuperable;

    @Column(name = "total_inventariable_ref", precision = 18, scale = 6)
    private BigDecimal totalInventariableRef;

    @Column(name = "observacion_detalle", length = 255)
    private String observacionDetalle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoImportacionSaldoInicial estado = EstadoImportacionSaldoInicial.PENDIENTE;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "movimiento_id")
    private Long movimientoId;

    @Column(name = "movimiento_detalle_id")
    private Long movimientoDetalleId;

    @Column(name = "numero_movimiento", length = 30)
    private String numeroMovimiento;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(name = "procesado_en")
    private LocalDateTime procesadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoImportacionSaldoInicial.PENDIENTE;
        }
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getLoteMigracion() {
        return loteMigracion;
    }

    public void setLoteMigracion(String loteMigracion) {
        this.loteMigracion = loteMigracion;
    }

    public String getSistemaOrigen() {
        return sistemaOrigen;
    }

    public void setSistemaOrigen(String sistemaOrigen) {
        this.sistemaOrigen = sistemaOrigen;
    }

    public String getIdOrigenCabecera() {
        return idOrigenCabecera;
    }

    public void setIdOrigenCabecera(String idOrigenCabecera) {
        this.idOrigenCabecera = idOrigenCabecera;
    }

    public String getCodigoMovimientoExterno() {
        return codigoMovimientoExterno;
    }

    public void setCodigoMovimientoExterno(String codigoMovimientoExterno) {
        this.codigoMovimientoExterno = codigoMovimientoExterno;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public String getConfirmar() {
        return confirmar;
    }

    public void setConfirmar(String confirmar) {
        this.confirmar = confirmar;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public String getUsuarioUsername() {
        return usuarioUsername;
    }

    public void setUsuarioUsername(String usuarioUsername) {
        this.usuarioUsername = usuarioUsername;
    }

    public String getCodigoAlmacenDestino() {
        return codigoAlmacenDestino;
    }

    public void setCodigoAlmacenDestino(String codigoAlmacenDestino) {
        this.codigoAlmacenDestino = codigoAlmacenDestino;
    }

    public String getCodigoUbicacionDestino() {
        return codigoUbicacionDestino;
    }

    public void setCodigoUbicacionDestino(String codigoUbicacionDestino) {
        this.codigoUbicacionDestino = codigoUbicacionDestino;
    }

    public String getMotivoMovimiento() {
        return motivoMovimiento;
    }

    public void setMotivoMovimiento(String motivoMovimiento) {
        this.motivoMovimiento = motivoMovimiento;
    }

    public String getDocumentoReferencia() {
        return documentoReferencia;
    }

    public void setDocumentoReferencia(String documentoReferencia) {
        this.documentoReferencia = documentoReferencia;
    }

    public String getObservacionCabecera() {
        return observacionCabecera;
    }

    public void setObservacionCabecera(String observacionCabecera) {
        this.observacionCabecera = observacionCabecera;
    }

    public String getObservacionDocumentaria() {
        return observacionDocumentaria;
    }

    public void setObservacionDocumentaria(String observacionDocumentaria) {
        this.observacionDocumentaria = observacionDocumentaria;
    }

    public String getIdOrigenDetalle() {
        return idOrigenDetalle;
    }

    public void setIdOrigenDetalle(String idOrigenDetalle) {
        this.idOrigenDetalle = idOrigenDetalle;
    }

    public String getLineaOrigen() {
        return lineaOrigen;
    }

    public void setLineaOrigen(String lineaOrigen) {
        this.lineaOrigen = lineaOrigen;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitarioInventariable() {
        return costoUnitarioInventariable;
    }

    public void setCostoUnitarioInventariable(BigDecimal costoUnitarioInventariable) {
        this.costoUnitarioInventariable = costoUnitarioInventariable;
    }

    public BigDecimal getValorUnitarioSinIgv() {
        return valorUnitarioSinIgv;
    }

    public void setValorUnitarioSinIgv(BigDecimal valorUnitarioSinIgv) {
        this.valorUnitarioSinIgv = valorUnitarioSinIgv;
    }

    public BigDecimal getTasaIgvPct() {
        return tasaIgvPct;
    }

    public void setTasaIgvPct(BigDecimal tasaIgvPct) {
        this.tasaIgvPct = tasaIgvPct;
    }

    public String getIgvRecuperable() {
        return igvRecuperable;
    }

    public void setIgvRecuperable(String igvRecuperable) {
        this.igvRecuperable = igvRecuperable;
    }

    public BigDecimal getTotalInventariableRef() {
        return totalInventariableRef;
    }

    public void setTotalInventariableRef(BigDecimal totalInventariableRef) {
        this.totalInventariableRef = totalInventariableRef;
    }

    public String getObservacionDetalle() {
        return observacionDetalle;
    }

    public void setObservacionDetalle(String observacionDetalle) {
        this.observacionDetalle = observacionDetalle;
    }

    public EstadoImportacionSaldoInicial getEstado() {
        return estado;
    }

    public void setEstado(EstadoImportacionSaldoInicial estado) {
        this.estado = estado;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public Long getMovimientoId() {
        return movimientoId;
    }

    public void setMovimientoId(Long movimientoId) {
        this.movimientoId = movimientoId;
    }

    public Long getMovimientoDetalleId() {
        return movimientoDetalleId;
    }

    public void setMovimientoDetalleId(Long movimientoDetalleId) {
        this.movimientoDetalleId = movimientoDetalleId;
    }

    public String getNumeroMovimiento() {
        return numeroMovimiento;
    }

    public void setNumeroMovimiento(String numeroMovimiento) {
        this.numeroMovimiento = numeroMovimiento;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public LocalDateTime getProcesadoEn() {
        return procesadoEn;
    }

    public void setProcesadoEn(LocalDateTime procesadoEn) {
        this.procesadoEn = procesadoEn;
    }
}
