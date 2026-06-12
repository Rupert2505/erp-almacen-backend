package pe.com.ballena.erpalmacen.inventario.movimientos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 40)
    private TipoMovimientoInventario tipoMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMovimientoInventario estado = EstadoMovimientoInventario.BORRADOR;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private ProveedorEntity proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_origen_id")
    private AlmacenEntity almacenOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_destino_id")
    private AlmacenEntity almacenDestino;

    @Column(name = "documento_referencia", length = 100)
    private String documentoReferencia;

    @Column(length = 500)
    private String observacion;

    @Column(name = "motivo_movimiento", length = 150)
    private String motivoMovimiento;

    @Column(name = "orden_trabajo", length = 80)
    private String ordenTrabajo;

    @Column(name = "area_solicitante", length = 120)
    private String areaSolicitante;

    @Column(length = 120)
    private String solicitante;

    @Column(name = "responsable_entrega", length = 120)
    private String responsableEntrega;

    @Column(name = "responsable_recepcion", length = 120)
    private String responsableRecepcion;

    @Column(name = "guia_serie", length = 20)
    private String guiaSerie;

    @Column(name = "guia_numero", length = 50)
    private String guiaNumero;

    @Column(name = "guia_fecha")
    private LocalDate guiaFecha;

    @Column(name = "comprobante_tipo", length = 30)
    private String comprobanteTipo;

    @Column(name = "comprobante_serie", length = 20)
    private String comprobanteSerie;

    @Column(name = "comprobante_numero", length = 50)
    private String comprobanteNumero;

    @Column(name = "comprobante_fecha_emision")
    private LocalDate comprobanteFechaEmision;

    @Column(name = "orden_compra_numero", length = 50)
    private String ordenCompraNumero;

    @Column(name = "tipo_documento", length = 30)
    private String tipoDocumento;

    @Column(name = "serie_documento", length = 30)
    private String serieDocumento;

    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento;

    @Column(name = "orden_compra", length = 100)
    private String ordenCompra;

    @Column(name = "fecha_pedido")
    private LocalDate fechaPedido;

    @Column(name = "fecha_recepcion")
    private LocalDate fechaRecepcion;

    @Column(precision = 18, scale = 6)
    private BigDecimal flete;

    @Column(precision = 18, scale = 6)
    private BigDecimal movilidad;

    @Column(name = "otros_gastos", precision = 18, scale = 6)
    private BigDecimal otrosGastos;

    @Column(name = "observacion_documentaria", columnDefinition = "TEXT")
    private String observacionDocumentaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "confirmado_en")
    private LocalDateTime confirmadoEn;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        creadoEn = now;
        if (fechaMovimiento == null) {
            fechaMovimiento = now;
        }
        if (estado == null) {
            estado = EstadoMovimientoInventario.BORRADOR;
        }
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public TipoMovimientoInventario getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimientoInventario tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public EstadoMovimientoInventario getEstado() {
        return estado;
    }

    public void setEstado(EstadoMovimientoInventario estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public ProveedorEntity getProveedor() {
        return proveedor;
    }

    public void setProveedor(ProveedorEntity proveedor) {
        this.proveedor = proveedor;
    }

    public AlmacenEntity getAlmacenOrigen() {
        return almacenOrigen;
    }

    public void setAlmacenOrigen(AlmacenEntity almacenOrigen) {
        this.almacenOrigen = almacenOrigen;
    }

    public AlmacenEntity getAlmacenDestino() {
        return almacenDestino;
    }

    public void setAlmacenDestino(AlmacenEntity almacenDestino) {
        this.almacenDestino = almacenDestino;
    }

    public String getDocumentoReferencia() {
        return documentoReferencia;
    }

    public void setDocumentoReferencia(String documentoReferencia) {
        this.documentoReferencia = documentoReferencia;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getMotivoMovimiento() {
        return motivoMovimiento;
    }

    public void setMotivoMovimiento(String motivoMovimiento) {
        this.motivoMovimiento = motivoMovimiento;
    }

    public String getOrdenTrabajo() {
        return ordenTrabajo;
    }

    public void setOrdenTrabajo(String ordenTrabajo) {
        this.ordenTrabajo = ordenTrabajo;
    }

    public String getAreaSolicitante() {
        return areaSolicitante;
    }

    public void setAreaSolicitante(String areaSolicitante) {
        this.areaSolicitante = areaSolicitante;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public String getResponsableEntrega() {
        return responsableEntrega;
    }

    public void setResponsableEntrega(String responsableEntrega) {
        this.responsableEntrega = responsableEntrega;
    }

    public String getResponsableRecepcion() {
        return responsableRecepcion;
    }

    public void setResponsableRecepcion(String responsableRecepcion) {
        this.responsableRecepcion = responsableRecepcion;
    }

    public String getGuiaSerie() {
        return guiaSerie;
    }

    public void setGuiaSerie(String guiaSerie) {
        this.guiaSerie = guiaSerie;
    }

    public String getGuiaNumero() {
        return guiaNumero;
    }

    public void setGuiaNumero(String guiaNumero) {
        this.guiaNumero = guiaNumero;
    }

    public LocalDate getGuiaFecha() {
        return guiaFecha;
    }

    public void setGuiaFecha(LocalDate guiaFecha) {
        this.guiaFecha = guiaFecha;
    }

    public String getComprobanteTipo() {
        return comprobanteTipo;
    }

    public void setComprobanteTipo(String comprobanteTipo) {
        this.comprobanteTipo = comprobanteTipo;
    }

    public String getComprobanteSerie() {
        return comprobanteSerie;
    }

    public void setComprobanteSerie(String comprobanteSerie) {
        this.comprobanteSerie = comprobanteSerie;
    }

    public String getComprobanteNumero() {
        return comprobanteNumero;
    }

    public void setComprobanteNumero(String comprobanteNumero) {
        this.comprobanteNumero = comprobanteNumero;
    }

    public LocalDate getComprobanteFechaEmision() {
        return comprobanteFechaEmision;
    }

    public void setComprobanteFechaEmision(LocalDate comprobanteFechaEmision) {
        this.comprobanteFechaEmision = comprobanteFechaEmision;
    }

    public String getOrdenCompraNumero() {
        return ordenCompraNumero;
    }

    public void setOrdenCompraNumero(String ordenCompraNumero) {
        this.ordenCompraNumero = ordenCompraNumero;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getSerieDocumento() {
        return serieDocumento;
    }

    public void setSerieDocumento(String serieDocumento) {
        this.serieDocumento = serieDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getOrdenCompra() {
        return ordenCompra;
    }

    public void setOrdenCompra(String ordenCompra) {
        this.ordenCompra = ordenCompra;
    }

    public LocalDate getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(LocalDate fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public LocalDate getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDate fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public BigDecimal getFlete() {
        return flete;
    }

    public void setFlete(BigDecimal flete) {
        this.flete = flete;
    }

    public BigDecimal getMovilidad() {
        return movilidad;
    }

    public void setMovilidad(BigDecimal movilidad) {
        this.movilidad = movilidad;
    }

    public BigDecimal getOtrosGastos() {
        return otrosGastos;
    }

    public void setOtrosGastos(BigDecimal otrosGastos) {
        this.otrosGastos = otrosGastos;
    }

    public String getObservacionDocumentaria() {
        return observacionDocumentaria;
    }

    public void setObservacionDocumentaria(String observacionDocumentaria) {
        this.observacionDocumentaria = observacionDocumentaria;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getConfirmadoEn() {
        return confirmadoEn;
    }

    public void setConfirmadoEn(LocalDateTime confirmadoEn) {
        this.confirmadoEn = confirmadoEn;
    }

    public LocalDateTime getAnuladoEn() {
        return anuladoEn;
    }

    public void setAnuladoEn(LocalDateTime anuladoEn) {
        this.anuladoEn = anuladoEn;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
