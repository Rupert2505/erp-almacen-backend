package pe.com.ballena.erpalmacen.usuarios.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;
import pe.com.ballena.erpalmacen.usuarios.dto.PermisoResponse;
import pe.com.ballena.erpalmacen.usuarios.dto.RolResponse;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioCambiarRolesRequest;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioCreateRequest;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioResetPasswordRequest;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioResponse;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioUpdateRequest;
import pe.com.ballena.erpalmacen.usuarios.entity.PermisoEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.RolEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.PermisoRepository;
import pe.com.ballena.erpalmacen.usuarios.repository.RolRepository;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UsuarioAdminService {

    private static final String ROL_ADMIN = "ADMIN";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioAdminService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PermisoRepository permisoRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String texto, Boolean activo, Long rolId, Pageable pageable) {
        return usuarioRepository.findAll(buildUsuarioSpecification(texto, activo, rolId), pageable)
                .map(this::toUsuarioResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return toUsuarioResponse(obtenerUsuario(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        String username = cleanRequired(request.username());
        String email = cleanRequired(request.email()).toLowerCase(Locale.ROOT);
        validarUsernameDisponible(username);
        validarEmailDisponible(email);

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setNombres(cleanRequired(request.nombres()));
        usuario.setApellidos(cleanRequired(request.apellidos()));
        usuario.setEmail(email);
        usuario.setActivo(request.activo() == null || request.activo());
        usuario.getRoles().addAll(obtenerRolesActivos(request.rolesIds()));

        return toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request, Authentication authentication) {
        UsuarioEntity usuario = obtenerUsuario(id);
        String email = cleanRequired(request.email()).toLowerCase(Locale.ROOT);
        validarEmailDisponibleParaActualizar(email, id);

        if (request.activo() != null && !request.activo() && usuario.isActivo()) {
            validarDesactivacionPermitida(usuario, authentication);
        }

        usuario.setNombres(cleanRequired(request.nombres()));
        usuario.setApellidos(cleanRequired(request.apellidos()));
        usuario.setEmail(email);
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }

        return toUsuarioResponse(usuario);
    }

    @Transactional
    public UsuarioResponse activar(Long id) {
        UsuarioEntity usuario = obtenerUsuario(id);
        usuario.setActivo(true);
        return toUsuarioResponse(usuario);
    }

    @Transactional
    public UsuarioResponse desactivar(Long id, Authentication authentication) {
        UsuarioEntity usuario = obtenerUsuario(id);
        validarDesactivacionPermitida(usuario, authentication);
        usuario.setActivo(false);
        return toUsuarioResponse(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarRoles(Long id, UsuarioCambiarRolesRequest request, Authentication authentication) {
        UsuarioEntity usuario = obtenerUsuario(id);
        Set<RolEntity> roles = obtenerRolesActivos(request.rolesIds());
        if (remueveRolAdmin(usuario, roles)) {
            validarCambioRolAdminPermitido(usuario, authentication);
        }
        usuario.getRoles().clear();
        usuario.getRoles().addAll(roles);
        return toUsuarioResponse(usuario);
    }

    @Transactional
    public UsuarioResponse resetPassword(Long id, UsuarioResetPasswordRequest request) {
        UsuarioEntity usuario = obtenerUsuario(id);
        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        return toUsuarioResponse(usuario);
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return rolRepository.findAll().stream()
                .sorted(Comparator.comparing(RolEntity::getNombre))
                .map(this::toRolResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolResponse obtenerRol(Long id) {
        return rolRepository.findById(id)
                .map(this::toRolResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<PermisoResponse> listarPermisos() {
        return permisoRepository.findAll().stream()
                .sorted(Comparator.comparing(PermisoEntity::getCodigo))
                .map(this::toPermisoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermisoResponse obtenerPermiso(Long id) {
        return permisoRepository.findById(id)
                .map(this::toPermisoResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado"));
    }

    private Specification<UsuarioEntity> buildUsuarioSpecification(String texto, Boolean activo, Long rolId) {
        String textoNormalizado = normalizeSearch(texto);
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (activo != null) {
                predicates.add(criteriaBuilder.equal(root.get("activo"), activo));
            }
            if (rolId != null) {
                Join<UsuarioEntity, RolEntity> rolesJoin = root.join("roles", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(rolesJoin.get("id"), rolId));
            }
            if (textoNormalizado != null) {
                String pattern = "%" + textoNormalizado.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombres")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("apellidos")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private UsuarioEntity obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Set<RolEntity> obtenerRolesActivos(Set<Long> rolesIds) {
        if (rolesIds == null || rolesIds.isEmpty()) {
            throw new BusinessException("Debe asignar al menos un rol");
        }
        List<RolEntity> roles = rolRepository.findByIdIn(rolesIds);
        if (roles.size() != rolesIds.size()) {
            throw new ResourceNotFoundException("Uno o mas roles no existen");
        }
        boolean tieneRolInactivo = roles.stream().anyMatch(rol -> !rol.isActivo());
        if (tieneRolInactivo) {
            throw new BusinessException("No se pueden asignar roles inactivos");
        }
        return new HashSet<>(roles);
    }

    private void validarUsernameDisponible(String username) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new BusinessException("El username ya existe");
        }
    }

    private void validarEmailDisponible(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("El email ya existe");
        }
    }

    private void validarEmailDisponibleParaActualizar(String email, Long id) {
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException("El email ya existe");
        }
    }

    private void validarDesactivacionPermitida(UsuarioEntity usuario, Authentication authentication) {
        if (!esUsuarioAutenticado(usuario, authentication)) {
            return;
        }
        if (esAdmin(usuario) && usuarioRepository.countActiveUsersByRoleName(ROL_ADMIN) <= 1) {
            throw new BusinessException("No se puede desactivar el unico administrador activo");
        }
    }

    private void validarCambioRolAdminPermitido(UsuarioEntity usuario, Authentication authentication) {
        if (!esUsuarioAutenticado(usuario, authentication)) {
            return;
        }
        if (usuario.isActivo() && usuarioRepository.countActiveUsersByRoleName(ROL_ADMIN) <= 1) {
            throw new BusinessException("No se puede quitar el rol ADMIN al unico administrador activo");
        }
    }

    private boolean remueveRolAdmin(UsuarioEntity usuario, Set<RolEntity> nuevosRoles) {
        return esAdmin(usuario) && nuevosRoles.stream().noneMatch(rol -> ROL_ADMIN.equals(rol.getNombre()));
    }

    private boolean esAdmin(UsuarioEntity usuario) {
        return usuario.getRoles().stream().anyMatch(rol -> ROL_ADMIN.equals(rol.getNombre()) && rol.isActivo());
    }

    private boolean esUsuarioAutenticado(UsuarioEntity usuario, Authentication authentication) {
        return authentication != null && usuario.getUsername().equals(authentication.getName());
    }

    private UsuarioResponse toUsuarioResponse(UsuarioEntity usuario) {
        List<RolResponse> roles = usuario.getRoles().stream()
                .sorted(Comparator.comparing(RolEntity::getNombre))
                .map(this::toRolResponse)
                .toList();
        List<PermisoResponse> permisos = usuario.getRoles().stream()
                .filter(RolEntity::isActivo)
                .flatMap(rol -> rol.getPermisos().stream())
                .filter(PermisoEntity::isActivo)
                .distinct()
                .sorted(Comparator.comparing(PermisoEntity::getCodigo))
                .map(this::toPermisoResponse)
                .toList();
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getEmail(),
                usuario.isActivo(),
                roles,
                permisos,
                usuario.getCreadoEn(),
                usuario.getActualizadoEn()
        );
    }

    private RolResponse toRolResponse(RolEntity rol) {
        List<PermisoResponse> permisos = rol.getPermisos().stream()
                .sorted(Comparator.comparing(PermisoEntity::getCodigo))
                .map(this::toPermisoResponse)
                .toList();
        return new RolResponse(
                rol.getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.isActivo(),
                permisos,
                rol.getCreadoEn(),
                rol.getActualizadoEn()
        );
    }

    private PermisoResponse toPermisoResponse(PermisoEntity permiso) {
        return new PermisoResponse(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getDescripcion(),
                permiso.isActivo(),
                permiso.getCreadoEn(),
                permiso.getActualizadoEn()
        );
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String cleanRequired(String value) {
        String cleanValue = clean(value);
        if (cleanValue == null || cleanValue.isBlank()) {
            throw new BusinessException("La solicitud contiene campos obligatorios vacios");
        }
        return cleanValue;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
