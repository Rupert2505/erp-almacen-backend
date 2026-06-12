package pe.com.ballena.erpalmacen.maestros.subfamilias.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaResponse;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.subfamilias.entity.SubfamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.subfamilias.repository.SubfamiliaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

@Service
public class SubfamiliaService {

    private final SubfamiliaRepository subfamiliaRepository;
    private final FamiliaRepository familiaRepository;

    public SubfamiliaService(SubfamiliaRepository subfamiliaRepository, FamiliaRepository familiaRepository) {
        this.subfamiliaRepository = subfamiliaRepository;
        this.familiaRepository = familiaRepository;
    }

    @Transactional(readOnly = true)
    public Page<SubfamiliaResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return subfamiliaRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return subfamiliaRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return subfamiliaRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return subfamiliaRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubfamiliaResponse> listarActivas(Pageable pageable) {
        return subfamiliaRepository.findByActivo(true, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubfamiliaResponse> listarPorFamilia(Long familiaId, Pageable pageable) {
        return subfamiliaRepository.findByFamiliaIdAndActivo(familiaId, true, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SubfamiliaResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public SubfamiliaResponse crear(SubfamiliaCreateRequest request) {
        FamiliaEntity familia = findFamiliaActiva(request.familiaId());
        String nombre = cleanRequired(request.nombre());
        if (subfamiliaRepository.existsByFamiliaIdAndNombreIgnoreCase(familia.getId(), nombre)) {
            throw new BusinessException("Ya existe una subfamilia con el nombre indicado para la familia");
        }

        SubfamiliaEntity subfamilia = new SubfamiliaEntity();
        subfamilia.setFamilia(familia);
        subfamilia.setNombre(nombre);
        subfamilia.setDescripcion(clean(request.descripcion()));
        subfamilia.setActivo(true);
        return toResponse(subfamiliaRepository.save(subfamilia));
    }

    @Transactional
    public SubfamiliaResponse actualizar(Long id, SubfamiliaUpdateRequest request) {
        SubfamiliaEntity subfamilia = findById(id);
        FamiliaEntity familia = findFamiliaActiva(request.familiaId());
        String nombre = cleanRequired(request.nombre());
        if (subfamiliaRepository.existsByFamiliaIdAndNombreIgnoreCaseAndIdNot(familia.getId(), nombre, id)) {
            throw new BusinessException("Ya existe una subfamilia con el nombre indicado para la familia");
        }

        subfamilia.setFamilia(familia);
        subfamilia.setNombre(nombre);
        subfamilia.setDescripcion(clean(request.descripcion()));
        if (request.activo() != null) {
            subfamilia.setActivo(request.activo());
        }
        return toResponse(subfamilia);
    }

    @Transactional
    public SubfamiliaResponse desactivar(Long id) {
        SubfamiliaEntity subfamilia = findById(id);
        subfamilia.setActivo(false);
        return toResponse(subfamilia);
    }

    @Transactional
    public SubfamiliaResponse activar(Long id) {
        SubfamiliaEntity subfamilia = findById(id);
        subfamilia.setActivo(true);
        return toResponse(subfamilia);
    }

    private FamiliaEntity findFamiliaActiva(Long id) {
        FamiliaEntity familia = familiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));
        if (!familia.isActivo()) {
            throw new BusinessException("No se puede usar una familia inactiva");
        }
        return familia;
    }

    private SubfamiliaEntity findById(Long id) {
        return subfamiliaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subfamilia no encontrada"));
    }

    private SubfamiliaResponse toResponse(SubfamiliaEntity subfamilia) {
        FamiliaEntity familia = subfamilia.getFamilia();
        return new SubfamiliaResponse(
                subfamilia.getId(),
                familia.getId(),
                familia.getNombre(),
                subfamilia.getNombre(),
                subfamilia.getDescripcion(),
                subfamilia.isActivo(),
                subfamilia.getCreadoEn(),
                subfamilia.getActualizadoEn()
        );
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
