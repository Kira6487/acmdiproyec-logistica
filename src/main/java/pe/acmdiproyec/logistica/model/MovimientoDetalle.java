package pe.acmdiproyec.logistica.model;

import java.math.BigDecimal;

/**
 * Linea de detalle de un movimiento de inventario. Corresponde con la tabla
 * {@code public.movimiento_detalle}. Sus filas son inmutables: la base de datos
 * impide actualizarlas o borrarlas.
 */
public class MovimientoDetalle {

    private Long idMovimientoDetalle;
    private Long idMovimiento;
    private Producto producto;
    private BigDecimal cantidad;

    /**
     * Stock actual del producto tras el movimiento. No se persiste: se lee de
     * {@code inventario} unicamente para mostrar el efecto de la operacion.
     */
    private BigDecimal stockActual;

    public Long getIdMovimientoDetalle() {
        return idMovimientoDetalle;
    }

    public void setIdMovimientoDetalle(Long idMovimientoDetalle) {
        this.idMovimientoDetalle = idMovimientoDetalle;
    }

    public Long getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(Long idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public void setStockActual(BigDecimal stockActual) {
        this.stockActual = stockActual;
    }
}
