package pe.acmdiproyec.logistica.controller.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class IndexControllerTest {

    @Test
    void debeDevolverLaVistaInicial() {
        IndexController controller = new IndexController();

        assertThat(controller.mostrarInicio()).isEqualTo("index");
    }

    @Test
    void plantillaDebeMostrarElEstadoPreliminar() throws IOException {
        try (InputStream recurso = getClass().getClassLoader()
                .getResourceAsStream("templates/index.html")) {
            assertThat(recurso).isNotNull();
            String contenido = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(contenido)
                .contains("Sistema Logístico ACM Diproyec")
                .contains("Versión preliminar");
        }
    }
}
