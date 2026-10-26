package pe.acmdiproyec.logistica.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.CategoriaDao;
import pe.acmdiproyec.logistica.dao.ProductoDao;
import pe.acmdiproyec.logistica.dao.UnidadMedidaDao;
import pe.acmdiproyec.logistica.dto.ProductoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.Categoria;
import pe.acmdiproyec.logistica.model.Producto;
import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Reglas de negocio de los productos: validacion de categoria y unidad de
 * medida contra sus tablas, duplicados, e integridad referencial al eliminar.
 *
 * La fila de {@code inventario} de cada producto la crea el disparador
 * {@code trg_productos_crear_inventario}; este servicio no la inserta ni la
 * modifica.
 */
@Service
public class ProductoService {

    private static final String MENSAJE_NO_ENCONTRADO =
            "El producto indicado no existe.";
    private static final String MENSAJE_DUPLICADO =
            "Ya existe un producto con el codigo o el nombre ingresado.";
    private static final String MENSAJE_CON_MOVIMIENTOS =
            "No se puede eliminar el producto porque tiene movimientos registrados.";
    private static final String MENSAJE_CON_EXISTENCIAS =
            "No se puede eliminar el producto porque tiene existencias disponibles.";
    private static final String MENSAJE_CON_INVENTARIO =
            "No se puede eliminar el producto porque ya existe su registro de inventario. "
                    + "Marquelo como inactivo para dejar de utilizarlo.";

    private final ProductoDao productoDao;
    private final CategoriaDao categoriaDao;
    private final UnidadMedidaDao unidadMedidaDao;

    public ProductoService(
            ProductoDao productoDao,
            CategoriaDao categoriaDao,
            UnidadMedidaDao unidadMedidaDao) {
        this.productoDao = productoDao;
        this.categoriaDao = categoriaDao;
        this.unidadMedidaDao = unidadMedidaDao;
    }

    public List<Producto> listarTodos() {
        return productoDao.listarTodos();
    }

    /** Devuelve el formulario de alta o edicion para el producto indicado. */
    public ProductoFormDto obtenerFormulario(Long idProducto) {
        return convertirFormulario(buscarProducto(idProducto));
    }

    public void crear(ProductoFormDto formulario) {
        normalizar(formulario);
        validarRelaciones(formulario);
        validarDuplicados(formulario, null);

        Producto producto = convertir(formulario);
        int filas;
        try {
            filas = productoDao.guardar(producto);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo registrar el producto.");
        }
    }

    public void actualizar(Long idProducto, ProductoFormDto formulario) {
        buscarProducto(idProducto);
        normalizar(formulario);
        validarRelaciones(formulario);
        validarDuplicados(formulario, idProducto);

        Producto producto = convertir(formulario);
        producto.setIdProducto(idProducto);
        int filas;
        try {
            filas = productoDao.actualizar(producto);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_DUPLICADO);
        }
        if (filas != 1) {
            throw new ReglaNegocioException("No se pudo actualizar el producto.");
        }
    }

    /**
     * Eliminacion fisica, permitida solo cuando las claves foraneas y las reglas
     * del negocio lo admiten. No se borran registros dependientes en cascada:
     * los movimientos y el inventario se conservan siempre.
     */
    public void eliminar(Long idProducto) {
        buscarProducto(idProducto);

        if (productoDao.contarMovimientosAsociados(idProducto) > 0) {
            throw new ReglaNegocioException(MENSAJE_CON_MOVIMIENTOS);
        }
        if (productoDao.obtenerStockActual(idProducto).compareTo(BigDecimal.ZERO) != 0) {
            throw new ReglaNegocioException(MENSAJE_CON_EXISTENCIAS);
        }

        int filas;
        try {
            filas = productoDao.eliminar(idProducto);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException(MENSAJE_CON_INVENTARIO);
        }
        if (filas != 1) {
            throw new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADO);
        }
    }

    public Producto buscarProducto(Long idProducto) {
        return productoDao.buscarPorId(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADO));
    }

    /**
     * La categoria y la unidad de medida deben existir en sus tablas: no se
     * aceptan identificadores arbitrarios enviados desde el navegador.
     */
    private void validarRelaciones(ProductoFormDto formulario) {
        categoriaDao.buscarPorId(formulario.getIdCategoria())
                .orElseThrow(() -> new ReglaNegocioException(
                        "La categoria indicada no existe. Seleccione una categoria valida."));
        unidadMedidaDao.buscarPorId(formulario.getIdUnidadMedida())
                .orElseThrow(() -> new ReglaNegocioException(
                        "La unidad de medida indicada no existe. Seleccione una unidad valida."));
    }

    /** Unica validacion de duplicados: no se repite en el controlador. */
    private void validarDuplicados(ProductoFormDto formulario, Long idExcluido) {
        if (productoDao.contarPorCodigo(formulario.getCodigo(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe un producto con el codigo ingresado.");
        }
        if (productoDao.contarPorNombre(formulario.getNombre(), idExcluido) > 0) {
            throw new ReglaNegocioException("Ya existe un producto con el nombre ingresado.");
        }
    }

    /** Recorta espacios y aplica los valores por defecto del esquema. */
    private void normalizar(ProductoFormDto formulario) {
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
        if (formulario.getStockMinimo() == null) {
            formulario.setStockMinimo(BigDecimal.ZERO);
        }
    }

    private Producto convertir(ProductoFormDto formulario) {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(formulario.getIdCategoria());

        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setIdUnidadMedida(formulario.getIdUnidadMedida());

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setUnidadMedida(unidadMedida);
        producto.setCodigo(formulario.getCodigo());
        producto.setNombre(formulario.getNombre());
        producto.setDescripcion(formulario.getDescripcion());
        producto.setStockMinimo(formulario.getStockMinimo());
        producto.setActivo(formulario.isActivo());
        return producto;
    }

    private ProductoFormDto convertirFormulario(Producto producto) {
        ProductoFormDto formulario = new ProductoFormDto();
        formulario.setIdProducto(producto.getIdProducto());
        if (producto.getCategoria() != null) {
            formulario.setIdCategoria(producto.getCategoria().getIdCategoria());
        }
        if (producto.getUnidadMedida() != null) {
            formulario.setIdUnidadMedida(producto.getUnidadMedida().getIdUnidadMedida());
        }
        formulario.setCodigo(producto.getCodigo());
        formulario.setNombre(producto.getNombre());
        formulario.setDescripcion(producto.getDescripcion());
        formulario.setStockMinimo(producto.getStockMinimo());
        formulario.setActivo(producto.isActivo());
        return formulario;
    }
}
