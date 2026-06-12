package pe.com.ballena.erpalmacen.maestros.productos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Long>, JpaSpecificationExecutor<ProductoEntity> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    @Query("""
            select p
            from ProductoEntity p
            left join fetch p.categoria
            join fetch p.tipoArticulo
            join fetch p.familia
            left join fetch p.subfamilia
            left join fetch p.marca
            join fetch p.unidadMedida
            where p.id = :id
            """)
    java.util.Optional<ProductoEntity> findDetalleById(@Param("id") Long id);
}
