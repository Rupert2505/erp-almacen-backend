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
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.categorias.service.CategoriaService;
import pe.com.ballena.erpalmacen.maestros.familias.dto.FamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.familias.dto.FamiliaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.familias.service.FamiliaService;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.marcas.service.MarcaService;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalCreateRequest;
import pe.com.ballena.erpalmacen.maestros.personal.dto.PersonalUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.personal.service.PersonalService;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoCreateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoResponse;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.service.ProductoService;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorCreateRequest;
import pe.com.ballena.erpalmacen.maestros.proveedores.service.ProveedorService;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.subfamilias.service.SubfamiliaService;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloCreateRequest;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.service.TipoArticuloService;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.service.UnidadMedidaService;
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
    private CategoriaService categoriaService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private PersonalService personalService;

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
    private UnidadMedidaService unidadMedidaService;

    @Autowired
    private TipoArticuloRepository tipoArticuloRepository;

    @Test
    void productoCrudBusquedaYCamposOpcionalesFuncionan() {
        String suffix = suffix();
        var unidad = crearUnidad(suffix);
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia " + suffix, "F" + suffix.substring(0, 8), "Familia test"));
        var tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();

        var producto = productoService.crear(new ProductoCreateRequest(
                null,
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
        assertThat(producto.codigo()).startsWith(familia.prefijo());
        assertThat(productoService.listar(
                producto.codigo(),
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
                producto.codigo(),
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

        assertThat(producto.nombre()).isEqualTo(("Producto Integral " + suffix).toUpperCase());
        assertThat(producto.descripcion()).isEqualTo(("Descripcion busqueda " + suffix).toUpperCase());
        assertThat(actualizado.nombre()).contains("EDITADO");
        assertThat(actualizado.controlaLote()).isTrue();
        assertThatThrownBy(() -> productoService.actualizar(producto.id(), new ProductoUpdateRequest(
                producto.codigo() + "X",
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
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("codigo");
        assertThat(productoService.desactivar(producto.id()).activo()).isFalse();
    }

    @Test
    void productoGeneraCodigoSecuencialPorPrefijoDeFamilia() {
        String suffix = suffix();
        var unidad = crearUnidad(suffix);
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia Codigo " + suffix, "CG" + suffix.substring(0, 8), null));
        var tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();

        assertThat(productoService.obtenerSiguienteCodigo(familia.id())).isEqualTo(familia.prefijo() + "-1");

        var primero = productoService.crear(new ProductoCreateRequest(
                null,
                "Producto Codigo 1 " + suffix,
                null,
                tipoArticulo.getId(),
                familia.id(),
                null,
                null,
                unidad.getId(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ));
        var segundo = productoService.crear(new ProductoCreateRequest(
                "IGNORADO" + suffix,
                "Producto Codigo 2 " + suffix,
                null,
                tipoArticulo.getId(),
                familia.id(),
                null,
                null,
                unidad.getId(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ));

        assertThat(primero.codigo()).isEqualTo(familia.prefijo() + "-1");
        assertThat(segundo.codigo()).isEqualTo(familia.prefijo() + "-2");
        assertThat(productoService.obtenerSiguienteCodigo(familia.id())).isEqualTo(familia.prefijo() + "-3");

        var familiaActualizada = familiaService.actualizar(familia.id(), new FamiliaUpdateRequest(
                familia.nombre(),
                "OTRO" + suffix.substring(0, 6),
                familia.descripcion(),
                true
        ));

        assertThat(familiaActualizada.prefijo()).startsWith("OTRO");
        assertThat(productoService.obtenerSiguienteCodigo(familia.id())).isEqualTo(familiaActualizada.prefijo() + "-3");
    }

    @Test
    void productosSinOrdenExplicitoDevuelveMasRecientesPrimero() {
        String suffix = suffix();
        var unidad = crearUnidad(suffix);
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia Orden " + suffix, "OR" + suffix.substring(0, 8), null));
        var tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();

        var primero = productoService.crear(new ProductoCreateRequest(
                null,
                "Producto Orden Primero " + suffix,
                "OrdenProducto " + suffix,
                tipoArticulo.getId(),
                familia.id(),
                null,
                null,
                unidad.getId(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ));
        var segundo = productoService.crear(new ProductoCreateRequest(
                null,
                "Producto Orden Segundo " + suffix,
                "OrdenProducto " + suffix,
                tipoArticulo.getId(),
                familia.id(),
                null,
                null,
                unidad.getId(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ));

        var productos = productoService.listar(
                "OrdenProducto " + suffix,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        ).getContent();

        assertThat(productos).extracting(ProductoResponse::id)
                .containsSubsequence(segundo.id(), primero.id());
    }

    @Test
    void productoSinFamiliaOTipoArticuloFalla() {
        String suffix = suffix();
        var unidad = crearUnidad(suffix);
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia " + suffix, "F" + suffix.substring(0, 8), null));
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
        var familia = familiaService.crear(new FamiliaCreateRequest("Familia Dup " + suffix, "FD" + suffix.substring(0, 8), null));
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

        assertThatThrownBy(() -> familiaService.crear(new FamiliaCreateRequest("familia dup " + suffix, "FX" + suffix.substring(0, 8), null)))
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
    void maestrosProductosYAlmacenGuardanTextoDeNegocioEnMayusculas() {
        String suffix = suffix();

        var categoria = categoriaService.crear(new CategoriaCreateRequest(
                "cat" + suffix,
                "categoria mixta " + suffix.toLowerCase(),
                "descripcion categoria"
        ));
        var familia = familiaService.crear(new FamiliaCreateRequest(
                "familia mixta " + suffix.toLowerCase(),
                "FM" + suffix.substring(0, 8).toLowerCase(),
                "descripcion familia"
        ));
        var subfamilia = subfamiliaService.crear(new SubfamiliaCreateRequest(
                familia.id(),
                "subfamilia mixta " + suffix.toLowerCase(),
                "descripcion subfamilia"
        ));
        var marca = marcaService.crear(new MarcaCreateRequest(
                "mar" + suffix,
                "marca mixta " + suffix.toLowerCase(),
                "descripcion marca"
        ));
        var unidad = unidadMedidaService.crear(new UnidadMedidaCreateRequest(
                "um" + suffix,
                "unidad mixta " + suffix.toLowerCase(),
                "um" + suffix.substring(0, 4).toLowerCase()
        ));
        var tipo = tipoArticuloService.crear(new TipoArticuloCreateRequest(
                "ti" + suffix,
                "tipo mixto " + suffix.toLowerCase(),
                "descripcion tipo"
        ));
        var proveedor = proveedorService.crear(new ProveedorCreateRequest(
                "ruc",
                "pe" + suffix.substring(0, 8).toLowerCase(),
                "proveedor mixto " + suffix.toLowerCase(),
                "comercial mixto",
                "av. prueba 123",
                "999111222",
                "Proveedor" + suffix + "@Demo.COM"
        ));
        var almacen = almacenService.crear(new AlmacenCreateRequest(
                "alm" + suffix,
                "almacen mixto " + suffix.toLowerCase(),
                "direccion almacen",
                "responsable almacen"
        ));
        var ubicacion = ubicacionAlmacenService.crear(new UbicacionAlmacenCreateRequest(
                almacen.id(),
                "ub" + suffix,
                "ubicacion mixta " + suffix.toLowerCase(),
                "descripcion ubicacion"
        ));
        var producto = productoService.crear(new ProductoCreateRequest(
                null,
                "producto mixto " + suffix.toLowerCase(),
                "descripcion producto",
                tipo.id(),
                familia.id(),
                subfamilia.id(),
                marca.id(),
                unidad.id(),
                BigDecimal.ZERO,
                null,
                null,
                false,
                false
        ));

        assertThat(categoria.codigo()).isEqualTo(("cat" + suffix).toUpperCase());
        assertThat(categoria.nombre()).isEqualTo(("categoria mixta " + suffix).toUpperCase());
        assertThat(categoria.descripcion()).isEqualTo("DESCRIPCION CATEGORIA");
        assertThat(familia.nombre()).isEqualTo(("familia mixta " + suffix).toUpperCase());
        assertThat(familia.prefijo()).isEqualTo(("FM" + suffix.substring(0, 8)).toUpperCase());
        assertThat(familia.descripcion()).isEqualTo("DESCRIPCION FAMILIA");
        assertThat(subfamilia.nombre()).isEqualTo(("subfamilia mixta " + suffix).toUpperCase());
        assertThat(subfamilia.descripcion()).isEqualTo("DESCRIPCION SUBFAMILIA");
        assertThat(marca.codigo()).isEqualTo(("mar" + suffix).toUpperCase());
        assertThat(marca.nombre()).isEqualTo(("marca mixta " + suffix).toUpperCase());
        assertThat(marca.descripcion()).isEqualTo("DESCRIPCION MARCA");
        assertThat(unidad.codigo()).isEqualTo(("um" + suffix).toUpperCase());
        assertThat(unidad.nombre()).isEqualTo(("unidad mixta " + suffix).toUpperCase());
        assertThat(unidad.abreviatura()).isEqualTo(("um" + suffix.substring(0, 4)).toUpperCase());
        assertThat(tipo.codigo()).isEqualTo(("ti" + suffix).toUpperCase());
        assertThat(tipo.nombre()).isEqualTo(("tipo mixto " + suffix).toUpperCase());
        assertThat(tipo.descripcion()).isEqualTo("DESCRIPCION TIPO");
        assertThat(proveedor.tipoDocumento()).isEqualTo("RUC");
        assertThat(proveedor.numeroDocumento()).isEqualTo(("pe" + suffix.substring(0, 8)).toUpperCase());
        assertThat(proveedor.razonSocial()).isEqualTo(("proveedor mixto " + suffix).toUpperCase());
        assertThat(proveedor.nombreComercial()).isEqualTo("COMERCIAL MIXTO");
        assertThat(proveedor.direccion()).isEqualTo("AV. PRUEBA 123");
        assertThat(proveedor.email()).isEqualTo(("Proveedor" + suffix + "@Demo.COM").toLowerCase());
        assertThat(almacen.codigo()).isEqualTo(("alm" + suffix).toUpperCase());
        assertThat(almacen.nombre()).isEqualTo(("almacen mixto " + suffix).toUpperCase());
        assertThat(almacen.direccion()).isEqualTo("DIRECCION ALMACEN");
        assertThat(almacen.responsable()).isEqualTo("RESPONSABLE ALMACEN");
        assertThat(ubicacion.codigo()).isEqualTo(("ub" + suffix).toUpperCase());
        assertThat(ubicacion.nombre()).isEqualTo(("ubicacion mixta " + suffix).toUpperCase());
        assertThat(ubicacion.descripcion()).isEqualTo("DESCRIPCION UBICACION");
        assertThat(producto.nombre()).isEqualTo(("producto mixto " + suffix).toUpperCase());
        assertThat(producto.descripcion()).isEqualTo("DESCRIPCION PRODUCTO");
    }

    @Test
    void personalCrudBusquedaNormalizacionYDuplicadosFuncionan() {
        String suffix = suffix();
        var personal = personalService.crear(new PersonalCreateRequest(
                "dni",
                "77" + suffix.substring(0, 8).toLowerCase(),
                "Juan Carlos " + suffix.toLowerCase(),
                "Perez Gomez",
                "Supervisor",
                "Almacen Central",
                "999888777",
                "Personal" + suffix + "@Demo.COM"
        ));

        assertThat(personal.tipoDocumento()).isEqualTo("DNI");
        assertThat(personal.numeroDocumento()).isEqualTo("77" + suffix.substring(0, 8));
        assertThat(personal.nombres()).contains("JUAN CARLOS");
        assertThat(personal.apellidos()).isEqualTo("PEREZ GOMEZ");
        assertThat(personal.cargo()).isEqualTo("SUPERVISOR");
        assertThat(personal.area()).isEqualTo("ALMACEN CENTRAL");
        assertThat(personal.email()).isEqualTo(("Personal" + suffix + "@Demo.COM").toLowerCase());
        assertThat(personal.activo()).isTrue();

        assertThat(personalService.listar("juan carlos " + suffix, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(p -> p.id().equals(personal.id()));
        assertThat(personalService.listar(personal.numeroDocumento(), null, PageRequest.of(0, 10)).getContent())
                .anyMatch(p -> p.id().equals(personal.id()));
        assertThat(personalService.listar("supervisor", true, PageRequest.of(0, 10)).getContent())
                .anyMatch(p -> p.id().equals(personal.id()));

        var actualizado = personalService.actualizar(personal.id(), new PersonalUpdateRequest(
                "dni",
                personal.numeroDocumento(),
                "Carlos " + suffix,
                "Lopez",
                "Coordinador",
                "Operaciones",
                "111222333",
                "Nuevo" + suffix + "@Demo.COM"
        ));

        assertThat(actualizado.nombres()).startsWith("CARLOS");
        assertThat(actualizado.area()).isEqualTo("OPERACIONES");
        assertThat(actualizado.email()).isEqualTo(("Nuevo" + suffix + "@Demo.COM").toLowerCase());
        assertThat(personalService.desactivar(personal.id()).activo()).isFalse();
        assertThat(personalService.activar(personal.id()).activo()).isTrue();

        assertThatThrownBy(() -> personalService.crear(new PersonalCreateRequest(
                "DNI",
                personal.numeroDocumento().toLowerCase(),
                "Duplicado",
                null,
                null,
                null,
                null,
                null
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("personal");
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
