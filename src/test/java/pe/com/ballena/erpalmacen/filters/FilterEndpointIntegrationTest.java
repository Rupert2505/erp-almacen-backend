package pe.com.ballena.erpalmacen.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.almacenes.service.AlmacenService;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.service.UbicacionAlmacenService;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoDetalleCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioCreateRequest;
import pe.com.ballena.erpalmacen.inventario.service.MovimientoInventarioService;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.categorias.service.CategoriaService;
import pe.com.ballena.erpalmacen.maestros.familias.dto.FamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.familias.service.FamiliaService;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.marcas.service.MarcaService;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalCreateRequest;
import pe.com.ballena.erpalmacen.maestros.personal.service.PersonalService;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoCreateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.service.ProductoService;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorCreateRequest;
import pe.com.ballena.erpalmacen.maestros.proveedores.service.ProveedorService;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.subfamilias.service.SubfamiliaService;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloCreateRequest;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.service.TipoArticuloService;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.service.UnidadMedidaService;
import pe.com.ballena.erpalmacen.shared.security.JwtService;
import pe.com.ballena.erpalmacen.usuarios.entity.RolEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.RolRepository;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FilterEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private FamiliaService familiaService;

    @Autowired
    private SubfamiliaService subfamiliaService;

    @Autowired
    private MarcaService marcaService;

    @Autowired
    private UnidadMedidaService unidadMedidaService;

    @Autowired
    private TipoArticuloService tipoArticuloService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private PersonalService personalService;

    @Autowired
    private AlmacenService almacenService;

    @Autowired
    private UbicacionAlmacenService ubicacionAlmacenService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @Autowired
    private TipoArticuloRepository tipoArticuloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void endpointsDeMaestrosFiltranPorTextoActivoYRelaciones() throws Exception {
        String suffix = suffix();
        var categoria = categoriaService.crear(new CategoriaCreateRequest("CAT" + suffix, "Categoria Filtro " + suffix, "Catalogo filtro"));
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia Filtro " + suffix, "FF" + suffix.substring(0, 8), "Familia filtro"));
        var subfamilia = subfamiliaService.crear(new SubfamiliaCreateRequest(familia.id(), "Subfamilia Filtro " + suffix, "Subfamilia filtro"));
        var marca = marcaService.crear(new MarcaCreateRequest("MAR" + suffix, "Marca Filtro " + suffix, "Marca filtro"));
        var unidad = unidadMedidaService.crear(new UnidadMedidaCreateRequest("UM" + suffix, "Unidad Filtro " + suffix, "UF" + suffix.substring(0, 4)));
        var tipo = tipoArticuloService.crear(new TipoArticuloCreateRequest("TA" + suffix, "Tipo Filtro " + suffix, "Tipo filtro"));
        var almacen = almacenService.crear(new AlmacenCreateRequest("ALM" + suffix, "Almacen Filtro " + suffix, "Direccion filtro", "Responsable filtro"));
        var ubicacion = ubicacionAlmacenService.crear(new UbicacionAlmacenCreateRequest(almacen.id(), "UB" + suffix, "Ubicacion Filtro " + suffix, "Ubicacion filtro"));

        assertEndpointContains("/api/categorias?texto=filtro " + suffix + "&activo=true&size=10", categoria.id());
        assertEndpointContains("/api/familias?texto=filtro " + suffix + "&activo=true&size=10", familia.id());
        assertEndpointContains("/api/subfamilias?texto=filtro " + suffix + "&activo=true&size=10", subfamilia.id());
        assertEndpointContains("/api/subfamilias/por-familia/" + familia.id() + "?size=10", subfamilia.id());
        assertEndpointContains("/api/marcas?texto=filtro " + suffix + "&activo=true&size=10", marca.id());
        assertEndpointContains("/api/unidades-medida?texto=filtro " + suffix + "&activo=true&size=10", unidad.id());
        assertEndpointContains("/api/tipos-articulo?texto=filtro " + suffix + "&activo=true&size=10", tipo.id());
        assertEndpointContains("/api/almacenes?texto=filtro " + suffix + "&activo=true&size=10", almacen.id());
        assertEndpointContains("/api/ubicaciones?texto=filtro " + suffix + "&activo=true&almacenId=" + almacen.id() + "&size=10", ubicacion.id());
        assertEndpointContains("/api/almacenes/" + almacen.id() + "/ubicaciones?texto=filtro " + suffix + "&activo=true&size=10", ubicacion.id());
    }

    @Test
    void endpointsDeProductosProveedoresPersonalYUsuariosFiltranCorrectamente() throws Exception {
        String suffix = suffix();
        var unidad = unidadMedidaService.crear(new UnidadMedidaCreateRequest("UP" + suffix, "Unidad Producto " + suffix, "UP" + suffix.substring(0, 4)));
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia Producto " + suffix, "FP" + suffix.substring(0, 8), null));
        var marca = marcaService.crear(new MarcaCreateRequest("MP" + suffix, "Marca Producto " + suffix, null));
        var tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();
        var producto = productoService.crear(new ProductoCreateRequest(
                null,
                "Producto Busqueda " + suffix,
                "Descripcion Busqueda " + suffix,
                tipoArticulo.getId(),
                familia.id(),
                null,
                marca.id(),
                unidad.id(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ));
        var proveedor = proveedorService.crear(new ProveedorCreateRequest(
                "RUC",
                "20" + suffix.substring(0, 8),
                "Proveedor Busqueda " + suffix,
                "Comercial Busqueda " + suffix,
                "Direccion",
                "999999999",
                "proveedor" + suffix.toLowerCase() + "@demo.com"
        ));
        var personal = personalService.crear(new PersonalCreateRequest(
                "DNI",
                "10" + suffix.substring(0, 8),
                "Personal Busqueda " + suffix,
                "Apellido",
                "Supervisor",
                "Almacen",
                "999888777",
                "personal" + suffix.toLowerCase() + "@demo.com"
        ));
        UsuarioEntity usuario = crearUsuario("usuario_filtro_" + suffix.toLowerCase());

        assertEndpointContains("/api/productos?texto=" + producto.codigo().toLowerCase() + "&activo=true&tipoArticuloId="
                + tipoArticulo.getId() + "&familiaId=" + familia.id() + "&marcaId=" + marca.id() + "&unidadMedidaId=" + unidad.id() + "&size=10", producto.id());
        assertEndpointContains("/api/productos?texto=busqueda " + suffix.toLowerCase() + "&size=10", producto.id());
        assertEndpointContains("/api/proveedores?search=busqueda " + suffix.toLowerCase() + "&activo=true&size=10", proveedor.id());
        assertEndpointContains("/api/proveedores?q=" + proveedor.numeroDocumento() + "&size=10", proveedor.id());
        assertEndpointContains("/api/personal?term=supervisor&activo=true&size=10", personal.id());
        assertEndpointContains("/api/personal?nombre=personal busqueda " + suffix.toLowerCase() + "&size=10", personal.id());
        assertEndpointContains("/api/usuarios?texto=" + usuario.getUsername() + "&activo=true&rolId="
                + usuario.getRoles().iterator().next().getId() + "&size=10", usuario.getId());
    }

    @Test
    void endpointsDeInventarioStockKardexYReportesFiltranCorrectamente() throws Exception {
        String suffix = suffix();
        var unidad = unidadMedidaService.crear(new UnidadMedidaCreateRequest("UI" + suffix, "Unidad Inventario " + suffix, "UI" + suffix.substring(0, 4)));
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia Inventario " + suffix, "FI" + suffix.substring(0, 8), null));
        var tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();
        var producto = productoService.crear(new ProductoCreateRequest(
                null,
                "Producto Inventario " + suffix,
                "Producto sin ubicacion " + suffix,
                tipoArticulo.getId(),
                familia.id(),
                null,
                null,
                unidad.id(),
                new BigDecimal("100.0000"),
                null,
                null,
                false,
                false
        ));
        var proveedor = proveedorService.crear(new ProveedorCreateRequest(
                "RUC",
                "30" + suffix.substring(0, 8),
                "Proveedor Inventario " + suffix,
                null,
                null,
                null,
                null
        ));
        var almacen = almacenService.crear(new AlmacenCreateRequest("AI" + suffix, "Almacen Inventario " + suffix, null, null));

        var entrada = movimientoInventarioService.crearMovimiento(new MovimientoInventarioCreateRequest(
                TipoMovimientoInventario.ENTRADA_COMPRA,
                LocalDateTime.now().minusHours(1),
                proveedor.id(),
                null,
                null,
                almacen.id(),
                null,
                "DOC-" + suffix,
                "Entrada filtro " + suffix,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                "G001",
                "GR-" + suffix,
                LocalDate.now(),
                "FACTURA",
                "F001",
                "FAC-" + suffix,
                LocalDate.now(),
                "OC-" + suffix,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Documento filtro " + suffix,
                List.of(new MovimientoDetalleCreateRequest(
                        producto.id(),
                        null,
                        null,
                        new BigDecimal("5.0000"),
                        new BigDecimal("3.123456"),
                        "Detalle filtro " + suffix
                ))
        ), authentication());

        assertEndpointContains("/api/inventario/movimientos?tipoMovimiento=ENTRADA_COMPRA&estado=CONFIRMADO&texto="
                + producto.codigo().toLowerCase() + "&fechaDesde=" + LocalDateTime.now().minusDays(1) + "&fechaHasta=" + LocalDateTime.now().plusDays(1) + "&size=10", entrada.id());
        assertEndpointContains("/api/inventario/stock?productoId=" + producto.id() + "&almacenId=" + almacen.id()
                + "&texto=" + producto.codigo().toLowerCase() + "&soloConStock=true&size=10", producto.id());
        assertEndpointContains("/api/inventario/stock/producto/" + producto.id() + "?size=10", producto.id());
        assertEndpointContains("/api/inventario/stock/almacen/" + almacen.id() + "?size=10", producto.id());
        assertEndpointContains("/api/inventario/stock/bajo-minimo?almacenId=" + almacen.id() + "&size=10", producto.id());
        assertEndpointContains("/api/inventario/kardex?productoId=" + producto.id() + "&almacenId=" + almacen.id()
                + "&tipoMovimiento=ENTRADA_COMPRA&texto=" + producto.codigo().toLowerCase() + "&fechaDesde="
                + LocalDateTime.now().minusDays(1) + "&fechaHasta=" + LocalDateTime.now().plusDays(1) + "&size=10", entrada.id());
        assertEndpointContains("/api/inventario/kardex/producto/" + producto.id() + "?size=10", entrada.id());
        assertEndpointContains("/api/inventario/kardex/producto/" + producto.id() + "/almacen/" + almacen.id() + "?size=10", entrada.id());
        assertEndpointContains("/api/inventario/kardex/movimiento/" + entrada.id(), entrada.id());
        assertEndpointContains("/api/reportes/inventario/movimientos?estado=CONFIRMADO&almacenId=" + almacen.id()
                + "&productoId=" + producto.id() + "&size=10", entrada.id());
        assertEndpointContains("/api/reportes/inventario/stock-valorizado?almacenId=" + almacen.id()
                + "&productoId=" + producto.id() + "&soloConStock=true&size=10", producto.id());
        assertEndpointContains("/api/reportes/inventario/entradas-proveedor?proveedorId=" + proveedor.id() + "&size=10", proveedor.id());
        assertEndpointContains("/api/reportes/inventario/productos-bajo-minimo?almacenId=" + almacen.id() + "&size=10", producto.id());
    }

    private void assertEndpointContains(String uri, Long expectedId) throws Exception {
        String body = mockMvc.perform(get(uri)
                        .header("Authorization", "Bearer " + adminToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(responseContainsId(body, expectedId))
                .as("Expected id %s in response for %s. Body: %s", expectedId, uri, body)
                .isTrue();
    }

    private boolean responseContainsId(String body, Long expectedId) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        return containsId(data, expectedId);
    }

    private boolean containsId(JsonNode node, Long expectedId) {
        if (node.isObject()) {
            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                String fieldName = field.getKey();
                if (("id".equals(fieldName) || fieldName.endsWith("Id"))
                        && value.canConvertToLong()
                        && value.asLong() == expectedId) {
                    return true;
                }
            }
            java.util.Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                if (containsId(children.next(), expectedId)) {
                    return true;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsId(child, expectedId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String adminToken() {
        return jwtService.generateToken("admin", List.of("ADMIN"), List.of());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private UsuarioEntity crearUsuario(String username) {
        RolEntity rol = rolRepository.findByNombre("ADMIN").orElseThrow();
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode("Test123*"));
        usuario.setNombres("Usuario Filtro");
        usuario.setApellidos("Endpoint");
        usuario.setEmail(username + "@demo.com");
        usuario.setActivo(true);
        usuario.getRoles().add(rol);
        return usuarioRepository.save(usuario);
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
