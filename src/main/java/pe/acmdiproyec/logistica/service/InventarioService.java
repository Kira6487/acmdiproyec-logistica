package pe.acmdiproyec.logistica.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.InventarioDao;
import pe.acmdiproyec.logistica.model.Inventario;

/**
 * Consulta de existencias actuales. Es de solo lectura: el stock lo mantienen
 * los disparadores de la base de datos y esta capa nunca lo calcula.
 */
@Service
public class InventarioService {

    /** Estado de stock de un producto cuyo saldo no supera al minimo. */
    public static final String ESTADO_BAJO = "BAJO";
    /** Estado de stock de un producto cuyo saldo supera al minimo. */
    public static final String ESTADO_NORMAL = "NORMAL";

    private final InventarioDao inventarioDao;

    public InventarioService(InventarioDao inventarioDao) {
        this.inventarioDao = inventarioDao;
    }

    public List<Inventario> listarTodo() {
        return inventarioDao.listarTodo();
    }

    /**
     * Consulta de existencias con filtros opcionales. Los parametros nulos o en
     * blanco no restringen el resultado. El filtro se aplica sobre la
     * informacion ya recuperada, de modo que no genera consultas adicionales.
     */
    public List<Inventario> listarFiltrado(String busqueda, Long idCategoria, String estadoStock) {
        String criterio = busqueda == null ? "" : busqueda.trim().toLowerCase();
        return inventarioDao.listarTodo().stream()
                .filter(fila -> coincideBusqueda(fila, criterio))
                .filter(fila -> idCategoria == null || categoriaEs(fila, idCategoria))
                .filter(fila -> coincideEstado(fila, estadoStock))
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean coincideBusqueda(Inventario fila, String criterio) {
        if (criterio.isEmpty() || fila.getProducto() == null) {
            return true;
        }
        String codigo = fila.getProducto().getCodigo() == null ? "" : fila.getProducto().getCodigo().toLowerCase();
        String nombre = fila.getProducto().getNombre() == null ? "" : fila.getProducto().getNombre().toLowerCase();
        return codigo.contains(criterio) || nombre.contains(criterio);
    }

    private boolean categoriaEs(Inventario fila, Long idCategoria) {
        return fila.getProducto() != null
                && fila.getProducto().getCategoria() != null
                && idCategoria.equals(fila.getProducto().getCategoria().getIdCategoria());
    }

    private boolean coincideEstado(Inventario fila, String estadoStock) {
        if (estadoStock == null || estadoStock.isBlank()) {
            return true;
        }
        boolean bajo = estaPorDebajoDelMinimo(fila);
        return ESTADO_BAJO.equalsIgnoreCase(estadoStock) ? bajo : !bajo;
    }

    /** Un producto requiere reposicion cuando su stock no supera al minimo. */
    private boolean estaPorDebajoDelMinimo(Inventario fila) {
        if (fila.getStock() == null || fila.getProducto() == null
                || fila.getProducto().getStockMinimo() == null) {
            return false;
        }
        return fila.getStock().compareTo(fila.getProducto().getStockMinimo()) <= 0;
    }
}
