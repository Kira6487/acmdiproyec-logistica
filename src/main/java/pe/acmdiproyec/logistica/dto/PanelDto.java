package pe.acmdiproyec.logistica.dto;

import java.util.ArrayList;
import java.util.List;

import pe.acmdiproyec.logistica.model.Inventario;
import pe.acmdiproyec.logistica.model.Movimiento;

/**
 * Datos del panel logistico.
 *
 * Todos los valores se calculan con consultas reales sobre las tablas de
 * productos, inventario y movimientos. Ningun campo es un dato simulado: si un
 * indicador no puede obtenerse con las operaciones existentes, no se muestra.
 */
public class PanelDto {

    /** Productos con estado activo. */
    private long productosActivos;

    /** Movimientos cuya fecha es de hoy. */
    private long movimientosHoy;

    /** Productos cuyo stock es igual o inferior al stock minimo. */
    private long alertasStock;

    /** Movimientos anulados en los ultimos siete dias. */
    private long anulacionesSieteDias;

    /** Existencias por debajo del stock minimo, ordenadas de menor a mayor. */
    private List<Inventario> productosPorReponer = new ArrayList<>();

    /** Ultimos movimientos registrados, del mas reciente al mas antiguo. */
    private List<Movimiento> ultimosMovimientos = new ArrayList<>();

    public long getProductosActivos() {
        return productosActivos;
    }

    public void setProductosActivos(long productosActivos) {
        this.productosActivos = productosActivos;
    }

    public long getMovimientosHoy() {
        return movimientosHoy;
    }

    public void setMovimientosHoy(long movimientosHoy) {
        this.movimientosHoy = movimientosHoy;
    }

    public long getAlertasStock() {
        return alertasStock;
    }

    public void setAlertasStock(long alertasStock) {
        this.alertasStock = alertasStock;
    }

    public long getAnulacionesSieteDias() {
        return anulacionesSieteDias;
    }

    public void setAnulacionesSieteDias(long anulacionesSieteDias) {
        this.anulacionesSieteDias = anulacionesSieteDias;
    }

    public List<Inventario> getProductosPorReponer() {
        return productosPorReponer;
    }

    public void setProductosPorReponer(List<Inventario> productosPorReponer) {
        this.productosPorReponer = productosPorReponer == null ? new ArrayList<>() : productosPorReponer;
    }

    public List<Movimiento> getUltimosMovimientos() {
        return ultimosMovimientos;
    }

    public void setUltimosMovimientos(List<Movimiento> ultimosMovimientos) {
        this.ultimosMovimientos = ultimosMovimientos == null ? new ArrayList<>() : ultimosMovimientos;
    }

    /** Indica si existe al menos un producto con stock por debajo del minimo. */
    public boolean isConAlertas() {
        return alertasStock > 0;
    }
}
