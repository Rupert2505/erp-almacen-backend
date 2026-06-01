package pe.com.ballena.erpalmacen.shared.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.usuarios.entity.PermisoEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.RolEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .disabled(!usuario.isActivo())
                .authorities(buildAuthorities(usuario))
                .build();
    }

    private Set<SimpleGrantedAuthority> buildAuthorities(UsuarioEntity usuario) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        for (RolEntity rol : usuario.getRoles()) {
            if (!rol.isActivo()) {
                continue;
            }
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre()));
            for (PermisoEntity permiso : rol.getPermisos()) {
                if (permiso.isActivo()) {
                    authorities.add(new SimpleGrantedAuthority(permiso.getCodigo()));
                }
            }
        }
        return authorities;
    }
}
