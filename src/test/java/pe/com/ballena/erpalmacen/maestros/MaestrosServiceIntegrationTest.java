package pe.com.ballena.erpalmacen.maestros;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.almacenes.service.AlmacenService;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.service.UbicacionAlmacenService;
import pe.com.ballena.erpalmacen.maestros.familias.dto.FamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.familias.service.FamiliaService;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.marcas.service.MarcaService;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoCreateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.service.ProductoService;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorCreateRequest;
import pe.com.ballena.erpalmacen.maestros.proveedores.service.ProveedorService;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.subfamilias.service.SubfamiliaService;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloCreateRequest;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.service.TipoArticuloService;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MaestrosServiceIntegrationTest {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private FamiliaService familiaService;

    @Autowired
    private SubfamiliaService subfamiliaService;

    @Autowired
    private MarcaService marcaService;

    @Autowired
    private TipoArticuloService tipoArticuloService;

    @Autowired
    private AlmacenService almacenService;

    @Autowired
    private UbicacionAlmacenService ubicacionAlmacenService;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private TipoArticuloRepository tipoArticuloRepository;

    @Test
    void productoCrudBusquedaYCamposOpcionalesFuncionan() {
        String suffix = suffix();
        var unidad = crearUnidad(suffix);
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia " + suffix, "Familia test"));
        var tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();

        var producto = productoService.crear(new ProductoCreateRequest(
                "PROD" + suffix,
                "Producto Integral " + suffix,
                "Descripcion busqueda " + suffix,
                tipoArticulo.getId(),
                familia.id(),
                null,
                null,
                unidad.getId(),
                new BigDecimal("2.0000"),
                new BigDecimal("20.0000"),
                new BigDecimal("8.5000"),
                false,
                false
        ));

        assertThat(producto.subfamiliaId()).isNull();
        assertThat(producto.marcaId()).isNull();
        assertThat(productoService.listar(
                "PROD" + suffix,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(p -> p.id().equals(producto.id()));
        assertThat(productoService.listar(
                "Integral " + suffix,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(p -> p.id().equals(producto.id()));

        var actualizado = productoService.actualizar(producto.id(), new ProductoUpdateRequest(
                "PROD" + suffix,
                "Producto Integral Editado " + suffix,
                "Descripcion editada",
                tipoArticulo.getId(),
                familia.id(),
                null,
                null,
                unidad.getId(),
                new BigDecimal("1.0000"),
                new BigDecimal("30.0000"),
                new BigDecimal("9.0000"),
                true,
                false
        ));

        assertThat(actualizado.nombre()).contains("Editado");
        assertThat(actualizado.controlaLote()).isTrue();
        assertThat(productoService.desactivar(producto.id()).activo()).isFalse();
    }

    @Test
    void productoSinFamiliaOTipoArticuloFalla() {
        String suffix = suffix();
        var unidad = crearUnidad(suffix);
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia " + suffix, null));
        var tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();

        assertThatThrownBy(() -> productoService.crear(new ProductoCreateRequest(
                "PRODSF" + suffix,
                "Producto sin familia",
                null,
                tipoArticulo.getId(),
                null,
                null,
                null,
                unidad.getId(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("familia");

        assertThatThrownBy(() -> productoService.crear(new ProductoCreateRequest(
                "PRODST" + suffix,
                "Producto sin tipo",
                null,
                null,
                familia.id(),
                null,
                null,
                unidad.getId(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("tipo de articulo");
    }

    @Test
    void familiasSubfamiliasMarcasYTiposArticuloValidanActivosYDuplicados() {
        String suffix = suffix();
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia Dup " + suffix, null));
        var subfamilia = subfamiliaService.crear(new SubfamiliaCreateRequest(
                familia.id(),
                "Subfamilia " + suffix,
                null
        ));
        var marca = marcaService.crear(new MarcaCreateRequest("M" + suffix, "Marca " + suffix, null));
        var tipo = tipoArticuloService.crear(new TipoArticuloCreateRequest("TA" + suffix, "Tipo " + suffix, null));

        assertThat(subfamilia.familiaId()).isEqualTo(familia.id());
        assertThat(marca.activo()).isTrue();
        assertThat(tipoArticuloService.listarActivos(PageRequest.of(0, 20)).getContent())
                .anyMatch(t -> t.id().equals(tipo.id()));
        assertThat(subfamiliaService.listarPorFamilia(familia.id(), PageRequest.of(0, 20)).getContent())
                .anyMatch(s -> s.id().equals(subfamilia.id()));

        assertThatThrownBy(() -> familiaService.crear(new FamiliaCreateRequest("familia dup " + suffix, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("familia");
        assertThatThrownBy(() -> subfamiliaService.crear(new SubfamiliaCreateRequest(
                familia.id(),
                "subfamilia " + suffix,
                null
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("subfamilia");
        assertThatThrownBy(() -> marcaService.crear(new MarcaCreateRequest("M" + suffix, "Otra marca", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("marca");
        assertThatThrownBy(() -> tipoArticuloService.crear(new TipoArticuloCreateRequest("TA" + suffix, "Otro tipo", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tipo de articulo");
    }

    @Test
    void proveedoresBuscanPorRazonDocumentoYNombreComercial() {
        String suffix = suffix();
        var proveedor = proveedorService.crear(new ProveedorCreateRequest(
                "RUC",
                "20" + suffix.substring(0, 8),
                "Proveedor Integral " + suffix,
                "Comercial " + suffix,
                "Direccion",
                "999999999",
                "proveedor" + suffix.toLowerCase() + "@demo.com"
        ));

        assertThat(proveedorService.listar("Integral " + suffix, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(p -> p.id().equals(proveedor.id()));
        assertThat(proveedorService.listar(proveedor.numeroDocumento(), null, PageRequest.of(0, 10)).getContent())
                .anyMatch(p -> p.id().equals(proveedor.id()));
        assertThat(proveedorService.listar("Comercial " + suffix, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(p -> p.id().equals(proveedor.id()));
        assertThatThrownBy(() -> proveedorService.crear(new ProveedorCreateRequest(
                "RUC",
                proveedor.numeroDocumento(),
                "Proveedor duplicado",
                null,
                null,
                null,
                null
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("proveedor");
    }

    @Test
    void almacenesYUbicacionesSeCreanYListanPorAlmacen() {
        String suffix = suffix();
        var almacen = almacenService.crear(new AlmacenCreateRequest(
                "ALM" + suffix,
                "Almacen " + suffix,
                "Direccion",
                "Responsable"
        ));
        var ubicacion = ubicacionAlmacenService.crear(new UbicacionAlmacenCreateRequest(
                almacen.id(),
                "R" + suffix,
                "Rack " + suffix,
                "Rack de prueba"
        ));

        assertThat(ubicacion.almacenId()).isEqualTo(almacen.id());
        assertThat(ubicacionAlmacenService.listarPorAlmacen(
                almacen.id(),
                "Rack " + suffix,
                true,
                PageRequest.of(0, 10)
        ).getContent()).anyMatch(u -> u.id().equals(ubicacion.id()));
    }

    private UnidadMedidaEntity crearUnidad(String suffix) {
        UnidadMedidaEntity unidad = new UnidadMedidaEntity();
        unidad.setCodigo("U" + suffix);
        unidad.setNombre("Unidad " + suffix);
        unidad.setAbreviatura("U" + suffix.substring(0, 4));
        unidad.setActivo(true);
        return unidadMedidaRepository.save(unidad);
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
