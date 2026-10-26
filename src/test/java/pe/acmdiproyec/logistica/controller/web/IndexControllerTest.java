package pe.acmdiproyec.logistica.controller.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.AbstractView;

class IndexControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new IndexController())
                .setViewResolvers((viewName, locale) -> new AbstractView() {
                    @Override
                    protected void renderMergedOutputModel(
                            Map<String, Object> model,
                            HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
                        response.setContentType("text/html;charset=UTF-8");
                        response.getWriter().write(leerRecurso("templates/" + viewName + ".html"));
                    }
                })
                .build();
    }

    @Test
    void inicioDebeResponderYRenderizarLaPlantilla() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Sistema Logístico ACM Diproyec")))
                .andExpect(content().string(containsString("Versión preliminar")));
    }

    @Test
    void inicioDebeMostrarLosAccesosPrincipalesSinRutasFuturas() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Entrada de inventario")))
                .andExpect(content().string(containsString("Salida de inventario")))
                .andExpect(content().string(containsString("Stock actual")))
                .andExpect(content().string(containsString("Historial de movimientos")))
                .andExpect(content().string(containsString("Datos maestros")))
                .andExpect(content().string(containsString("Reportes")))
                .andExpect(content().string(containsString("Próximamente")));
    }

    @Test
    void recursosEstaticosDebenEstarDisponibles() throws IOException {
        assertThat(leerRecurso("static/css/styles.css"))
                .contains("--touch-target: 2.75rem");
        assertThat(leerRecurso("static/js/app.js"))
                .contains("[data-nav-toggle]");
    }

    @Test
    void vistaDeErrorNoDebeExponerDetallesInternos() throws IOException {
        String contenido = leerRecurso("templates/error.html");

        assertThat(contenido)
                .contains("No fue posible completar la solicitud")
                .contains("Volver al inicio")
                .doesNotContain("${exception}", "${trace}", "stacktrace", "exception.message");
    }

    private String leerRecurso(String ruta) throws IOException {
        try (InputStream recurso = getClass().getClassLoader().getResourceAsStream(ruta)) {
            assertThat(recurso).isNotNull();
            return new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
