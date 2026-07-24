package pe.com.ballena.erpalmacen.maestros.personal.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalCreateRequest;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalResponse;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.personal.service.PersonalService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/personal")
public class PersonalController {

    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PersonalResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(personalService.listar(
                firstNonBlank(texto, search, q, nombre, term),
                activo,
                pageable
        )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PersonalResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personalService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PersonalResponse>> crear(@Valid @RequestBody PersonalCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Personal creado correctamente", personalService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PersonalResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PersonalUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Personal actualizado correctamente", personalService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PersonalResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Personal desactivado correctamente", personalService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PersonalResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Personal activado correctamente", personalService.activar(id)));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
