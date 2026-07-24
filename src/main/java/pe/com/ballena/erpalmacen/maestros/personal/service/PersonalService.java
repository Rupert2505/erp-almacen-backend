package pe.com.ballena.erpalmacen.maestros.personal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalCreateRequest;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalResponse;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.personal.entity.PersonalEntity;
import pe.com.ballena.erpalmacen.maestros.personal.repository.PersonalRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class PersonalService {

    private final PersonalRepository personalRepository;

    public PersonalService(PersonalRepository personalRepository) {
        this.personalRepository = personalRepository;
    }

    @Transactional(readOnly = true)
    public Page<PersonalResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return personalRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return personalRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return personalRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return personalRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PersonalResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public PersonalResponse crear(PersonalCreateRequest request) {
        String numeroDocumento = normalizeCode(request.numeroDocumento());
        if (personalRepository.existsByNumeroDocumento(numeroDocumento)) {
            throw new BusinessException("Ya existe personal con el numero de documento indicado");
        }

        PersonalEntity personal = new PersonalEntity();
        applyValues(personal, request);
        personal.setActivo(true);
        return toResponse(personalRepository.save(personal));
    }

    @Transactional
    public PersonalResponse actualizar(Long id, PersonalUpdateRequest request) {
        PersonalEntity personal = findById(id);
        String numeroDocumento = normalizeCode(request.numeroDocumento());
        if (personalRepository.existsByNumeroDocumentoAndIdNot(numeroDocumento, id)) {
            throw new BusinessException("Ya existe personal con el numero de documento indicado");
        }

        applyValues(personal, request);
        return toResponse(personal);
    }

    @Transactional
    public PersonalResponse desactivar(Long id) {
        PersonalEntity personal = findById(id);
        personal.setActivo(false);
        return toResponse(personal);
    }

    @Transactional
    public PersonalResponse activar(Long id) {
        PersonalEntity personal = findById(id);
        personal.setActivo(true);
        return toResponse(personal);
    }

    private void applyValues(PersonalEntity personal, PersonalCreateRequest request) {
        personal.setTipoDocumento(normalizeCode(request.tipoDocumento()));
        personal.setNumeroDocumento(normalizeCode(request.numeroDocumento()));
        personal.setNombres(normalizeText(request.nombres()));
        personal.setApellidos(normalizeText(request.apellidos()));
        personal.setCargo(normalizeText(request.cargo()));
        personal.setArea(normalizeText(request.area()));
        personal.setTelefono(clean(request.telefono()));
        personal.setEmail(normalizeEmail(request.email()));
    }

    private void applyValues(PersonalEntity personal, PersonalUpdateRequest request) {
        personal.setTipoDocumento(normalizeCode(request.tipoDocumento()));
        personal.setNumeroDocumento(normalizeCode(request.numeroDocumento()));
        personal.setNombres(normalizeText(request.nombres()));
        personal.setApellidos(normalizeText(request.apellidos()));
        personal.setCargo(normalizeText(request.cargo()));
        personal.setArea(normalizeText(request.area()));
        personal.setTelefono(clean(request.telefono()));
        personal.setEmail(normalizeEmail(request.email()));
    }

    private PersonalEntity findById(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));
    }

    private PersonalResponse toResponse(PersonalEntity personal) {
        return new PersonalResponse(
                personal.getId(),
                personal.getTipoDocumento(),
                personal.getNumeroDocumento(),
                personal.getNombres(),
                personal.getApellidos(),
                personal.getCargo(),
                personal.getArea(),
                personal.getTelefono(),
                personal.getEmail(),
                personal.isActivo(),
                personal.getCreadoEn(),
                personal.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        String cleanValue = cleanRequired(value);
        return cleanValue == null ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toLowerCase(Locale.ROOT);
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
