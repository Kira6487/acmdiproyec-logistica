package pe.acmdiproyec.logistica.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.InventarioDao;
import pe.acmdiproyec.logistica.dao.MovimientoDao;
import pe.acmdiproyec.logistica.dao.ProductoDao;
import pe.acmdiproyec.logistica.dto.PanelDto;
import pe.acmdiproyec.logistica.model.Inventario;
import pe.acmdiproyec.logistica.model.Movimiento;

/**
 * Calcula los indicadores del panel logistico a partir de datos reales.
 *
 * Todos los valores proceden de las operaciones ya existentes sobre
 * {@code productos}, {@code inventario} y {@code movimientos}. No se inventa
 * ninguna metrica: si un dato no puede obtenerse, no se incorpora al panel.
 *
 * Nota de diseno: los indicadores de movimientos se calculan con una sola
 * consulta al historial, filtrando en memoria. Las lineas de los movimientos
 * recientes se completan por separado y su numero esta acotado por
 * {@code ULTIMOS_MOVIMIENTOS}. Para el volumen de un unico almacen es
 * suficiente; si el historial creciera, convendria sustituirlo por consultas de
 * agregacion en el DAO.
 */
@Service
public class PanelService {

    /** Cantidad maxima de movimientos recientes que muestra el panel. */
    private static final int ULTIMOS_MOVIMIENTOS = 5;

    private static final String ESTADO_ANULADO = MovimientoService.ESTADO_ANULADO;

    private final ProductoDao productoDao;
    private final InventarioDao inventarioDao;
    private final MovimientoDao movimientoDao;

    public PanelService(
            ProductoDao productoDao,
            InventarioDao inventarioDao,
            MovimientoDao movimientoDao) {
        this.productoDao = productoDao;
        this.inventarioDao = inventarioDao;
        this.movimientoDao = movimientoDao;
    }

    public PanelDto obtenerPanel() {
        PanelDto panel = new PanelDto();

        List<pe.acmdiproyec.logistica.model.Producto> productos = productoDao.listarTodos();
        panel.setProductosActivos(
                productos.stream().filter(pe.acmdiproyec.logistica.model.Producto::isActivo).count());

        List<Inventario> existencias = inventarioDao.listarTodo();
        List<Inventario> porReponer = existencias.stream()
                .filter(this::estaPorDebajoDelMinimo)
                .sorted(Comparator.comparing(Inventario::getStock))
                .collect(Collectors.toList());
        panel.setAlertasStock(porReponer.size());
        panel.setProductosPorReponer(porReponer);

        List<Movimiento> movimientos = movimientoDao.listarTodos(null, null, null, null, null);
        OffsetDateTime inicioHoy = inicioDelDia();
        OffsetDateTime haceSieteDias = OffsetDateTime.now().minus(Duration.ofDays(7));

        panel.setMovimientosHoy(movimientos.stream()
                .filter(m -> m.getFechaMovimiento() != null && !m.getFechaMovimiento().isBefore(inicioHoy))
                .count());

        panel.setAnulacionesSieteDias(movimientos.stream()
                .filter(m -> ESTADO_ANULADO.equals(m.getEstado()))
                .filter(m -> m.getFechaAnulacion() != null && !m.getFechaAnulacion().isBefore(haceSieteDias))
                .count());

        List<Movimiento> recientes =
                movimientos.stream().limit(ULTIMOS_MOVIMIENTOS).collect(Collectors.toList());
        // Se completan unicamente los movimientos que se van a mostrar, de modo
        // que el numero de consultas adicionales esta acotado por ULTIMOS_MOVIMIENTOS.
        recientes.forEach(m -> m.setDetalles(movimientoDao.listarDetalles(m.getIdMovimiento())));
        panel.setUltimosMovimientos(recientes);

        return panel;
    }

    /** Un producto requiere reposicion cuando su stock no supera al minimo. */
    private boolean estaPorDebajoDelMinimo(Inventario existencia) {
        if (existencia.getStock() == null || existencia.getProducto() == null) {
            return false;
        }
        if (existencia.getProducto().getStockMinimo() == null) {
            return false;
        }
        return existencia.getStock().compareTo(existencia.getProducto().getStockMinimo()) <= 0;
    }

    private OffsetDateTime inicioDelDia() {
        ZonedDateTime hoy = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        return hoy.toOffsetDateTime();
    }
}
