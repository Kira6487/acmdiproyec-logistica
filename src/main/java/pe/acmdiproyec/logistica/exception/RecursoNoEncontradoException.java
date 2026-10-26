package pe.acmdiproyec.logistica.exception;

/**
 * El registro solicitado no existe en la base de datos. Se usa para responder
 * de forma comprensible ante identificadores inexistentes, sin provocar un
 * error 500.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
