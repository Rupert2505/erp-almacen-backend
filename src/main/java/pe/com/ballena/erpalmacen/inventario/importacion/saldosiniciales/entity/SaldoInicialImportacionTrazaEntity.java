package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "importacion_saldos_iniciales_traza")
public class SaldoInicialImportacionTrazaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staging_id", nullable = false)
    private SaldoInicialImportacionEntity staging;

    @Column(name = "lote_migracion", nullable = false, length = 80)
    private String loteMigracion;

    @Column(name = "sistema_origen", length = 80)
    private String sistemaOrigen;

    @Column(name = "id_origen_cabecera", length = 120)
    private String idOrigenCabecera;

    @Column(name = "id_origen_detalle", length = 120)
    private String idOrigenDetalle;

    @Column(name = "codigo_movimiento_externo", nullable = false, length = 100)
    private String codigoMovimientoExterno;

    @Column(name = "linea_origen", nullable = false, length = 80)
    private String lineaOrigen;

    @Column(name = "codigo_producto", nullable = false, length = 50)
    private String codigoProducto;

    @Column(name = "codigo_almacen_destino", nullable = false, length = 30)
    private String codigoAlmacenDestino;

    @Column(name = "codigo_ubicacion_destino", length = 30)
    private String codigoUbicacionDestino;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario_inventariable", precision = 18, scale = 6)
    private BigDecimal costoUnitarioInventariable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimiento_id", nullable = false)
    private MovimientoInventarioEntity movimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimiento_detalle_id", nullable = false)
    private MovimientoDetalleEntity movimientoDetalle;

    @Column(name = "numero_movimiento", nullable = false, length = 30)
    private String numeroMovimiento;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public SaldoInicialImportacionEntity getStaging() {
        return staging;
    }

    public void setStaging(SaldoInicialImportacionEntity staging) {
        this.staging = staging;
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

    public String getIdOrigenDetalle() {
        return idOrigenDetalle;
    }

    public void setIdOrigenDetalle(String idOrigenDetalle) {
        this.idOrigenDetalle = idOrigenDetalle;
    }

    public String getCodigoMovimientoExterno() {
        return codigoMovimientoExterno;
    }

    public void setCodigoMovimientoExterno(String codigoMovimientoExterno) {
        this.codigoMovimientoExterno = codigoMovimientoExterno;
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

    public String getNumeroMovimiento() {
        return numeroMovimiento;
    }

    public void setNumeroMovimiento(String numeroMovimiento) {
        this.numeroMovimiento = numeroMovimiento;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
