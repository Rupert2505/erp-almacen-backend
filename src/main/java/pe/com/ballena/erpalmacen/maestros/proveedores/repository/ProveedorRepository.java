package pe.com.ballena.erpalmacen.maestros.proveedores.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;

public interface ProveedorRepository extends JpaRepository<ProveedorEntity, Long> {

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);

    Page<ProveedorEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select p
            from ProveedorEntity p
            where lower(p.numeroDocumento) like lower(concat('%', :texto, '%'))
               or lower(p.razonSocial) like lower(concat('%', :texto, '%'))
               or lower(p.nombreComercial) like lower(concat('%', :texto, '%'))
            """)
    Page<ProveedorEntity> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            select p
            from ProveedorEntity p
            where (lower(p.numeroDocumento) like lower(concat('%', :texto, '%'))
                or lower(p.razonSocial) like lower(concat('%', :texto, '%'))
                or lower(p.nombreComercial) like lower(concat('%', :texto, '%')))
              and p.activo = :activo
            """)
    Page<ProveedorEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
