package pe.acmdiproyec.logistica.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.UnidadMedidaDao;
import pe.acmdiproyec.logistica.dto.UnidadMedidaFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Reglas de negocio de las unidades de medida: duplicados, integridad
 * referencial con productos y traduccion de errores de persistencia a mensajes
 * comprensibles para el usuario.
 */
@Service
public class UnidadMedidaService {

    private static final String MENSAJE_NO_ENCONTRADA =
            "La unidad de medida indicada no existe.";
    private static final String MENSAJE_DUPLICADO =
            "Ya existe una unidad de medida con el codigo, el nombre o la abreviatura ingresados.";
    private static final String MENSAJE_CON_PRODUCTOS =
            "No se puede eliminar la unidad de medida mientras existan productos asociados a ella.";

    private final UnidadMedidaDao unidadMedidaDao;

    public UnidadMedidaService(UnidadMedidaDao unidadMedidaDao) {
        this.unidadMedidaDao = unidadMedidaDao;
    }

    public List<UnidadMedida> listarTodas() {
        return unidadMedidaDao.listarTodas();
    }

    /** Devuelve el formulario de alta o edicion para la unidad indicada. */
    public UnidadMedidaFormDto obtenerFormulario(Long idUnidadMedida) {
        return convertirFormulario(buscarUnidadMedida(idUnidadMedida));
    }

    public void crear(UnidadMedidaFormDto formulario) {
        normalizar(formulario);
        validarDuplicados(formulario, null);

        UnidadMedida unidadMedida = convertir(formulario);
        int filas;
        try {
            filas = unidadMedidaDao.guardar(unidadMedida);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo registrar la unidad de medida.");
        }
    }

    public void actualizar(Long idUnidadMedida, UnidadMedidaFormDto formulario) {
        buscarUnidadMedida(idUnidadMedida);
        normalizar(formulario);
        validarDuplicados(formulario, idUnidadMedida);

        UnidadMedida unidadMedida = convertir(formulario);
        unidadMedida.setIdUnidadMedida(idUnidadMedida);
        int filas;
        try {
            filas = unidadMedidaDao.actualizar(unidadMedida);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo actualizar la unidad de medida.");
        }
    }

    /**
     * Elimina la unidad solo si ningun producto la referencia; en caso
     * contrario se conserva el registro y se explica el motivo.
     */
    public void eliminar(Long idUnidadMedida) {
        buscarUnidadMedida(idUnidadMedida);

        if (unidadMedidaDao.contarProductosAsociados(idUnidadMedida) > 0) {
            throw new ReglaNegocioException(MENSAJE_CON_PRODUCTOS);
        }

        int filas;
        try {
            filas = unidadMedidaDao.eliminar(idUnidadMedida);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_CON_PRODUCTOS);
        }
        if (filas != 1) {
            throw new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADA);
        }
    }

    public UnidadMedida buscarUnidadMedida(Long idUnidadMedida) {
        return unidadMedidaDao.buscarPorId(idUnidadMedida)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADA));
    }

    /** Unica validacion de duplicados: no se repite en el controlador. */
    private void validarDuplicados(UnidadMedidaFormDto formulario, Long idExcluido) {
        if (unidadMedidaDao.contarPorCodigo(formulario.getCodigo(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe una unidad de medida con el codigo ingresado.");
        }
        if (unidadMedidaDao.contarPorNombre(formulario.getNombre(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe una unidad de medida con el nombre ingresado.");
        }
        if (unidadMedidaDao.contarPorAbreviatura(formulario.getAbreviatura(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe una unidad de medida con la abreviatura ingresada.");
        }
    }

    /** Recorta espacios antes de validar y persistir. */
    private void normalizar(UnidadMedidaFormDto formulario) {
        if (formulario.getCodigo() != null) {
            formulario.setCodigo(formulario.getCodigo().trim());
        }
        if (formulario.getNombre() != null) {
            formulario.setNombre(formulario.getNombre().trim());
        }
        if (formulario.getAbreviatura() != null) {
            formulario.setAbreviatura(formulario.getAbreviatura().trim());
        }
    }

    private UnidadMedida convertir(UnidadMedidaFormDto formulario) {
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setCodigo(formulario.getCodigo());
        unidadMedida.setNombre(formulario.getNombre());
        unidadMedida.setAbreviatura(formulario.getAbreviatura());
        unidadMedida.setActivo(formulario.isActivo());
        return unidadMedida;
    }

    private UnidadMedidaFormDto convertirFormulario(UnidadMedida unidadMedida) {
        UnidadMedidaFormDto formulario = new UnidadMedidaFormDto();
        formulario.setIdUnidadMedida(unidadMedida.getIdUnidadMedida());
        formulario.setCodigo(unidadMedida.getCodigo());
        formulario.setNombre(unidadMedida.getNombre());
        formulario.setAbreviatura(unidadMedida.getAbreviatura());
        formulario.setActivo(unidadMedida.isActivo());
        return formulario;
    }
}
