package pe.com.ballena.erpalmacen.inventario.movimientos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_detalle")
public class MovimientoDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimiento_id", nullable = false)
    private MovimientoInventarioEntity movimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_origen_id")
    private UbicacionAlmacenEntity ubicacionOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_destino_id")
    private UbicacionAlmacenEntity ubicacionDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_detalle_origen_id")
    private MovimientoDetalleEntity movimientoDetalleOrigen;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", precision = 18, scale = 6)
    private BigDecimal costoUnitario;

    @Column(name = "valor_unitario_sin_igv", precision = 18, scale = 6)
    private BigDecimal valorUnitarioSinIgv;

    @Column(name = "tasa_igv", precision = 7, scale = 4)
    private BigDecimal tasaIgv;

    @Column(name = "monto_igv_unitario", precision = 18, scale = 6)
    private BigDecimal montoIgvUnitario;

    @Column(name = "monto_igv_total", precision = 18, scale = 6)
    private BigDecimal montoIgvTotal;

    @Column(name = "precio_unitario_con_igv", precision = 18, scale = 6)
    private BigDecimal precioUnitarioConIgv;

    @Column(name = "igv_recuperable")
    private Boolean igvRecuperable;

    @Column(name = "costo_unitario_inventariable", precision = 18, scale = 6)
    private BigDecimal costoUnitarioInventariable;

    @Column(name = "total_inventariable", precision = 18, scale = 6)
    private BigDecimal totalInventariable;

    @Column(name = "total_con_igv", precision = 18, scale = 6)
    private BigDecimal totalConIgv;

    @Column(name = "total_linea", precision = 18, scale = 6)
    private BigDecimal totalLinea;

    @Column(name = "costo_documento_unitario", precision = 18, scale = 6)
    private BigDecimal costoDocumentoUnitario;

    @Column(name = "valor_stock", precision = 18, scale = 6)
    private BigDecimal valorStock;

    @Column(name = "valor_documento", precision = 18, scale = 6)
    private BigDecimal valorDocumento;

    @Column(name = "diferencia_valorizacion", precision = 18, scale = 6)
    private BigDecimal diferenciaValorizacion;

    @Column(length = 255)
    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = LocalDateTime.now();
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

    public ProductoEntity getProducto() {
        return producto;
    }

    public void setProducto(ProductoEntity producto) {
        this.producto = producto;
    }

    public UbicacionAlmacenEntity getUbicacionOrigen() {
        return ubicacionOrigen;
    }

    public void setUbicacionOrigen(UbicacionAlmacenEntity ubicacionOrigen) {
        this.ubicacionOrigen = ubicacionOrigen;
    }

    public UbicacionAlmacenEntity getUbicacionDestino() {
        return ubicacionDestino;
    }

    public void setUbicacionDestino(UbicacionAlmacenEntity ubicacionDestino) {
        this.ubicacionDestino = ubicacionDestino;
    }

    public MovimientoDetalleEntity getMovimientoDetalleOrigen() {
        return movimientoDetalleOrigen;
    }

    public void setMovimientoDetalleOrigen(MovimientoDetalleEntity movimientoDetalleOrigen) {
        this.movimientoDetalleOrigen = movimientoDetalleOrigen;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getValorUnitarioSinIgv() {
        return valorUnitarioSinIgv;
    }

    public void setValorUnitarioSinIgv(BigDecimal valorUnitarioSinIgv) {
        this.valorUnitarioSinIgv = valorUnitarioSinIgv;
    }

    public BigDecimal getTasaIgv() {
        return tasaIgv;
    }

    public void setTasaIgv(BigDecimal tasaIgv) {
        this.tasaIgv = tasaIgv;
    }

    public BigDecimal getMontoIgvUnitario() {
        return montoIgvUnitario;
    }

    public void setMontoIgvUnitario(BigDecimal montoIgvUnitario) {
        this.montoIgvUnitario = montoIgvUnitario;
    }

    public BigDecimal getMontoIgvTotal() {
        return montoIgvTotal;
    }

    public void setMontoIgvTotal(BigDecimal montoIgvTotal) {
        this.montoIgvTotal = montoIgvTotal;
    }

    public BigDecimal getPrecioUnitarioConIgv() {
        return precioUnitarioConIgv;
    }

    public void setPrecioUnitarioConIgv(BigDecimal precioUnitarioConIgv) {
        this.precioUnitarioConIgv = precioUnitarioConIgv;
    }

    public Boolean getIgvRecuperable() {
        return igvRecuperable;
    }

    public void setIgvRecuperable(Boolean igvRecuperable) {
        this.igvRecuperable = igvRecuperable;
    }

    public BigDecimal getCostoUnitarioInventariable() {
        return costoUnitarioInventariable;
    }

    public void setCostoUnitarioInventariable(BigDecimal costoUnitarioInventariable) {
        this.costoUnitarioInventariable = costoUnitarioInventariable;
    }

    public BigDecimal getTotalInventariable() {
        return totalInventariable;
    }

    public void setTotalInventariable(BigDecimal totalInventariable) {
        this.totalInventariable = totalInventariable;
    }

    public BigDecimal getTotalConIgv() {
        return totalConIgv;
    }

    public void setTotalConIgv(BigDecimal totalConIgv) {
        this.totalConIgv = totalConIgv;
    }

    public BigDecimal getTotalLinea() {
        return totalLinea;
    }

    public void setTotalLinea(BigDecimal totalLinea) {
        this.totalLinea = totalLinea;
    }

    public BigDecimal getCostoDocumentoUnitario() {
        return costoDocumentoUnitario;
    }

    public void setCostoDocumentoUnitario(BigDecimal costoDocumentoUnitario) {
        this.costoDocumentoUnitario = costoDocumentoUnitario;
    }

    public BigDecimal getValorStock() {
        return valorStock;
    }

    public void setValorStock(BigDecimal valorStock) {
        this.valorStock = valorStock;
    }

    public BigDecimal getValorDocumento() {
        return valorDocumento;
    }

    public void setValorDocumento(BigDecimal valorDocumento) {
        this.valorDocumento = valorDocumento;
    }

    public BigDecimal getDiferenciaValorizacion() {
        return diferenciaValorizacion;
    }

    public void setDiferenciaValorizacion(BigDecimal diferenciaValorizacion) {
        this.diferenciaValorizacion = diferenciaValorizacion;
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

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
