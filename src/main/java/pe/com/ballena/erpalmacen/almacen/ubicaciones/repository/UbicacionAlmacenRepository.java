package pe.com.ballena.erpalmacen.almacen.ubicaciones.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;

public interface UbicacionAlmacenRepository extends JpaRepository<UbicacionAlmacenEntity, Long> {

    boolean existsByAlmacen_IdAndCodigo(Long almacenId, String codigo);

    boolean existsByAlmacen_IdAndCodigoAndIdNot(Long almacenId, String codigo, Long id);

    Page<UbicacionAlmacenEntity> findByActivo(Boolean activo, Pageable pageable);

    Page<UbicacionAlmacenEntity> findByAlmacen_Id(Long almacenId, Pageable pageable);

    Page<UbicacionAlmacenEntity> findByAlmacen_IdAndActivo(Long almacenId, Boolean activo, Pageable pageable);

    @Query("""
            select u
            from UbicacionAlmacenEntity u
            join fetch u.almacen
            where lower(u.codigo) like lower(concat('%', :texto, '%'))
               or lower(u.nombre) like lower(concat('%', :texto, '%'))
            """)
    Page<UbicacionAlmacenEntity> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            select u
            from UbicacionAlmacenEntity u
            join fetch u.almacen
            where (lower(u.codigo) like lower(concat('%', :texto, '%'))
                or lower(u.nombre) like lower(concat('%', :texto, '%')))
              and u.activo = :activo
            """)
    Page<UbicacionAlmacenEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );

    @Query("""
            select u
            from UbicacionAlmacenEntity u
            join fetch u.almacen
            where (lower(u.codigo) like lower(concat('%', :texto, '%'))
                or lower(u.nombre) like lower(concat('%', :texto, '%')))
              and u.almacen.id = :almacenId
            """)
    Page<UbicacionAlmacenEntity> buscarPorTextoYAlmacen(
            @Param("texto") String texto,
            @Param("almacenId") Long almacenId,
            Pageable pageable
    );

    @Query("""
            select u
            from UbicacionAlmacenEntity u
            join fetch u.almacen
            where (lower(u.codigo) like lower(concat('%', :texto, '%'))
                or lower(u.nombre) like lower(concat('%', :texto, '%')))
              and u.almacen.id = :almacenId
              and u.activo = :activo
            """)
    Page<UbicacionAlmacenEntity> buscarPorTextoAlmacenYActivo(
            @Param("texto") String texto,
            @Param("almacenId") Long almacenId,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
