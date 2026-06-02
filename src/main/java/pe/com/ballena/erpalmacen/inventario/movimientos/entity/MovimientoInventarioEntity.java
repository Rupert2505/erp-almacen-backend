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
