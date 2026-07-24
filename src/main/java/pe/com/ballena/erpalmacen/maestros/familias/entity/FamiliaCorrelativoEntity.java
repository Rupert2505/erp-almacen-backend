package pe.com.ballena.erpalmacen.maestros.familias.entity;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "familia_correlativos")
public class FamiliaCorrelativoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private FamiliaEntity familia;

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

    public FamiliaEntity getFamilia() {
        return familia;
    }

    public void setFamilia(FamiliaEntity familia) {
        this.familia = familia;
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
