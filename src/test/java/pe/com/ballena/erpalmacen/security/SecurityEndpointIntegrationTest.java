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
import pe.com.ballena.erpalmacen.usuarios.repository.PermisoRepository;
import pe.com.ballena.erpalmacen.usuarios.repository.RolRepository;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private PermisoRepository permisoRepository;

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
    void pdfMovimientoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/reportes/inventario/movimientos/1/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSinPermisoDevuelve403() throws Exception {
        UsuarioEntity usuario = crearUsuarioSinRoles("sin_permiso", true);
        String token = jwtService.generateToken(usuario.getUsername(), List.of(), List.of());

        mockMvc.perform(get("/api/reportes/inventario/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelarMovimientoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/inventario/movimientos/1/cancelar")
                        .contentType("application/json")
                        .content("{\"motivoCancelacion\":\"Error\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelarMovimientoPatchSinTokenDevuelve401() throws Exception {
        mockMvc.perform(patch("/api/inventario/movimientos/1/cancelar")
                        .contentType("application/json")
                        .content("{\"motivo\":\"Error\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelarMovimientoUsuarioSinPermisoDevuelve403() throws Exception {
        UsuarioEntity usuario = crearUsuarioSinRoles("sin_permiso_cancelar", true);
        String token = jwtService.generateToken(usuario.getUsername(), List.of(), List.of());

        mockMvc.perform(post("/api/inventario/movimientos/1/cancelar")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"motivoCancelacion\":\"Error\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearDevolucionProveedorSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/inventario/movimientos/devoluciones-proveedor")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultarDevolucionesProveedorUsuarioSinPermisoDevuelve403() throws Exception {
        UsuarioEntity usuario = crearUsuarioSinRoles("sin_permiso_devolucion", true);
        String token = jwtService.generateToken(usuario.getUsername(), List.of(), List.of());

        mockMvc.perform(get("/api/inventario/movimientos/1/devoluciones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cantidadesDevolucionUsuarioSinPermisoCrearDevuelve403() throws Exception {
        UsuarioEntity usuario = crearUsuario("solo_lectura_devolucion", true, "CONSULTA");
        String token = jwtService.generateToken(usuario.getUsername(), List.of("CONSULTA"), List.of());

        mockMvc.perform(get("/api/inventario/movimientos/1/cantidades-devolucion")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioConInventarioSalidaPuedeListarYCrearPersonalPeroNoEditar() throws Exception {
        RolEntity rolSalida = rolRepository.findByNombre("OPERADOR_ALMACEN").orElseThrow();
        rolSalida.getPermisos().add(permisoRepository.findByCodigo("INVENTARIO_SALIDA").orElseThrow());
        rolRepository.save(rolSalida);
        UsuarioEntity usuario = crearUsuario("operador_salida_personal", true, "OPERADOR_ALMACEN");
        String token = jwtService.generateToken(
                usuario.getUsername(),
                List.of("OPERADOR_ALMACEN"),
                List.of("INVENTARIO_SALIDA")
        );
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String payload = """
                {
                  "tipoDocumento": "DNI",
                  "numeroDocumento": "7%s",
                  "nombres": "Personal salida %s"
                }
                """.formatted(suffix, suffix);

        mockMvc.perform(get("/api/personal?activo=true&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/personal")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/personal/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload))
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
        UsuarioEntity usuario = crearUsuarioSinRoles(prefijo, activo);
        RolEntity rol = rolRepository.findByNombre(rolNombre).orElseThrow();
        usuario.getRoles().add(rol);
        return usuarioRepository.save(usuario);
    }

    private UsuarioEntity crearUsuarioSinRoles(String prefijo, boolean activo) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(prefijo + "_" + suffix);
        usuario.setPassword(passwordEncoder.encode("Test123*"));
        usuario.setNombres("Usuario");
        usuario.setApellidos("Seguridad");
        usuario.setEmail(prefijo + "_" + suffix + "@demo.com");
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }
}
