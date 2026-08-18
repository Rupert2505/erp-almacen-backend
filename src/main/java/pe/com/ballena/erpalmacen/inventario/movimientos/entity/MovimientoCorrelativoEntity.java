package pe.com.ballena.erpalmacen.inventario.movimientos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_correlativos")
public class MovimientoCorrelativoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String prefijo;

    @Column(name = "ultimo_correlativo", nullable = false)
    private Long ultimoCorrelativo = 0L;

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

    public String getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(String prefijo) {
        this.prefijo = prefijo;
    }

    public Long getUltimoCorrelativo() {
        return ultimoCorrelativo;
    }

    public void setUltimoCorrelativo(Long ultimoCorrelativo) {
        this.ultimoCorrelativo = ultimoCorrelativo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
