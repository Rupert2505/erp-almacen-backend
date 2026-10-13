package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity.SaldoInicialImportacionEntity;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.shared.EstadoImportacionSaldoInicial;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface SaldoInicialImportacionRepository extends JpaRepository<SaldoInicialImportacionEntity, Long> {

    List<SaldoInicialImportacionEntity> findByLoteMigracionOrderByCodigoMovimientoExternoAscIdAsc(String loteMigracion);

    List<SaldoInicialImportacionEntity> findByLoteMigracionAndEstadoOrderByCodigoMovimientoExternoAscIdAsc(
            String loteMigracion,
            EstadoImportacionSaldoInicial estado
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from SaldoInicialImportacionEntity s
            where s.loteMigracion = :loteMigracion
              and s.codigoMovimientoExterno = :codigoMovimientoExterno
            order by s.id asc
            """)
    List<SaldoInicialImportacionEntity> findGrupoForUpdate(
            @Param("loteMigracion") String loteMigracion,
            @Param("codigoMovimientoExterno") String codigoMovimientoExterno
    );

    long countByLoteMigracion(String loteMigracion);

    long countByLoteMigracionAndEstado(
            String loteMigracion,
            EstadoImportacionSaldoInicial estado
    );
}
