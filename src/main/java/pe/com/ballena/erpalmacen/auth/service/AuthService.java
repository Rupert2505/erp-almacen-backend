package pe.com.ballena.erpalmacen.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.auth.dto.AuthUserResponse;
import pe.com.ballena.erpalmacen.auth.dto.LoginRequest;
import pe.com.ballena.erpalmacen.auth.dto.LoginResponse;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.security.JwtService;
import pe.com.ballena.erpalmacen.usuarios.entity.PermisoEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.RolEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.service.UsuarioService;

import java.util.Comparator;
import java.util.List;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioService usuarioService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UsuarioEntity usuario = usuarioService.obtenerPorUsername(request.username());
        if (!usuario.isActivo()) {
            throw new BusinessException("Usuario inactivo", HttpStatus.UNAUTHORIZED);
        }

        AuthUserResponse authUser = buildAuthUserResponse(usuario);
        String token = jwtService.generateToken(authUser.username(), authUser.roles(), authUser.permisos());

        return new LoginResponse(
                token,
                TOKEN_TYPE,
                jwtService.getExpirationSeconds(),
                authUser.username(),
                authUser.nombres(),
                authUser.apellidos(),
                authUser.email(),
                authUser.roles(),
                authUser.permisos()
        );
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Usuario no autenticado");
        }

        UsuarioEntity usuario = usuarioService.obtenerPorUsername(authentication.getName());
        if (!usuario.isActivo()) {
            throw new BusinessException("Usuario inactivo", HttpStatus.UNAUTHORIZED);
        }

        return buildAuthUserResponse(usuario);
    }

    private AuthUserResponse buildAuthUserResponse(UsuarioEntity usuario) {
        List<String> roles = usuario.getRoles().stream()
                .filter(RolEntity::isActivo)
                .map(RolEntity::getNombre)
                .sorted()
                .toList();

        List<String> permisos = usuario.getRoles().stream()
                .filter(RolEntity::isActivo)
                .flatMap(rol -> rol.getPermisos().stream())
                .filter(PermisoEntity::isActivo)
                .map(PermisoEntity::getCodigo)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        return new AuthUserResponse(
                usuario.getUsername(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getEmail(),
                roles,
                permisos
        );
    }
}
