package pe.acmdiproyec.logistica.exception;

/**
 * Regla de negocio incumplida: duplicados, registros asociados u otras
 * condiciones que impiden completar la operacion. El mensaje esta pensado para
 * mostrarse al usuario y siempre se expresa en espanol.
 */
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String message) {
        super(message);
    }
}
