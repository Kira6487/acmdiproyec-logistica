package pe.acmdiproyec.logistica.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.MotivoMovimientoDao;
import pe.acmdiproyec.logistica.dto.MotivoMovimientoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.MotivoMovimiento;

/**
 * Reglas de negocio de los motivos de movimiento, uno de los datos maestros del
 * sistema conforme a {@code docs/GUIA_DESARROLLO_EQUIPO.md}.
 *
 * El tipo debe pertenecer al enum {@code tipo_movimiento}, la combinacion
 * nombre + tipo debe ser unica ({@code uq_motivos_movimiento_nombre_tipo}) y no
 * se permite eliminar un motivo que ya hayan usado los movimientos.
 */
@Service
public class MotivoMovimientoService {

    private static final String MENSAJE_NO_ENCONTRADO =
            "El motivo de movimiento indicado no existe.";
    private static final String MENSAJE_DUPLICADO =
            "Ya existe un motivo de movimiento con el codigo o el nombre y tipo ingresados.";
    private static final String MENSAJE_CON_MOVIMIENTOS =
            "No se puede eliminar el motivo porque ya fue utilizado en movimientos registrados.";

    private final MotivoMovimientoDao motivoMovimientoDao;

    public MotivoMovimientoService(MotivoMovimientoDao motivoMovimientoDao) {
        this.motivoMovimientoDao = motivoMovimientoDao;
    }

    public List<MotivoMovimiento> listarTodos() {
        return motivoMovimientoDao.listarTodos();
    }

    public List<MotivoMovimiento> listarPorTipo(String tipo) {
        validarTipo(tipo);
        return motivoMovimientoDao.listarPorTipo(tipo);
    }

    /** Devuelve el formulario de alta o edicion para el motivo indicado. */
    public MotivoMovimientoFormDto obtenerFormulario(Long idMotivoMovimiento) {
        return convertirFormulario(buscarMotivo(idMotivoMovimiento));
    }

    public void crear(MotivoMovimientoFormDto formulario) {
        normalizar(formulario);
        validarTipo(formulario.getTipo());
        validarDuplicados(formulario, null);

        MotivoMovimiento motivo = convertir(formulario);
        int filas;
        try {
            filas = motivoMovimientoDao.guardar(motivo);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo registrar el motivo de movimiento.");
        }
    }

    public void actualizar(Long idMotivoMovimiento, MotivoMovimientoFormDto formulario) {
        buscarMotivo(idMotivoMovimiento);
        normalizar(formulario);
        validarTipo(formulario.getTipo());
        validarDuplicados(formulario, idMotivoMovimiento);

        MotivoMovimiento motivo = convertir(formulario);
        motivo.setIdMotivoMovimiento(idMotivoMovimiento);
        int filas;
        try {
            filas = motivoMovimientoDao.actualizar(motivo);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo actualizar el motivo de movimiento.");
        }
    }

    /**
     * Elimina el motivo solo si ningun movimiento lo referencia; en caso
     * contrario se conserva el registro y se explica el motivo.
     */
    public void eliminar(Long idMotivoMovimiento) {
        buscarMotivo(idMotivoMovimiento);

        if (motivoMovimientoDao.contarMovimientosAsociados(idMotivoMovimiento) > 0) {
            throw new ReglaNegocioException(MENSAJE_CON_MOVIMIENTOS);
        }

        int filas;
        try {
            filas = motivoMovimientoDao.eliminar(idMotivoMovimiento);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_CON_MOVIMIENTOS);
        }
        if (filas != 1) {
            throw new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADO);
        }
    }

    public MotivoMovimiento buscarMotivo(Long idMotivoMovimiento) {
        return motivoMovimientoDao.buscarPorId(idMotivoMovimiento)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADO));
    }

    /** El tipo debe ser uno de los valores del enum {@code tipo_movimiento}. */
    private void validarTipo(String tipo) {
        if (!MotivoMovimiento.TIPO_ENTRADA.equals(tipo)
                && !MotivoMovimiento.TIPO_SALIDA.equals(tipo)) {
            throw new ReglaNegocioException(
                    "El tipo de movimiento debe ser ENTRADA o SALIDA.");
        }
    }

    /** Unica validacion de duplicados: no se repite en el controlador. */
    private void validarDuplicados(MotivoMovimientoFormDto formulario, Long idExcluido) {
        if (motivoMovimientoDao.contarPorCodigo(formulario.getCodigo(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe un motivo con el codigo ingresado.");
        }
        if (motivoMovimientoDao.contarPorNombreYTipo(
                formulario.getNombre(), formulario.getTipo(), idExcluido) > 0) {
            throw new ReglaNegocioException(
                    "Ya existe un motivo con el nombre y tipo ingresados.");
        }
    }

    /** Recorta espacios y vacia la descripcion cuando no aporta contenido. */
    private void normalizar(MotivoMovimientoFormDto formulario) {
        if (formulario.getCodigo() != null) {
            formulario.setCodigo(formulario.getCodigo().trim());
        }
        if (formulario.getNombre() != null) {
            formulario.setNombre(formulario.getNombre().trim());
        }
        if (formulario.getTipo() != null) {
            formulario.setTipo(formulario.getTipo().trim());
        }
        if (formulario.getDescripcion() != null) {
            String descripcion = formulario.getDescripcion().trim();
            formulario.setDescripcion(descripcion.isEmpty() ? null : descripcion);
        }
    }

    private MotivoMovimiento convertir(MotivoMovimientoFormDto formulario) {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setCodigo(formulario.getCodigo());
        motivo.setNombre(formulario.getNombre());
        motivo.setTipo(formulario.getTipo());
        motivo.setDescripcion(formulario.getDescripcion());
        motivo.setActivo(formulario.isActivo());
        return motivo;
    }

    private MotivoMovimientoFormDto convertirFormulario(MotivoMovimiento motivo) {
        MotivoMovimientoFormDto formulario = new MotivoMovimientoFormDto();
        formulario.setIdMotivoMovimiento(motivo.getIdMotivoMovimiento());
        formulario.setCodigo(motivo.getCodigo());
        formulario.setNombre(motivo.getNombre());
        formulario.setTipo(motivo.getTipo());
        formulario.setDescripcion(motivo.getDescripcion());
        formulario.setActivo(motivo.isActivo());
        return formulario;
    }
}
