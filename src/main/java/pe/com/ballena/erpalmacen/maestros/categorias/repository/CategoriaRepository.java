package pe.com.ballena.erpalmacen.maestros.categorias.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.categorias.entity.CategoriaEntity;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    Page<CategoriaEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select c
            from CategoriaEntity c
            where lower(c.codigo) like lower(concat('%', :texto, '%'))
               or lower(c.nombre) like lower(concat('%', :texto, '%'))
            """)
    Page<CategoriaEntity> buscarPorTexto(
            @Param("texto") String texto,
            Pageable pageable
    );

    @Query("""
            select c
            from CategoriaEntity c
            where (lower(c.codigo) like lower(concat('%', :texto, '%'))
                or lower(c.nombre) like lower(concat('%', :texto, '%')))
              and c.activo = :activo
            """)
    Page<CategoriaEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
