package pe.com.ballena.erpalmacen.maestros.familias.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.familias.dto.FamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.familias.dto.FamiliaResponse;
import pe.com.ballena.erpalmacen.maestros.familias.dto.FamiliaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaCorrelativoEntity;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaCorrelativoRepository;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class FamiliaService {

    private final FamiliaRepository familiaRepository;
    private final FamiliaCorrelativoRepository familiaCorrelativoRepository;

    public FamiliaService(
            FamiliaRepository familiaRepository,
            FamiliaCorrelativoRepository familiaCorrelativoRepository
    ) {
        this.familiaRepository = familiaRepository;
        this.familiaCorrelativoRepository = familiaCorrelativoRepository;
    }

    @Transactional(readOnly = true)
    public Page<FamiliaResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return familiaRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return familiaRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return familiaRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return familiaRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<FamiliaResponse> listarActivas(Pageable pageable) {
        return familiaRepository.findByActivo(true, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FamiliaResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public FamiliaResponse crear(FamiliaCreateRequest request) {
        String nombre = cleanRequired(request.nombre());
        String prefijo = normalizePrefix(request.prefijo());
        if (familiaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una familia con el nombre indicado");
        }
        if (familiaRepository.existsByPrefijo(prefijo)) {
            throw new BusinessException("Ya existe una familia con el prefijo indicado");
        }

        FamiliaEntity familia = new FamiliaEntity();
        familia.setNombre(normalizeText(nombre));
        familia.setPrefijo(prefijo);
        familia.setDescripcion(normalizeText(request.descripcion()));
        familia.setActivo(true);
        FamiliaEntity familiaGuardada = familiaRepository.save(familia);

        FamiliaCorrelativoEntity correlativo = new FamiliaCorrelativoEntity();
        correlativo.setFamilia(familiaGuardada);
        correlativo.setUltimoCorrelativo(0L);
        familiaCorrelativoRepository.save(correlativo);

        return toResponse(familiaGuardada);
    }

    @Transactional
    public FamiliaResponse actualizar(Long id, FamiliaUpdateRequest request) {
        FamiliaEntity familia = findById(id);
        String nombre = cleanRequired(request.nombre());
        String prefijo = normalizePrefix(request.prefijo());
        if (familiaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe una familia con el nombre indicado");
        }
        if (familiaRepository.existsByPrefijoAndIdNot(prefijo, id)) {
            throw new BusinessException("Ya existe una familia con el prefijo indicado");
        }

        familia.setNombre(normalizeText(nombre));
        familia.setPrefijo(prefijo);
        familia.setDescripcion(normalizeText(request.descripcion()));
        if (request.activo() != null) {
            familia.setActivo(request.activo());
        }
        return toResponse(familia);
    }

    @Transactional
    public FamiliaResponse desactivar(Long id) {
        FamiliaEntity familia = findById(id);
        familia.setActivo(false);
        return toResponse(familia);
    }

    @Transactional
    public FamiliaResponse activar(Long id) {
        FamiliaEntity familia = findById(id);
        familia.setActivo(true);
        return toResponse(familia);
    }

    private FamiliaEntity findById(Long id) {
        return familiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));
    }

    private FamiliaResponse toResponse(FamiliaEntity familia) {
        return new FamiliaResponse(
                familia.getId(),
                familia.getNombre(),
                familia.getPrefijo(),
                familia.getDescripcion(),
                familia.isActivo(),
                familia.getCreadoEn(),
                familia.getActualizadoEn()
        );
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizePrefix(String value) {
        String prefijo = cleanRequired(value);
        if (prefijo == null || prefijo.isBlank()) {
            throw new BusinessException("El prefijo de la familia es obligatorio");
        }
        prefijo = prefijo.toUpperCase(Locale.ROOT);
        if (!prefijo.matches("^[A-Z0-9]+$")) {
            throw new BusinessException("El prefijo solo puede contener letras y numeros");
        }
        return prefijo;
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
