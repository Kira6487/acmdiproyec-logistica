package pe.acmdiproyec.logistica.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import pe.acmdiproyec.logistica.model.Producto;

/**
 * Acceso a datos de los productos (tabla {@code public.productos}). Solo
 * declara operaciones de persistencia; las reglas de negocio corresponden a la
 * capa de servicio.
 */
public interface ProductoDao {

    List<Producto> listarTodos();

    Optional<Producto> buscarPorId(Long idProducto);

    int guardar(Producto producto);

    int actualizar(Producto producto);

    int eliminar(Long idProducto);

    int contarPorCodigo(String codigo, Long idProductoExcluido);

    int contarPorNombre(String nombre, Long idProductoExcluido);

    /** Detalles de movimiento que referencian al producto. */
    long contarMovimientosAsociados(Long idProducto);

    /** Stock actual en {@code inventario}; cero si aun no existe la fila. */
    BigDecimal obtenerStockActual(Long idProducto);
}
