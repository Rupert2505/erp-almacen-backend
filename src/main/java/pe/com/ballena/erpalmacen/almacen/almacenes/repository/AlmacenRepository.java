package pe.com.ballena.erpalmacen.almacen.almacenes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;

public interface AlmacenRepository extends JpaRepository<AlmacenEntity, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    Page<AlmacenEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select a
            from AlmacenEntity a
            where lower(a.codigo) like lower(concat('%', :texto, '%'))
               or lower(a.nombre) like lower(concat('%', :texto, '%'))
            """)
    Page<AlmacenEntity> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            select a
            from AlmacenEntity a
            where (lower(a.codigo) like lower(concat('%', :texto, '%'))
                or lower(a.nombre) like lower(concat('%', :texto, '%')))
              and a.activo = :activo
            """)
    Page<AlmacenEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
