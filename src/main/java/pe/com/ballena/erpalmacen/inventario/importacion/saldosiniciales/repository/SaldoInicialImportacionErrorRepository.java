package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.entity.SaldoInicialImportacionErrorEntity;

import java.util.List;

public interface SaldoInicialImportacionErrorRepository
        extends JpaRepository<SaldoInicialImportacionErrorEntity, Long> {

    List<SaldoInicialImportacionErrorEntity> findByLoteMigracionOrderByIdAsc(String loteMigracion);

    void deleteByLoteMigracion(String loteMigracion);

    long countByLoteMigracion(String loteMigracion);
}
