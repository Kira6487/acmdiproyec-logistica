package pe.acmdiproyec.logistica.security;

import static org.assertj.core.api.Assertions.assertThat;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.model.Rol;
import pe.acmdiproyec.logistica.model.Usuario;

import static org.mockito.Mockito.when;

/**
 * Pruebas de autenticación y autorización alineadas con los roles reales del
 * modelo de datos: ADMIN y LOGISTICA.
 *
 * No se añaden dependencias: se usa MockMvc con el filtro de seguridad real y
 * se autenticación mediante un login de formulario completo (incluido CSRF y
 * verificación BCrypt). La base de datos no se toca: UsuarioDao se sustituye
 * por un doble y el perfil "test" excluye la autoconfiguración del DataSource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadRolesTest {

    private static final String CORREO_ADMIN = "admin@acmdiproyec.test";
    private static final String CORREO_LOGISTICA = "logistica@acmdiproyec.test";
    private static final String CLAVE = "clave-de-prueba-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioDao usuarioDao;

    // ---------------------------------------------------------------
    // 1. La configuración reconoce el rol ADMIN como ROLE_ADMIN
    // ---------------------------------------------------------------

    @Test
    void debeConvertirElRolAdminEnLaAutoridadRoleAdmin() {
        prepararUsuariosEnElDoble();

        UserDetails detalles = customUserDetailsService.loadUserByUsername(CORREO_ADMIN);

        assertThat(detalles.getUsername()).isEqualTo(CORREO_ADMIN);
        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void debeConvertirElRolLogisticaEnLaAutoridadRoleLogistica() {
        prepararUsuariosEnElDoble();

        UserDetails detalles = customUserDetailsService.loadUserByUsername(CORREO_LOGISTICA);

        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_LOGISTICA");
    }

    @Test
    void elPasswordAlmacenadoEsUnHashBCryptVerificable() {
        prepararUsuariosEnElDoble();

        UserDetails detalles = customUserDetailsService.loadUserByUsername(CORREO_ADMIN);

        assertThat(detalles.getPassword()).isNotEqualTo(CLAVE);
        assertThat(passwordEncoder.matches(CLAVE, detalles.getPassword())).isTrue();
    }

    // ---------------------------------------------------------------
    // 2 y 3. ADMIN sí y LOGISTICA no acceden a gestión de usuarios
    // ---------------------------------------------------------------

    @Test
    void adminPuedeAccederAGestionDeUsuarios() throws Exception {
        prepararUsuariosEnElDoble();
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(get("/usuarios").session(sesion))
                .andExpect(status().isOk());
    }

    @Test
    void logisticaNoPuedeAccederAGestionDeUsuarios() throws Exception {
        prepararUsuariosEnElDoble();
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/usuarios").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    void logisticaNoPuedeAccederAlFormularioDeAltaDeUsuarios() throws Exception {
        prepararUsuariosEnElDoble();
        MockHttpSession sesion = iniciarSesion(CORREO_LOGISTICA);

        mockMvc.perform(get("/usuarios/nuevo").session(sesion))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // 4. Login y recursos estáticos siguen siendo públicos
    // ---------------------------------------------------------------

    @Test
    void laPaginaDeLoginEsPublica() throws Exception {
        prepararUsuariosEnElDoble();

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void losRecursosEstaticosSonPublicos() throws Exception {
        prepararUsuariosEnElDoble();

        mockMvc.perform(get("/css/styles.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/img/favicon.svg")).andExpect(status().isOk());
        mockMvc.perform(get("/css/index.css")).andExpect(status().isOk());
    }

    @Test
    void laPortadaEsPublica() throws Exception {
        prepararUsuariosEnElDoble();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    // ---------------------------------------------------------------
    // 5. Solicitud no autenticada a ruta protegida
    // ---------------------------------------------------------------

    @Test
    void unaSolicitudNoAutenticadaRedirigeAlLogin() throws Exception {
        prepararUsuariosEnElDoble();

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void elPanelRequiereAutenticacion() throws Exception {
        prepararUsuariosEnElDoble();

        mockMvc.perform(get("/panel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ---------------------------------------------------------------
    // 6. Las rutas generales siguen accesibles para ambos roles
    // ---------------------------------------------------------------

    @Test
    void ambosRolesPuedenAccederAlPanel() throws Exception {
        prepararUsuariosEnElDoble();

        for (String correo : List.of(CORREO_ADMIN, CORREO_LOGISTICA)) {
            mockMvc.perform(get("/panel").session(iniciarSesion(correo)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void ambosRolesPuedenConsultarInventario() throws Exception {
        prepararUsuariosEnElDoble();

        // /inventario todavía no tiene controlador. Lo que se verifica es que la
        // regla de autorización deja pasar a ambos roles: la respuesta procede de
        // la capa web, nunca de un 401/403 de seguridad.
        for (String correo : List.of(CORREO_ADMIN, CORREO_LOGISTICA)) {
            mockMvc.perform(get("/inventario").session(iniciarSesion(correo)))
                    .andExpect(resultado -> assertThat(resultado.getResponse().getStatus())
                            .as("La ruta /inventario debe estar autorizada para " + correo)
                            .isNotIn(401, 403));
        }

        // Sin autenticación sí debe bloquear.
        mockMvc.perform(get("/inventario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void ambosRolesPuedenAccederAMovimientos() throws Exception {
        prepararUsuariosEnElDoble();

        for (String correo : List.of(CORREO_ADMIN, CORREO_LOGISTICA)) {
            mockMvc.perform(get("/movimientos").session(iniciarSesion(correo)))
                    .andExpect(resultado -> assertThat(resultado.getResponse().getStatus())
                            .as("La ruta /movimientos debe estar autorizada para " + correo)
                            .isNotIn(401, 403));
        }

        mockMvc.perform(get("/movimientos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ---------------------------------------------------------------
    // Verificación del login: formulario, credenciales, redirección y logout
    // ---------------------------------------------------------------

    @Test
    void elLoginCorrectoRedirigeAlPanel() throws Exception {
        prepararUsuariosEnElDoble();
        MockHttpSession sesion = new MockHttpSession();

        mockMvc.perform(post("/login").session(sesion)
                        .param("username", CORREO_ADMIN)
                        .param("password", CLAVE)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/panel"));
    }

    @Test
    void unaClaveIncorrectaVuelveAlLoginConError() throws Exception {
        prepararUsuariosEnElDoble();
        MockHttpSession sesion = new MockHttpSession();

        mockMvc.perform(post("/login").session(sesion)
                        .param("username", CORREO_ADMIN)
                        .param("password", "clave-incorrecta")
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void elLogoutFinalizaLaSesionYVuelveAlLogin() throws Exception {
        prepararUsuariosEnElDoble();
        MockHttpSession sesion = iniciarSesion(CORREO_ADMIN);

        mockMvc.perform(post("/logout").session(sesion).param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        // La sesión ya no autentica: vuelve a exigir login.
        mockMvc.perform(get("/usuarios").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void unUsuarioInactivoNoPuedeIniciarSesion() throws Exception {
        when(usuarioDao.buscarPorCorreo(CORREO_LOGISTICA))
                .thenReturn(Optional.of(usuario("LOGISTICA", CORREO_LOGISTICA, false)));
        MockHttpSession sesion = new MockHttpSession();

        mockMvc.perform(post("/login").session(sesion)
                        .param("username", CORREO_LOGISTICA)
                        .param("password", CLAVE)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void elPanelOcultaElEnlaceDeUsuariosSegunElRol() throws Exception {
        prepararUsuariosEnElDoble();

        String htmlAdmin = mockMvc.perform(get("/panel").session(iniciarSesion(CORREO_ADMIN)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String htmlLogistica = mockMvc.perform(get("/panel").session(iniciarSesion(CORREO_LOGISTICA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(htmlAdmin)
                .as("ADMIN debe ver el acceso a gestión de usuarios")
                .contains("href=\"/usuarios\"");
        assertThat(htmlLogistica)
                .as("LOGISTICA no debe ver el acceso a gestión de usuarios")
                .doesNotContain("href=\"/usuarios\"");
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    private void prepararUsuariosEnElDoble() {
        when(usuarioDao.buscarPorCorreo(CORREO_ADMIN))
                .thenReturn(Optional.of(usuario("ADMIN", CORREO_ADMIN, true)));
        when(usuarioDao.buscarPorCorreo(CORREO_LOGISTICA))
                .thenReturn(Optional.of(usuario("LOGISTICA", CORREO_LOGISTICA, true)));
        when(usuarioDao.listarTodos()).thenReturn(List.of());
        when(usuarioDao.listarRolesActivos()).thenReturn(List.of());
    }

    private Usuario usuario(String codigoRol, String correo, boolean activo) {
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
        usuario.setPasswordHash(passwordEncoder.encode(CLAVE));
        usuario.setActivo(activo);
        return usuario;
    }

    /** Realiza un login de formulario completo y devuelve la sesión autenticada. */
    private MockHttpSession iniciarSesion(String correo) throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        MvcResult resultado = mockMvc.perform(post("/login")
                        .session(sesion)
                        .param("username", correo)
                        .param("password", CLAVE)
                        .param("_csrf", obtenerTokenCsrf(sesion)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/panel"))
                .andReturn();
        assertThat(resultado.getRequest().getSession(false)).isNotNull();
        return sesion;
    }

    /**
     * El token CSRF es específico de la sesión: debe recogerse con la misma
     * sesión que se usará en la solicitud POST.
     */
    private String obtenerTokenCsrf(MockHttpSession sesion) throws Exception {
        String html = mockMvc.perform(get("/login").session(sesion))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Matcher matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find())
                .as("El formulario de login debe exponer el token CSRF")
                .isTrue();
        return matcher.group(1);
    }
}
