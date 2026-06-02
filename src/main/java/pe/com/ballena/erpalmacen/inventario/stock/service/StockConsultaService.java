package pe.com.ballena.erpalmacen.inventario.stock.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.stock.dto.StockActualResponse;
import pe.com.ballena.erpalmacen.inventario.stock.dto.StockBajoMinimoResponse;
import pe.com.ballena.erpalmacen.inventario.stock.entity.StockActualEntity;
import pe.com.ballena.erpalmacen.inventario.stock.repository.StockActualRepository;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class StockConsultaService {

    private final StockActualRepository stockActualRepository;

    public StockConsultaService(StockActualRepository stockActualRepository) {
        this.stockActualRepository = stockActualRepository;
    }

    @Transactional(readOnly = true)
    public Page<StockActualResponse> listar(
            Long productoId,
            Long almacenId,
            Long ubicacionId,
            String texto,
            Boolean soloConStock,
            Pageable pageable
    ) {
        return stockActualRepository.findAll(
                buildStockSpecification(productoId, almacenId, ubicacionId, texto, soloConStock),
                pageable
        ).map(this::toStockActualResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockActualResponse> listarPorProducto(Long productoId, Pageable pageable) {
        return listar(productoId, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockActualResponse> listarPorAlmacen(Long almacenId, Pageable pageable) {
        return listar(null, almacenId, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockBajoMinimoResponse> listarBajoMinimo(Long almacenId, Pageable pageable) {
        return stockActualRepository.findAll(buildBajoMinimoSpecification(almacenId), pageable)
                .map(this::toStockBajoMinimoResponse);
    }

    private Specification<StockActualEntity> buildStockSpecification(
            Long productoId,
            Long almacenId,
            Long ubicacionId,
            String texto,
            Boolean soloConStock
    ) {
        String textoNormalizado = normalizeSearch(texto);
        return (root, query, criteriaBuilder) -> {
            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (productoId != null) {
                predicates.add(criteriaBuilder.equal(root.get("producto").get("id"), productoId));
            }
            if (almacenId != null) {
                predicates.add(criteriaBuilder.equal(root.get("almacen").get("id"), almacenId));
            }
            if (ubicacionId != null) {
                predicates.add(criteriaBuilder.equal(root.get("ubicacion").get("id"), ubicacionId));
            }
            if (Boolean.TRUE.equals(soloConStock)) {
                predicates.add(criteriaBuilder.greaterThan(root.get("cantidadActual"), BigDecimal.ZERO));
            }
            if (textoNormalizado != null) {
                String pattern = "%" + textoNormalizado.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("producto").get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("producto").get("nombre")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("almacen").get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("almacen").get("nombre")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ubicacion").get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ubicacion").get("nombre")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<StockActualEntity> buildBajoMinimoSpecification(Long almacenId) {
        return (root, query, criteriaBuilder) -> {
            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(criteriaBuilder.greaterThan(root.get("producto").get("stockMinimo"), BigDecimal.ZERO));
            predicates.add(criteriaBuilder.lessThan(
                    root.get("cantidadActual"),
                    root.get("producto").get("stockMinimo")
            ));
            if (almacenId != null) {
                predicates.add(criteriaBuilder.equal(root.get("almacen").get("id"), almacenId));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private StockActualResponse toStockActualResponse(StockActualEntity stock) {
        ProductoEntity producto = stock.getProducto();
        AlmacenEntity almacen = stock.getAlmacen();
        UbicacionAlmacenEntity ubicacion = stock.getUbicacion();

        return new StockActualResponse(
                stock.getId(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                almacen.getId(),
                almacen.getCodigo(),
                almacen.getNombre(),
                ubicacion == null ? null : ubicacion.getId(),
                ubicacion == null ? null : ubicacion.getCodigo(),
                ubicacion == null ? null : ubicacion.getNombre(),
                stock.getCantidadActual(),
                stock.getCostoPromedio(),
                stock.getFechaUltimoMovimiento(),
                stock.getCreadoEn(),
                stock.getActualizadoEn()
        );
    }

    private StockBajoMinimoResponse toStockBajoMinimoResponse(StockActualEntity stock) {
        ProductoEntity producto = stock.getProducto();
        AlmacenEntity almacen = stock.getAlmacen();
        UbicacionAlmacenEntity ubicacion = stock.getUbicacion();
        BigDecimal diferencia = producto.getStockMinimo().subtract(stock.getCantidadActual());

        return new StockBajoMinimoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                almacen.getId(),
                almacen.getCodigo(),
                almacen.getNombre(),
                ubicacion == null ? null : ubicacion.getId(),
                ubicacion == null ? null : ubicacion.getCodigo(),
                ubicacion == null ? null : ubicacion.getNombre(),
                stock.getCantidadActual(),
                producto.getStockMinimo(),
                diferencia
        );
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
