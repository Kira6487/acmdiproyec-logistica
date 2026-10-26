package pe.acmdiproyec.logistica.controller.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import pe.acmdiproyec.logistica.dao.CategoriaDao;
import pe.acmdiproyec.logistica.dao.InventarioDao;
import pe.acmdiproyec.logistica.dao.MotivoMovimientoDao;
import pe.acmdiproyec.logistica.dao.MovimientoDao;
import pe.acmdiproyec.logistica.dao.ProductoDao;
import pe.acmdiproyec.logistica.dao.UnidadMedidaDao;
import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.model.Categoria;
import pe.acmdiproyec.logistica.model.Inventario;
import pe.acmdiproyec.logistica.model.MotivoMovimiento;
import pe.acmdiproyec.logistica.model.Movimiento;
import pe.acmdiproyec.logistica.model.Producto;
import pe.acmdiproyec.logistica.model.Rol;
import pe.acmdiproyec.logistica.model.UnidadMedida;
import pe.acmdiproyec.logistica.model.Usuario;
import pe.acmdiproyec.logistica.security.SecurityConfig;

/**
 * Pruebas de los controladores de datos maestros, de sus formularios y de las
 * reglas de autorizacion de {@link SecurityConfig} sobre las rutas de
 * categorias y unidades de medida.
 *
 * No se toca la base de datos: los DAO se sustituyen por dobles y el perfil
 * {@code test} excluye la autoconfiguracion del DataSource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaestrosWebTest {

    private static final String CORREO_ADMIN = "admin@acmdiproyec.test";
    private static final String CORREO_LOGISTICA = "logistica@acmdiproyec.test";
    private static final String CLAVE = "clave-de-prueba-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioDao usuarioDao;

    @MockitoBean
    private CategoriaDao categoriaDao;

    @MockitoBean
    private UnidadMedidaDao unidadMedidaDao;

    @MockitoBean
    private ProductoDao productoDao;

    @MockitoBean
    private MovimientoDao movimientoDao;

    @MockitoBean
    private MotivoMovimientoDao motivoMovimientoDao;

    @MockitoBean
    private InventarioDao inventarioDao;

    @BeforeEach
    void preparar() {
        when(usuarioDao.buscarPorCorreo(CORREO_ADMIN))
                .thenReturn(Optional.of(usuario("ADMIN", CORREO_ADMIN)));
        when(usuarioDao.buscarPorCorreo(CORREO_LOGISTICA))
                .thenReturn(Optional.of(usuario("LOGISTICA", CORREO_LOGISTICA)));
        when(usuarioDao.listarRolesActivos()).thenReturn(List.of());
    }

    // ------------------------------------------------- acceso y autorizacion

    @Test
    void adminPuedeVerLosListados() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/categorias").session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/categorias/lista"));
        mockMvc.perform(get("/unidades-medida").session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/unidades-medida/lista"));
    }

    @Test
    void logisticaPuedeVerLosListados() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/categorias").session(sesion)).andExpect(status().isOk());
        mockMvc.perform(get("/unidades-medida").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void losAnonimosSonRedirigidosAlLogin() throws Exception {
        mockMvc.perform(get("/categorias"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/unidades-medida"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logisticaNoPuedeAccederAFormularios() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/categorias/nuevo").session(sesion)).andExpect(status().isForbidden());
        mockMvc.perform(get("/categorias/1/editar").session(sesion)).andExpect(status().isForbidden());
        mockMvc.perform(get("/unidades-medida/nuevo").session(sesion)).andExpect(status().isForbidden());
        mockMvc.perform(get("/unidades-medida/1/editar").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void logisticaNoPuedeModificarRegistros() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(post("/categorias").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/categorias/1").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/categorias/1/eliminar").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/unidades-medida").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/unidades-medida/1/eliminar").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        verify(categoriaDao, never()).guardar(any());
        verify(unidadMedidaDao, never()).guardar(any());
    }

    @Test
    void elListadoOcultaLasAccionesALaLogistica() throws Exception {
        when(categoriaDao.listarTodas()).thenReturn(List.of(categoria(1L, "DADOS", "Dados")));

        String comoAdmin = contenido(get("/categorias").session(iniciarSesion(CORREO_ADMIN)));
        String comoLogistica = contenido(get("/categorias").session(iniciarSesion(CORREO_LOGISTICA)));

        assertThat(comoAdmin).contains("Editar").contains("Eliminar").contains("Nueva categoría");
        assertThat(comoLogistica)
                .doesNotContain("Editar")
                .doesNotContain("Eliminar")
                .doesNotContain("Nueva categoría");
    }

    // ------------------------------------------------------------- escritura

    @Test
    void adminPuedeCrearUnaCategoria() throws Exception {
        when(categoriaDao.guardar(any(Categoria.class))).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/categorias").session(sesion)
                        .param("codigo", "DADOS")
                        .param("nombre", "Dados de concreto")
                        .param("descripcion", "Prefabricados")
                        .param("activo", "true")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"));

        verify(categoriaDao).guardar(any(Categoria.class));
    }

    @Test
    void adminPuedeActualizarUnaCategoria() throws Exception {
        when(categoriaDao.buscarPorId(1L)).thenReturn(Optional.of(categoria(1L, "DADOS", "Dados")));
        when(categoriaDao.actualizar(any(Categoria.class))).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/categorias/1").session(sesion)
                        .param("codigo", "DADOS-2")
                        .param("nombre", "Dados 2")
                        .param("activo", "true")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"));

        verify(categoriaDao).actualizar(any(Categoria.class));
    }

    @Test
    void adminPuedeEliminarUnaCategoria() throws Exception {
        when(categoriaDao.buscarPorId(1L)).thenReturn(Optional.of(categoria(1L, "DADOS", "Dados")));
        when(categoriaDao.contarProductosAsociados(1L)).thenReturn(0L);
        when(categoriaDao.eliminar(1L)).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/categorias/1/eliminar").session(sesion)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"));

        verify(categoriaDao).eliminar(1L);
    }

    @Test
    void adminPuedeCrearUnaUnidadDeMedida() throws Exception {
        when(unidadMedidaDao.guardar(any(UnidadMedida.class))).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/unidades-medida").session(sesion)
                        .param("codigo", "UNIDAD")
                        .param("nombre", "Unidad")
                        .param("abreviatura", "und")
                        .param("activo", "true")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/unidades-medida"));

        verify(unidadMedidaDao).guardar(any(UnidadMedida.class));
    }

    // ------------------------------------------------------ validacion y CSRF

    @Test
    void elFormularioMuestraLosErroresDeValidacion() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        // Un envio con campos obligatorios vacios debe volver a la vista del
        // formulario mostrando el mensaje junto a cada campo.
        MvcResult resultado = mockMvc.perform(post("/categorias").session(sesion)
                        .param("codigo", "   ")
                        .param("nombre", "")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/categorias/formulario"))
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("El codigo es obligatorio.");
        assertThat(html).contains("El nombre es obligatorio.");
        verify(categoriaDao, never()).guardar(any());
    }

    @Test
    void lasOperacionesDeEscrituraRespetanCsrf() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/categorias").session(sesion)
                        .param("codigo", "DADOS")
                        .param("nombre", "Dados"))
                .andExpect(status().isForbidden());
        verify(categoriaDao, never()).guardar(any());
    }

    // -------------------------------------------- identificadores y reglas

    @Test
    void elIdentificadorInexistenteRedirigeConMensaje() throws Exception {
        when(categoriaDao.buscarPorId(99L)).thenReturn(Optional.empty());
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(get("/categorias/99/editar").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("no existe");
    }

    @Test
    void laEliminacionBloqueadaMuestraUnMensaje() throws Exception {
        when(categoriaDao.buscarPorId(1L)).thenReturn(Optional.of(categoria(1L, "DADOS", "Dados")));
        when(categoriaDao.contarProductosAsociados(1L)).thenReturn(4L);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/categorias/1/eliminar").session(sesion)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("productos asociados");
        verify(categoriaDao, never()).eliminar(anyLong());
    }

    @Test
    void elListadoMuestraElEstadoVacio() throws Exception {
        when(categoriaDao.listarTodas()).thenReturn(List.of());

        String html = contenido(get("/categorias").session(iniciarSesion(CORREO_ADMIN)));

        assertThat(html).contains("No hay categorías registradas");
    }

    @Test
    void laUnidadInexistenteRedirigeConMensaje() throws Exception {
        when(unidadMedidaDao.buscarPorId(99L)).thenReturn(Optional.empty());
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(get("/unidades-medida/99/editar").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/unidades-medida"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("no existe");
    }

    // ------------------------------------------------------- modulo productos

    @Test
    void adminPuedeVerElListadoDeProductos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/productos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/productos/lista"));
    }

    @Test
    void logisticaPuedeVerElListadoDeProductos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/productos").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void losAnonimosSonRedirigidosAlLoginEnProductos() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logisticaNoPuedeAccederAFormulariosDeProductos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/productos/nuevo").session(sesion)).andExpect(status().isForbidden());
        mockMvc.perform(get("/productos/1/editar").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void logisticaNoPuedeModificarProductos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(post("/productos").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/productos/1").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/productos/1/eliminar").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        verify(productoDao, never()).guardar(any());
    }

    @Test
    void adminPuedeCrearUnProducto() throws Exception {
        prepararRelaciones();
        when(productoDao.guardar(any(Producto.class))).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/productos").session(sesion)
                        .param("codigo", "DADO-2CM")
                        .param("nombre", "Dado separador de 2 cm")
                        .param("idCategoria", "1")
                        .param("idUnidadMedida", "1")
                        .param("stockMinimo", "20.000")
                        .param("descripcion", "Prefabricado")
                        .param("activo", "true")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos"));

        verify(productoDao).guardar(any(Producto.class));
    }

    @Test
    void adminPuedeActualizarUnProducto() throws Exception {
        prepararRelaciones();
        when(productoDao.buscarPorId(1L)).thenReturn(Optional.of(producto(1L)));
        when(productoDao.actualizar(any(Producto.class))).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/productos/1").session(sesion)
                        .param("codigo", "DADO-3CM")
                        .param("nombre", "Dado separador de 3 cm")
                        .param("idCategoria", "1")
                        .param("idUnidadMedida", "1")
                        .param("stockMinimo", "15.000")
                        .param("activo", "true")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos"));

        verify(productoDao).actualizar(any(Producto.class));
    }

    @Test
    void adminPuedeEliminarUnProducto() throws Exception {
        when(productoDao.buscarPorId(1L)).thenReturn(Optional.of(producto(1L)));
        when(productoDao.contarMovimientosAsociados(1L)).thenReturn(0L);
        when(productoDao.obtenerStockActual(1L)).thenReturn(java.math.BigDecimal.ZERO);
        when(productoDao.eliminar(1L)).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/productos/1/eliminar").session(sesion)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos"));

        verify(productoDao).eliminar(1L);
    }

    @Test
    void elFormularioDeProductoMuestraLosErroresDeValidacion() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/productos").session(sesion)
                        .param("codigo", "   ")
                        .param("nombre", "")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/productos/formulario"))
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("El codigo es obligatorio.");
        assertThat(html).contains("El nombre es obligatorio.");
        assertThat(html).contains("La categoria es obligatoria.");
        assertThat(html).contains("La unidad de medida es obligatoria.");
        verify(productoDao, never()).guardar(any());
    }

    @Test
    void elFormularioDeProductoTraduceValoresNumericosInvalidos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/productos").session(sesion)
                        .param("codigo", "DADO-2CM")
                        .param("nombre", "Dado separador de 2 cm")
                        .param("idCategoria", "1")
                        .param("idUnidadMedida", "1")
                        .param("stockMinimo", "no-es-un-numero")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/productos/formulario"))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).contains("El stock minimo");
        verify(productoDao, never()).guardar(any());
    }

    @Test
    void elFormularioDeProductoRechazaUnaCategoriaInexistente() throws Exception {
        prepararRelaciones();
        when(categoriaDao.buscarPorId(1L)).thenReturn(Optional.empty());
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/productos").session(sesion)
                        .param("codigo", "DADO-2CM")
                        .param("nombre", "Dado separador de 2 cm")
                        .param("idCategoria", "1")
                        .param("idUnidadMedida", "1")
                        .param("stockMinimo", "20.000")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/productos/formulario"))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).contains("La categoria indicada no existe.");
        verify(productoDao, never()).guardar(any());
    }

    @Test
    void laEliminacionDeProductoBloqueadaMuestraUnMensaje() throws Exception {
        when(productoDao.buscarPorId(1L)).thenReturn(Optional.of(producto(1L)));
        when(productoDao.contarMovimientosAsociados(1L)).thenReturn(3L);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/productos/1/eliminar").session(sesion)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("movimientos registrados");
        verify(productoDao, never()).eliminar(anyLong());
    }

    @Test
    void elProductoInexistenteRedirigeConMensaje() throws Exception {
        when(productoDao.buscarPorId(99L)).thenReturn(Optional.empty());
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(get("/productos/99/editar").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("no existe");
    }

    @Test
    void elListadoDeProductosOcultaLasAccionesALaLogistica() throws Exception {
        when(productoDao.listarTodos()).thenReturn(List.of(producto(1L)));

        String comoAdmin = contenido(get("/productos").session(iniciarSesion(CORREO_ADMIN)));
        String comoLogistica = contenido(get("/productos").session(iniciarSesion(CORREO_LOGISTICA)));

        assertThat(comoAdmin).contains("Editar").contains("Eliminar").contains("Nuevo producto");
        assertThat(comoLogistica)
                .doesNotContain("Editar")
                .doesNotContain("Eliminar")
                .doesNotContain("Nuevo producto");
    }

    @Test
    void elFormularioDeProductoOfreceLasCategoriasYLasUnidades() throws Exception {
        prepararRelaciones();
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        String html = contenido(get("/productos/nuevo").session(sesion));

        assertThat(html).contains("Seleccione una categoría");
        assertThat(html).contains("Seleccione una unidad");
    }

    // --------------------------------------------- modulo movimientos/inventario

    @Test
    void adminPuedeVerElListadoDeMovimientos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/movimientos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/lista"));
    }

    @Test
    void logisticaPuedeVerElListadoDeMovimientos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/movimientos").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void losAnonimosSonRedirigidosAlLoginEnMovimientos() throws Exception {
        mockMvc.perform(get("/movimientos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/inventario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logisticaNoPuedeAccederAlFormularioDeMovimientos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/movimientos/nuevo").param("tipo", "ENTRADA").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    void logisticaNoPuedeRegistrarMovimientos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(post("/movimientos").session(sesion)
                        .param("tipo", "ENTRADA")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        verify(movimientoDao, never()).guardar(any());
    }

    @Test
    void adminPuedeRegistrarUnMovimiento() throws Exception {
        prepararMovimiento();
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(55L);
        when(movimientoDao.guardarDetalle(any())).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/movimientos").session(sesion)
                        .principal(() -> CORREO_ADMIN)
                        .param("tipo", "ENTRADA")
                        .param("idMotivoMovimiento", "1")
                        .param("lineas[0].idProducto", "1")
                        .param("lineas[0].cantidad", "10.000")
                        .param("observacion", "Ingreso por compra")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos/55"));

        verify(movimientoDao).guardar(any(Movimiento.class));
        verify(movimientoDao).guardarDetalle(any());
    }

    @Test
    void elFormularioDeMovimientoMuestraLosErroresDeValidacion() throws Exception {
        prepararMovimiento();
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/movimientos").session(sesion)
                        .principal(() -> CORREO_ADMIN)
                        .param("tipo", "ENTRADA")
                        .param("lineas[0].idProducto", "")
                        .param("lineas[0].cantidad", "")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/formulario"))
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("El motivo del movimiento es obligatorio.");
        verify(movimientoDao, never()).guardar(any());
    }

    @Test
    void elMovimientoInexistenteRedirigeConMensaje() throws Exception {
        when(movimientoDao.buscarPorId(99L)).thenReturn(Optional.empty());
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(get("/movimientos/99").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("no existe");
    }

    @Test
    void elListadoDeMovimientosOcultaElRegistroALaLogistica() throws Exception {
        when(movimientoDao.listarTodos(any(), any(), any(), any(), any()))
                .thenReturn(List.of(movimiento()));

        String comoAdmin = contenido(get("/movimientos").session(iniciarSesion(CORREO_ADMIN)));
        String comoLogistica = contenido(get("/movimientos").session(iniciarSesion(CORREO_LOGISTICA)));

        assertThat(comoAdmin).contains("Registrar entrada").contains("Registrar salida");
        assertThat(comoLogistica)
                .doesNotContain("Registrar entrada")
                .doesNotContain("Registrar salida");
    }

    @Test
    void ambosRolesPuedenConsultarElInventario() throws Exception {
        when(inventarioDao.listarTodo()).thenReturn(List.of(inventario()));
        when(categoriaDao.listarTodas()).thenReturn(List.of(categoria(1L, "DADOS", "Dados")));

        String comoAdmin = contenido(get("/inventario").session(iniciarSesion(CORREO_ADMIN)));
        String comoLogistica = contenido(get("/inventario").session(iniciarSesion(CORREO_LOGISTICA)));

        assertThat(comoAdmin).contains("Existencias actuales");
        assertThat(comoLogistica).contains("Existencias actuales");
    }

    // ------------------------------------------------- anulacion de movimientos

    @Test
    void elDetalleDeUnMovimientoVigenteMuestraLaAccionDeAnular() throws Exception {
        when(movimientoDao.buscarPorId(1L)).thenReturn(Optional.of(movimiento()));
        when(movimientoDao.listarDetalles(1L)).thenReturn(List.of());

        String html = contenido(get("/movimientos/1").session(iniciarSesion(CORREO_ADMIN)));

        assertThat(html).contains("Anular movimiento").contains("Motivo de la anulación");
    }

    @Test
    void elDetalleDeUnMovimientoAnuladoNoPermiteAnularDeNuevo() throws Exception {
        Movimiento anulado = movimiento();
        anulado.setEstado("ANULADO");
        anulado.setFechaAnulacion(java.time.OffsetDateTime.now());
        anulado.setMotivoAnulacion("Error de captura");
        anulado.setUsuarioAnulacion(usuario(CORREO_ADMIN, CORREO_ADMIN));
        when(movimientoDao.buscarPorId(1L)).thenReturn(Optional.of(anulado));
        when(movimientoDao.listarDetalles(1L)).thenReturn(List.of());

        String html = contenido(get("/movimientos/1").session(iniciarSesion(CORREO_ADMIN)));

        assertThat(html).doesNotContain("Anular movimiento");
    }

    @Test
    void logisticaNoPuedeAnularMovimientos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(post("/movimientos/1/anular").session(sesion)
                        .principal(() -> CORREO_LOGISTICA)
                        .param("motivoAnulacion", "Motivo")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        verify(movimientoDao, never()).anular(anyLong(), anyLong(), any());
    }

    @Test
    void adminPuedeAnularUnMovimiento() throws Exception {
        when(movimientoDao.buscarPorId(1L)).thenReturn(Optional.of(movimiento()));
        when(movimientoDao.anular(1L, 1L, "Error de captura")).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/movimientos/1/anular").session(sesion)
                        .principal(() -> CORREO_ADMIN)
                        .param("motivoAnulacion", "Error de captura")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos/1"));

        verify(movimientoDao).anular(1L, 1L, "Error de captura");
    }

    @Test
    void laAnulacionSinMotivoMuestraLosErroresDeValidacion() throws Exception {
        when(movimientoDao.buscarPorId(1L)).thenReturn(Optional.of(movimiento()));
        when(movimientoDao.listarDetalles(1L)).thenReturn(List.of());
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/movimientos/1/anular").session(sesion)
                        .principal(() -> CORREO_ADMIN)
                        .param("motivoAnulacion", "   ")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/detalle"))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .contains("El motivo de la anulacion es obligatorio.");
        verify(movimientoDao, never()).anular(anyLong(), anyLong(), any());
    }

    @Test
    void laAnulacionRequiereTokenCsrf() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/movimientos/1/anular").session(sesion)
                        .principal(() -> CORREO_ADMIN)
                        .param("motivoAnulacion", "Motivo"))
                .andExpect(status().isForbidden());
        verify(movimientoDao, never()).anular(anyLong(), anyLong(), any());
    }

    @Test
    void unaAnulacionYaHechaMuestraUnMensajeDeError() throws Exception {
        when(movimientoDao.buscarPorId(1L)).thenReturn(Optional.of(movimiento()));
        when(movimientoDao.anular(anyLong(), anyLong(), any())).thenReturn(0);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/movimientos/1/anular").session(sesion)
                        .principal(() -> CORREO_ADMIN)
                        .param("motivoAnulacion", "Motivo")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos/1"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("dos veces");
    }

    @Test
    void elHistorialMuestraLosDatosDeLaAnulacion() throws Exception {
        Movimiento anulado = movimiento();
        anulado.setEstado("ANULADO");
        anulado.setFechaAnulacion(java.time.OffsetDateTime.now());
        anulado.setMotivoAnulacion("Error de captura");
        anulado.setUsuarioAnulacion(usuario(CORREO_ADMIN, CORREO_ADMIN));
        when(movimientoDao.listarTodos(any(), any(), any(), any(), any()))
                .thenReturn(List.of(anulado));

        String html = contenido(get("/movimientos").session(iniciarSesion(CORREO_ADMIN)));

        assertThat(html).contains("Anulado el").contains("Error de captura");
    }

    @Test
    void losFiltrosPorEstadoSiguenFuncionando() throws Exception {
        when(movimientoDao.listarTodos(any(), any(), any(), any(), any()))
                .thenReturn(List.of(movimiento()));
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/movimientos").session(sesion).param("estado", "ANULADO"))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/lista"));

        verify(movimientoDao).listarTodos(any(), any(), eq("ANULADO"), any(), any());
    }

    // --------------------------------------------- modulo motivos de movimiento

    @Test
    void adminPuedeVerElListadoDeMotivos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/motivos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/motivos/lista"));
    }

    @Test
    void logisticaPuedeVerElListadoDeMotivos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/motivos").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void logisticaNoPuedeModificarMotivos() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/motivos/nuevo").session(sesion)).andExpect(status().isForbidden());
        mockMvc.perform(post("/motivos").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/motivos/1/eliminar").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isForbidden());
        verify(motivoMovimientoDao, never()).guardar(any());
    }

    @Test
    void adminPuedeCrearUnMotivo() throws Exception {
        when(motivoMovimientoDao.guardar(any())).thenReturn(1);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/motivos").session(sesion)
                        .param("codigo", "ENTRADA_COMPRA")
                        .param("nombre", "Compra")
                        .param("tipo", "ENTRADA")
                        .param("descripcion", "Ingreso por compra")
                        .param("activo", "true")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motivos"));

        verify(motivoMovimientoDao).guardar(any());
    }

    @Test
    void elFormularioDeMotivoMuestraLosErroresDeValidacion() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/motivos").session(sesion)
                        .param("codigo", "   ")
                        .param("nombre", "")
                        .param("tipo", "OTRO")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().isOk())
                .andExpect(view().name("maestros/motivos/formulario"))
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("El codigo es obligatorio.");
        assertThat(html).contains("El nombre es obligatorio.");
        verify(motivoMovimientoDao, never()).guardar(any());
    }

    @Test
    void laEliminacionDeMotivoBloqueadaMuestraUnMensaje() throws Exception {
        when(motivoMovimientoDao.buscarPorId(1L)).thenReturn(Optional.of(motivoMovimiento()));
        when(motivoMovimientoDao.contarMovimientosAsociados(1L)).thenReturn(5L);
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(post("/motivos/1/eliminar").session(sesion)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motivos"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("movimientos registrados");
        verify(motivoMovimientoDao, never()).eliminar(anyLong());
    }

    @Test
    void elMotivoInexistenteRedirigeConMensaje() throws Exception {
        when(motivoMovimientoDao.buscarPorId(99L)).thenReturn(Optional.empty());
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        MvcResult resultado = mockMvc.perform(get("/motivos/99/editar").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motivos"))
                .andReturn();

        assertThat(mensajeDeError(resultado)).contains("no existe");
    }

    @Test
    void elListadoDeMotivosOcultaLasAccionesALaLogistica() throws Exception {
        when(motivoMovimientoDao.listarTodos()).thenReturn(List.of(motivoMovimiento()));

        String comoAdmin = contenido(get("/motivos").session(iniciarSesion(CORREO_ADMIN)));
        String comoLogistica = contenido(get("/motivos").session(iniciarSesion(CORREO_LOGISTICA)));

        assertThat(comoAdmin).contains("Editar").contains("Eliminar").contains("Nuevo motivo");
        assertThat(comoLogistica)
                .doesNotContain("Editar")
                .doesNotContain("Eliminar")
                .doesNotContain("Nuevo motivo");
    }

    // ------------------------------------------- manejo de errores del servidor

    @Test
    void unaRutaInexistenteDebeResponderNotFoundYNo500() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/ruta-que-no-existe").session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    void unIdentificadorMalFormadoEnLaRutaDebeResponderBadRequest() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/movimientos/abc").session(sesion))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unParametroObligatorioAusenteDebeResponderBadRequest() throws Exception {
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        // /movimientos/nuevo exige el parametro "tipo".
        mockMvc.perform(get("/movimientos/nuevo").session(sesion))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------- auxiliares

    private String contenido(org.springframework.test.web.servlet.RequestBuilder solicitud) throws Exception {
        return mockMvc.perform(solicitud).andReturn().getResponse().getContentAsString();
    }

    private String mensajeDeError(MvcResult resultado) {
        Object valor = resultado.getFlashMap().get("mensajeError");
        return valor == null ? "" : String.valueOf(valor);
    }

    private MockHttpSession iniciarSesion(String correo) throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mockMvc.perform(post("/login").session(sesion)
                        .param("username", correo)
                        .param("password", CLAVE)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/panel"));
        return sesion;
    }

    /** El token CSRF es especifico de la sesion que lo solicita. */
    private String obtenerTokenCsrf(MockHttpSession sesion) throws Exception {
        String html = mockMvc.perform(get("/login").session(sesion))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).as("El formulario de login debe exponer el token CSRF").isTrue();
        return matcher.group(1);
    }

    private Categoria categoria(Long id, String codigo, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(id);
        categoria.setCodigo(codigo);
        categoria.setNombre(nombre);
        categoria.setActivo(true);
        return categoria;
    }

    /** Carga categorias y unidades de medida disponibles para los desplegables. */
    private void prepararRelaciones() {
        when(categoriaDao.listarTodas())
                .thenReturn(List.of(categoria(1L, "DADOS", "Dados")));
        when(unidadMedidaDao.listarTodas())
                .thenReturn(List.of(unidad(1L, "UNIDAD", "Unidad", "und")));
        when(categoriaDao.buscarPorId(1L)).thenReturn(Optional.of(categoria(1L, "DADOS", "Dados")));
        when(unidadMedidaDao.buscarPorId(1L)).thenReturn(Optional.of(unidad(1L, "UNIDAD", "Unidad", "und")));
    }

    private UnidadMedida unidad(Long id, String codigo, String nombre, String abreviatura) {
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setIdUnidadMedida(id);
        unidadMedida.setCodigo(codigo);
        unidadMedida.setNombre(nombre);
        unidadMedida.setAbreviatura(abreviatura);
        unidadMedida.setActivo(true);
        return unidadMedida;
    }

    private Producto producto(Long id) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setCategoria(categoria(1L, "DADOS", "Dados"));
        producto.setUnidadMedida(unidad(1L, "UNIDAD", "Unidad", "und"));
        producto.setCodigo("DADO-2CM");
        producto.setNombre("Dado separador de 2 cm");
        producto.setStockMinimo(new java.math.BigDecimal("20.000"));
        producto.setActivo(true);
        return producto;
    }

    /** Carga motivos, productos y existencias para el formulario de movimientos. */
    private void prepararMovimiento() {
        when(motivoMovimientoDao.listarPorTipo(any())).thenReturn(List.of(motivoMovimiento()));
        when(motivoMovimientoDao.buscarPorId(1L)).thenReturn(Optional.of(motivoMovimiento()));
        when(inventarioDao.listarTodo()).thenReturn(List.of(inventario()));
        when(productoDao.buscarPorId(1L)).thenReturn(Optional.of(producto(1L)));
        when(productoDao.obtenerStockActual(1L)).thenReturn(new java.math.BigDecimal("100.000"));
    }

    private MotivoMovimiento motivoMovimiento() {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setIdMotivoMovimiento(1L);
        motivo.setCodigo("ENTRADA_COMPRA");
        motivo.setNombre("Compra");
        motivo.setTipo("ENTRADA");
        motivo.setActivo(true);
        return motivo;
    }

    private Inventario inventario() {
        Inventario inventario = new Inventario();
        inventario.setIdInventario(1L);
        inventario.setProducto(producto(1L));
        inventario.setStock(new java.math.BigDecimal("100.000"));
        return inventario;
    }

    private Movimiento movimiento() {
        Movimiento movimiento = new Movimiento();
        movimiento.setIdMovimiento(1L);
        movimiento.setCodigo("MOV-20260101000000-ABCDEF12");
        movimiento.setMotivoMovimiento(motivoMovimiento());
        movimiento.setUsuario(usuario(CORREO_ADMIN, CORREO_ADMIN));
        movimiento.setFechaMovimiento(java.time.OffsetDateTime.now());
        movimiento.setTipo("ENTRADA");
        movimiento.setEstado("VIGENTE");
        return movimiento;
    }

    private Usuario usuario(String codigoRol, String correo) {
        Rol rol = new Rol();
        rol.setIdRol(1L);
        rol.setCodigo(codigoRol);
        rol.setNombre(codigoRol);
        rol.setActivo(true);

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setRol(rol);
        usuario.setCodigo("USR-1");
        usuario.setNombres("Nombre");
        usuario.setApellidos("Apellido");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(CLAVE));
        usuario.setActivo(true);
        return usuario;
    }
}
