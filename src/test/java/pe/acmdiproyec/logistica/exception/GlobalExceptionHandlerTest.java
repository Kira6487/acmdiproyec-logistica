package pe.acmdiproyec.logistica.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Pruebas del manejo centralizado de errores web.
 *
 * Se comprueba que cada excepcion se traduzca al codigo HTTP que le
 * corresponde, que los errores reales del servidor sigan respondiendo 500 y que
 * ninguna respuesta filtre detalles internos.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    @Test
    void recursoInexistenteDebeResponder404() {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        ModelAndView vista = manejador.manejarRecursoNoEncontrado(
                new NoResourceFoundException(HttpMethod.GET, "/ruta-inexistente"), respuesta);

        assertThat(respuesta.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(vista.getViewName()).isEqualTo("error");
    }

    @Test
    void registroInexistenteDebeResponder404() {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        ModelAndView vista = manejador.manejarRegistroNoEncontrado(
                new RecursoNoEncontradoException("El movimiento indicado no existe."), respuesta);

        assertThat(respuesta.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(vista.getViewName()).isEqualTo("error");
    }

    @Test
    void reglaDeNegocioIncumplidaDebeResponder400() {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        ModelAndView vista = manejador.manejarReglaDeNegocio(
                new ReglaNegocioException("El motivo es obligatorio."), respuesta);

        assertThat(respuesta.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(vista.getViewName()).isEqualTo("error");
    }

    @Test
    void metodoHttpNoSoportadoDebeResponder405() {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        ModelAndView vista = manejador.manejarMetodoNoSoportado(
                new HttpRequestMethodNotSupportedException("DELETE"), respuesta);

        assertThat(respuesta.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(vista.getViewName()).isEqualTo("error");
    }

    @Test
    void errorInesperadoDebeResponder500() {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        ModelAndView vista = manejador.manejarErrorInesperado(
                new IllegalStateException("fallo interno"), respuesta);

        assertThat(respuesta.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(vista.getViewName()).isEqualTo("error");
    }

    @Test
    void ningunaRespuestaDebeExponerDetallesInternos() {
        for (Object excepcion : new Object[] {
                new RecursoNoEncontradoException("El movimiento indicado no existe."),
                new ReglaNegocioException("El motivo es obligatorio."),
                new IllegalStateException("fallo interno con datos sensibles") }) {

            MockHttpServletResponse respuesta = new MockHttpServletResponse();
            ModelAndView vista;
            if (excepcion instanceof RecursoNoEncontradoException e) {
                vista = manejador.manejarRegistroNoEncontrado(e, respuesta);
            } else if (excepcion instanceof ReglaNegocioException e) {
                vista = manejador.manejarReglaDeNegocio(e, respuesta);
            } else {
                vista = manejador.manejarErrorInesperado((Exception) excepcion, respuesta);
            }

            assertThat(vista.getViewName())
                    .as("Toda respuesta de error usa la vista error")
                    .isEqualTo("error");
            assertThat(vista.getModel())
                    .as("La vista de error no recibe el mensaje de la excepcion")
                    .isEmpty();
        }
    }
}
