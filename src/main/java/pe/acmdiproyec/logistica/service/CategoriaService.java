package pe.acmdiproyec.logistica.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.CategoriaDao;
import pe.acmdiproyec.logistica.dto.CategoriaFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.Categoria;

/**
 * Reglas de negocio de las categorias de productos: duplicados, integridad
 * referencial con productos y traduccion de errores de persistencia a mensajes
 * comprensibles para el usuario.
 */
@Service
public class CategoriaService {

    private static final String MENSAJE_NO_ENCONTRADA =
            "La categoria indicada no existe.";
    private static final String MENSAJE_DUPLICADO =
            "Ya existe una categoria con el codigo o el nombre ingresado.";
    private static final String MENSAJE_CON_PRODUCTOS =
            "No se puede eliminar la categoria mientras existan productos asociados a ella.";

    private final CategoriaDao categoriaDao;

    public CategoriaService(CategoriaDao categoriaDao) {
        this.categoriaDao = categoriaDao;
    }

    public List<Categoria> listarTodas() {
        return categoriaDao.listarTodas();
    }

    /** Devuelve el formulario de alta o edicion para la categoria indicada. */
    public CategoriaFormDto obtenerFormulario(Long idCategoria) {
        return convertirFormulario(buscarCategoria(idCategoria));
    }

    public void crear(CategoriaFormDto formulario) {
        normalizar(formulario);
        validarDuplicados(formulario, null);

        Categoria categoria = convertir(formulario);
        int filas;
        try {
            filas = categoriaDao.guardar(categoria);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo registrar la categoria.");
        }
    }

    public void actualizar(Long idCategoria, CategoriaFormDto formulario) {
        buscarCategoria(idCategoria);
        normalizar(formulario);
        validarDuplicados(formulario, idCategoria);

        Categoria categoria = convertir(formulario);
        categoria.setIdCategoria(idCategoria);
        int filas;
        try {
            filas = categoriaDao.actualizar(categoria);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo actualizar la categoria.");
        }
    }

    /**
     * Elimina la categoria solo si ningun producto la referencia; en caso
     * contrario se conserva el registro y se explica el motivo.
     */
    public void eliminar(Long idCategoria) {
        buscarCategoria(idCategoria);

        if (categoriaDao.contarProductosAsociados(idCategoria) > 0) {
            throw new ReglaNegocioException(MENSAJE_CON_PRODUCTOS);
        }

        int filas;
        try {
            filas = categoriaDao.eliminar(idCategoria);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_CON_PRODUCTOS);
        }
        if (filas != 1) {
            throw new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADA);
        }
    }

    public Categoria buscarCategoria(Long idCategoria) {
        return categoriaDao.buscarPorId(idCategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADA));
    }

    /** Unica validacion de duplicados: no se repite en el controlador. */
    private void validarDuplicados(CategoriaFormDto formulario, Long idExcluido) {
        if (categoriaDao.contarPorCodigo(formulario.getCodigo(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe una categoria con el codigo ingresado.");
        }
        if (categoriaDao.contarPorNombre(formulario.getNombre(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe una categoria con el nombre ingresado.");
        }
    }

    /** Recorta espacios y vacia la descripcion cuando no aporta contenido. */
    private void normalizar(CategoriaFormDto formulario) {
        if (formulario.getCodigo() != null) {
            formulario.setCodigo(formulario.getCodigo().trim());
        }
        if (formulario.getNombre() != null) {
            formulario.setNombre(formulario.getNombre().trim());
        }
        if (formulario.getDescripcion() != null) {
            String descripcion = formulario.getDescripcion().trim();
            formulario.setDescripcion(descripcion.isEmpty() ? null : descripcion);
        }
    }

    private Categoria convertir(CategoriaFormDto formulario) {
        Categoria categoria = new Categoria();
        categoria.setCodigo(formulario.getCodigo());
        categoria.setNombre(formulario.getNombre());
        categoria.setDescripcion(formulario.getDescripcion());
        categoria.setActivo(formulario.isActivo());
        return categoria;
    }

    private CategoriaFormDto convertirFormulario(Categoria categoria) {
        CategoriaFormDto formulario = new CategoriaFormDto();
        formulario.setIdCategoria(categoria.getIdCategoria());
        formulario.setCodigo(categoria.getCodigo());
        formulario.setNombre(categoria.getNombre());
        formulario.setDescripcion(categoria.getDescripcion());
        formulario.setActivo(categoria.isActivo());
        return formulario;
    }
}
