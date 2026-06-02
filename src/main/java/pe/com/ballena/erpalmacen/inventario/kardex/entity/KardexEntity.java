package pe.com.ballena.erpalmacen.inventario.kardex.entity;

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
import jakarta.persistence.Table;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kardex")
public class KardexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimiento_id", nullable = false)
    private MovimientoInventarioEntity movimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimiento_detalle_id", nullable = false)
    private MovimientoDetalleEntity movimientoDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private AlmacenEntity almacen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_id")
    private UbicacionAlmacenEntity ubicacion;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 40)
    private TipoMovimientoInventario tipoMovimiento;

    @Column(name = "numero_movimiento", nullable = false, length = 30)
    private String numeroMovimiento;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal entrada = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal salida = BigDecimal.ZERO;

    @Column(name = "saldo_anterior", nullable = false, precision = 18, scale = 4)
    private BigDecimal saldoAnterior = BigDecimal.ZERO;

    @Column(name = "saldo_final", nullable = false, precision = 18, scale = 4)
    private BigDecimal saldoFinal = BigDecimal.ZERO;

    @Column(name = "costo_unitario", precision = 18, scale = 4)
    private BigDecimal costoUnitario;

    @Column(name = "costo_promedio", precision = 18, scale = 4)
    private BigDecimal costoPromedio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(length = 500)
    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public MovimientoInventarioEntity getMovimiento() {
        return movimiento;
    }

    public void setMovimiento(MovimientoInventarioEntity movimiento) {
        this.movimiento = movimiento;
    }

    public MovimientoDetalleEntity getMovimientoDetalle() {
        return movimientoDetalle;
    }

    public void setMovimientoDetalle(MovimientoDetalleEntity movimientoDetalle) {
        this.movimientoDetalle = movimientoDetalle;
    }

    public ProductoEntity getProducto() {
        return producto;
    }

    public void setProducto(ProductoEntity producto) {
        this.producto = producto;
    }

    public AlmacenEntity getAlmacen() {
        return almacen;
    }

    public void setAlmacen(AlmacenEntity almacen) {
        this.almacen = almacen;
    }

    public UbicacionAlmacenEntity getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(UbicacionAlmacenEntity ubicacion) {
        this.ubicacion = ubicacion;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public TipoMovimientoInventario getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimientoInventario tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public String getNumeroMovimiento() {
        return numeroMovimiento;
    }

    public void setNumeroMovimiento(String numeroMovimiento) {
        this.numeroMovimiento = numeroMovimiento;
    }

    public BigDecimal getEntrada() {
        return entrada;
    }

    public void setEntrada(BigDecimal entrada) {
        this.entrada = entrada;
    }

    public BigDecimal getSalida() {
        return salida;
    }

    public void setSalida(BigDecimal salida) {
        this.salida = salida;
    }

    public BigDecimal getSaldoAnterior() {
        return saldoAnterior;
    }

    public void setSaldoAnterior(BigDecimal saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getCostoPromedio() {
        return costoPromedio;
    }

    public void setCostoPromedio(BigDecimal costoPromedio) {
        this.costoPromedio = costoPromedio;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
