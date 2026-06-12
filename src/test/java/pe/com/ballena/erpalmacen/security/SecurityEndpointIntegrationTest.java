package pe.com.ballena.erpalmacen.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.shared.security.JwtService;
import pe.com.ballena.erpalmacen.usuarios.entity.RolEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.RolRepository;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminConTokenPuedeConsultarReportes() throws Exception {
        String token = jwtService.generateToken("admin", List.of("ADMIN"), List.of());

        mockMvc.perform(get("/api/reportes/inventario/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioSinPermisoDevuelve403() throws Exception {
        UsuarioEntity usuario = crearUsuario("consulta", true, "CONSULTA");
        String token = jwtService.generateToken(usuario.getUsername(), List.of("CONSULTA"), List.of());

        mockMvc.perform(get("/api/reportes/inventario/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioDesactivadoConTokenNoDebeAutenticarse() throws Exception {
        UsuarioEntity usuario = crearUsuario("admin_inactivo", false, "ADMIN");
        String token = jwtService.generateToken(usuario.getUsername(), List.of("ADMIN"), List.of("REPORTE_VER"));

        mockMvc.perform(get("/api/reportes/inventario/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private UsuarioEntity crearUsuario(String prefijo, boolean activo, String rolNombre) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        RolEntity rol = rolRepository.findByNombre(rolNombre).orElseThrow();
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(prefijo + "_" + suffix);
        usuario.setPassword(passwordEncoder.encode("Test123*"));
        usuario.setNombres("Usuario");
        usuario.setApellidos("Seguridad");
        usuario.setEmail(prefijo + "_" + suffix + "@demo.com");
        usuario.setActivo(activo);
        usuario.getRoles().add(rol);
        return usuarioRepository.save(usuario);
    }
}
