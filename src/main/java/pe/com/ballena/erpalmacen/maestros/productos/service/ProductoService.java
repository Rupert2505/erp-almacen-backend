package pe.com.ballena.erpalmacen.maestros.productos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.categorias.entity.CategoriaEntity;
import pe.com.ballena.erpalmacen.maestros.categorias.repository.CategoriaRepository;
import pe.com.ballena.erpalmacen.maestros.marcas.entity.MarcaEntity;
import pe.com.ballena.erpalmacen.maestros.marcas.repository.MarcaRepository;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoCreateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoResponse;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            MarcaRepository marcaRepository,
            UnidadMedidaRepository unidadMedidaRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listar(
            String texto,
            Boolean activo,
            Long categoriaId,
            Long marcaId,
            Long unidadMedidaId,
            Pageable pageable
    ) {
        return productoRepository.findAll(buildSpecification(texto, activo, categoriaId, marcaId, unidadMedidaId), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoCreateRequest request) {
        String codigo = normalizeCode(request.codigo());
        if (productoRepository.existsByCodigo(codigo)) {
            throw new BusinessException("Ya existe un producto con el codigo indicado");
        }

        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo(codigo);
        applyValues(producto, request);
        producto.setActivo(true);
        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoUpdateRequest request) {
        ProductoEntity producto = findById(id);
        String codigo = normalizeCode(request.codigo());
        if (productoRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe un producto con el codigo indicado");
        }

        producto.setCodigo(codigo);
        applyValues(producto, request);
        return toResponse(producto);
    }

    @Transactional
    public ProductoResponse desactivar(Long id) {
        ProductoEntity producto = findById(id);
        producto.setActivo(false);
        return toResponse(producto);
    }

    @Transactional
    public ProductoResponse activar(Long id) {
        ProductoEntity producto = findById(id);
        producto.setActivo(true);
        return toResponse(producto);
    }

    private void applyValues(ProductoEntity producto, ProductoCreateRequest request) {
        BigDecimal stockMinimo = request.stockMinimo() == null ? BigDecimal.ZERO : request.stockMinimo();
        validateAmounts(stockMinimo, request.stockMaximo(), request.costoReferencial());

        producto.setNombre(cleanRequired(request.nombre()));
        producto.setDescripcion(clean(request.descripcion()));
        producto.setCategoria(findCategoriaActiva(request.categoriaId()));
        producto.setMarca(findMarcaActiva(request.marcaId()));
        producto.setUnidadMedida(findUnidadMedidaActiva(request.unidadMedidaId()));
        producto.setStockMinimo(stockMinimo);
        producto.setStockMaximo(request.stockMaximo());
        producto.setCostoReferencial(request.costoReferencial());
        producto.setControlaLote(Boolean.TRUE.equals(request.controlaLote()));
        producto.setControlaSerie(Boolean.TRUE.equals(request.controlaSerie()));
    }

    private void applyValues(ProductoEntity producto, ProductoUpdateRequest request) {
        BigDecimal stockMinimo = request.stockMinimo() == null ? BigDecimal.ZERO : request.stockMinimo();
        validateAmounts(stockMinimo, request.stockMaximo(), request.costoReferencial());

        producto.setNombre(cleanRequired(request.nombre()));
        producto.setDescripcion(clean(request.descripcion()));
        producto.setCategoria(findCategoriaActiva(request.categoriaId()));
        producto.setMarca(findMarcaActiva(request.marcaId()));
        producto.setUnidadMedida(findUnidadMedidaActiva(request.unidadMedidaId()));
        producto.setStockMinimo(stockMinimo);
        producto.setStockMaximo(request.stockMaximo());
        producto.setCostoReferencial(request.costoReferencial());
        producto.setControlaLote(Boolean.TRUE.equals(request.controlaLote()));
        producto.setControlaSerie(Boolean.TRUE.equals(request.controlaSerie()));
    }

    private void validateAmounts(BigDecimal stockMinimo, BigDecimal stockMaximo, BigDecimal costoReferencial) {
        if (stockMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El stock minimo no puede ser negativo");
        }
        if (stockMaximo != null && stockMaximo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El stock maximo no puede ser negativo");
        }
        if (costoReferencial != null && costoReferencial.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El costo referencial no puede ser negativo");
        }
        if (stockMaximo != null && stockMaximo.compareTo(stockMinimo) < 0) {
            throw new BusinessException("El stock maximo debe ser mayor o igual al stock minimo");
        }
    }

    private ProductoEntity findById(Long id) {
        return productoRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private Specification<ProductoEntity> buildSpecification(
            String texto,
            Boolean activo,
            Long categoriaId,
            Long marcaId,
            Long unidadMedidaId
    ) {
        String textoNormalizado = normalizeSearch(texto);
        return (root, query, criteriaBuilder) -> {
            if (query != null && ProductoEntity.class.equals(query.getResultType())) {
                root.fetch("categoria", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("marca", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("unidadMedida", jakarta.persistence.criteria.JoinType.INNER);
                query.distinct(true);
            }

            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (textoNormalizado != null) {
                String pattern = "%" + textoNormalizado.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), pattern)
                ));
            }
            if (activo != null) {
                predicates.add(criteriaBuilder.equal(root.get("activo"), activo));
            }
            if (categoriaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (marcaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("marca").get("id"), marcaId));
            }
            if (unidadMedidaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("unidadMedida").get("id"), unidadMedidaId));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private CategoriaEntity findCategoriaActiva(Long id) {
        if (id == null) {
            return null;
        }
        CategoriaEntity categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        if (!categoria.isActivo()) {
            throw new BusinessException("No se puede usar una categoria inactiva");
        }
        return categoria;
    }

    private MarcaEntity findMarcaActiva(Long id) {
        if (id == null) {
            return null;
        }
        MarcaEntity marca = marcaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        if (!marca.isActivo()) {
            throw new BusinessException("No se puede usar una marca inactiva");
        }
        return marca;
    }

    private UnidadMedidaEntity findUnidadMedidaActiva(Long id) {
        UnidadMedidaEntity unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada"));
        if (!unidad.isActivo()) {
            throw new BusinessException("No se puede usar una unidad de medida inactiva");
        }
        return unidad;
    }

    private ProductoResponse toResponse(ProductoEntity producto) {
        CategoriaEntity categoria = producto.getCategoria();
        MarcaEntity marca = producto.getMarca();
        UnidadMedidaEntity unidad = producto.getUnidadMedida();

        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getCodigo(),
                categoria == null ? null : categoria.getNombre(),
                marca == null ? null : marca.getId(),
                marca == null ? null : marca.getCodigo(),
                marca == null ? null : marca.getNombre(),
                unidad.getId(),
                unidad.getCodigo(),
                unidad.getNombre(),
                unidad.getAbreviatura(),
                producto.getStockMinimo(),
                producto.getStockMaximo(),
                producto.getCostoReferencial(),
                producto.isControlaLote(),
                producto.isControlaSerie(),
                producto.isActivo(),
                producto.getCreadoEn(),
                producto.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return cleanRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
