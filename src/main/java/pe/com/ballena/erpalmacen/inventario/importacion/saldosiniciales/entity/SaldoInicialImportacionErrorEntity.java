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

import java.time.LocalDateTime;

@Entity
@Table(name = "importacion_saldos_iniciales_errores")
public class SaldoInicialImportacionErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staging_id")
    private SaldoInicialImportacionEntity staging;

    @Column(name = "lote_migracion", nullable = false, length = 80)
    private String loteMigracion;

    @Column(name = "codigo_movimiento_externo", length = 100)
    private String codigoMovimientoExterno;

    @Column(name = "linea_origen", length = 80)
    private String lineaOrigen;

    @Column(name = "codigo_error", nullable = false, length = 80)
    private String codigoError;

    @Column(nullable = false, length = 500)
    private String mensaje;

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

    public String getCodigoError() {
        return codigoError;
    }

    public void setCodigoError(String codigoError) {
        this.codigoError = codigoError;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
