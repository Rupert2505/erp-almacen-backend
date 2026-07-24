package pe.com.ballena.erpalmacen.maestros.tiposarticulo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloCreateRequest;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloResponse;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class TipoArticuloService {

    private final TipoArticuloRepository tipoArticuloRepository;

    public TipoArticuloService(TipoArticuloRepository tipoArticuloRepository) {
        this.tipoArticuloRepository = tipoArticuloRepository;
    }

    @Transactional(readOnly = true)
    public Page<TipoArticuloResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return tipoArticuloRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return tipoArticuloRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return tipoArticuloRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return tipoArticuloRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TipoArticuloResponse> listarActivos(Pageable pageable) {
        return tipoArticuloRepository.findByActivo(true, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TipoArticuloResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TipoArticuloResponse crear(TipoArticuloCreateRequest request) {
        String codigo = normalizeCode(request.codigo());
        String nombre = cleanRequired(request.nombre());
        validateUnique(codigo, nombre, null);

        TipoArticuloEntity tipoArticulo = new TipoArticuloEntity();
        tipoArticulo.setCodigo(codigo);
        tipoArticulo.setNombre(normalizeText(nombre));
        tipoArticulo.setDescripcion(normalizeText(request.descripcion()));
        tipoArticulo.setActivo(true);
        return toResponse(tipoArticuloRepository.save(tipoArticulo));
    }

    @Transactional
    public TipoArticuloResponse actualizar(Long id, TipoArticuloUpdateRequest request) {
        TipoArticuloEntity tipoArticulo = findById(id);
        String codigo = normalizeCode(request.codigo());
        String nombre = cleanRequired(request.nombre());
        validateUnique(codigo, nombre, id);

        tipoArticulo.setCodigo(codigo);
        tipoArticulo.setNombre(normalizeText(nombre));
        tipoArticulo.setDescripcion(normalizeText(request.descripcion()));
        if (request.activo() != null) {
            tipoArticulo.setActivo(request.activo());
        }
        return toResponse(tipoArticulo);
    }

    @Transactional
    public TipoArticuloResponse desactivar(Long id) {
        TipoArticuloEntity tipoArticulo = findById(id);
        tipoArticulo.setActivo(false);
        return toResponse(tipoArticulo);
    }

    @Transactional
    public TipoArticuloResponse activar(Long id) {
        TipoArticuloEntity tipoArticulo = findById(id);
        tipoArticulo.setActivo(true);
        return toResponse(tipoArticulo);
    }

    private void validateUnique(String codigo, String nombre, Long id) {
        boolean codigoExists = id == null
                ? tipoArticuloRepository.existsByCodigoIgnoreCase(codigo)
                : tipoArticuloRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id);
        if (codigoExists) {
            throw new BusinessException("Ya existe un tipo de articulo con el codigo indicado");
        }

        boolean nombreExists = id == null
                ? tipoArticuloRepository.existsByNombreIgnoreCase(nombre)
                : tipoArticuloRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id);
        if (nombreExists) {
            throw new BusinessException("Ya existe un tipo de articulo con el nombre indicado");
        }
    }

    private TipoArticuloEntity findById(Long id) {
        return tipoArticuloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de articulo no encontrado"));
    }

    private TipoArticuloResponse toResponse(TipoArticuloEntity tipoArticulo) {
        return new TipoArticuloResponse(
                tipoArticulo.getId(),
                tipoArticulo.getCodigo(),
                tipoArticulo.getNombre(),
                tipoArticulo.getDescripcion(),
                tipoArticulo.isActivo(),
                tipoArticulo.getCreadoEn(),
                tipoArticulo.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return cleanRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
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
