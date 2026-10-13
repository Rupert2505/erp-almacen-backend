package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity.SaldoInicialImportacionTrazaEntity;

import java.util.List;

public interface SaldoInicialImportacionTrazaRepository
        extends JpaRepository<SaldoInicialImportacionTrazaEntity, Long> {

    List<SaldoInicialImportacionTrazaEntity> findByLoteMigracionOrderByIdAsc(String loteMigracion);

    long countByLoteMigracion(String loteMigracion);
}
