package pe.acmdiproyec.logistica.controller.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import pe.acmdiproyec.logistica.dao.UnidadMedidaDao;
import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.model.Categoria;
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
