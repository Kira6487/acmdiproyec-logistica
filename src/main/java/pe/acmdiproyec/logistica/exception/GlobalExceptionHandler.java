package pe.acmdiproyec.logistica.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Manejo centralizado de errores web.
 *
 * Cada tipo de excepcion se traduce al codigo HTTP que le corresponde y a la
 * vista {@code error}, que nunca expone detalles internos. Los controladores
 * siguen capturando las excepciones de negocio para ofrecer mensajes
 * contextuales junto al formulario; este componente actua como red de
 * seguridad para las solicitudes que llegan sin controlador asociado o con un
 * error no previsto.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VISTA_ERROR = "error";

    /** Recurso o ruta inexistente: responde 404 y no un error de servidor. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView manejarRecursoNoEncontrado(
            NoResourceFoundException excepcion, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return new ModelAndView(VISTA_ERROR);
    }

    /** Identificador de registro inexistente en una operacion de negocio. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ModelAndView manejarRegistroNoEncontrado(
            RecursoNoEncontradoException excepcion, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return new ModelAndView(VISTA_ERROR);
    }

    /** Regla de negocio incumplida: peticiones validas pero no atendibles. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ModelAndView manejarReglaDeNegocio(
            ReglaNegocioException excepcion, HttpServletResponse response) {
        LOGGER.warn("Regla de negocio incumplida: {}", excepcion.getMessage());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return new ModelAndView(VISTA_ERROR);
    }

    /** Metodo HTTP no soportado por la ruta solicitada. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView manejarMetodoNoSoportado(
            HttpRequestMethodNotSupportedException excepcion,
            HttpServletResponse response) {
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        return new ModelAndView(VISTA_ERROR);
    }

    /**
     * Identificador de ruta con formato invalido, por ejemplo {@code /movimientos/abc}
     * cuando se espera un numero. Es un error de la peticion, no del servidor.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView manejarArgumentoConTipoInvalido(
            MethodArgumentTypeMismatchException excepcion,
            HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return new ModelAndView(VISTA_ERROR);
    }

    /** Parametro obligatorio ausente en la peticion. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ModelAndView manejarParametroAusente(
            MissingServletRequestParameterException excepcion,
            HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return new ModelAndView(VISTA_ERROR);
    }

    /** Cualquier otra excepcion se registra y responde 500 sin filtrar datos. */
    @ExceptionHandler(Exception.class)
    public ModelAndView manejarErrorInesperado(Exception exception, HttpServletResponse response) {
        LOGGER.error("Error inesperado al procesar una solicitud web", exception);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ModelAndView(VISTA_ERROR);
    }
}
