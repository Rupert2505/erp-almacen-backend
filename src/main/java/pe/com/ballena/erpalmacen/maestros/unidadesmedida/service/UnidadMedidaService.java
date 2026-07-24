package pe.com.ballena.erpalmacen.maestros.unidadesmedida.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaResponse;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;

    public UnidadMedidaService(UnidadMedidaRepository unidadMedidaRepository) {
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @Transactional(readOnly = true)
    public Page<UnidadMedidaResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return unidadMedidaRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return unidadMedidaRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return unidadMedidaRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return unidadMedidaRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnidadMedidaResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public UnidadMedidaResponse crear(UnidadMedidaCreateRequest request) {
        String codigo = normalizeCode(request.codigo());
        if (unidadMedidaRepository.existsByCodigo(codigo)) {
            throw new BusinessException("Ya existe una unidad de medida con el codigo indicado");
        }

        UnidadMedidaEntity unidad = new UnidadMedidaEntity();
        unidad.setCodigo(codigo);
        unidad.setNombre(normalizeText(request.nombre()));
        unidad.setAbreviatura(normalizeCode(request.abreviatura()));
        unidad.setActivo(true);
        return toResponse(unidadMedidaRepository.save(unidad));
    }

    @Transactional
    public UnidadMedidaResponse actualizar(Long id, UnidadMedidaUpdateRequest request) {
        UnidadMedidaEntity unidad = findById(id);
        String codigo = normalizeCode(request.codigo());
        if (unidadMedidaRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe una unidad de medida con el codigo indicado");
        }

        unidad.setCodigo(codigo);
        unidad.setNombre(normalizeText(request.nombre()));
        unidad.setAbreviatura(normalizeCode(request.abreviatura()));
        return toResponse(unidad);
    }

    @Transactional
    public UnidadMedidaResponse desactivar(Long id) {
        UnidadMedidaEntity unidad = findById(id);
        unidad.setActivo(false);
        return toResponse(unidad);
    }

    @Transactional
    public UnidadMedidaResponse activar(Long id) {
        UnidadMedidaEntity unidad = findById(id);
        unidad.setActivo(true);
        return toResponse(unidad);
    }

    private UnidadMedidaEntity findById(Long id) {
        return unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada"));
    }

    private UnidadMedidaResponse toResponse(UnidadMedidaEntity unidad) {
        return new UnidadMedidaResponse(
                unidad.getId(),
                unidad.getCodigo(),
                unidad.getNombre(),
                unidad.getAbreviatura(),
                unidad.isActivo(),
                unidad.getCreadoEn(),
                unidad.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
