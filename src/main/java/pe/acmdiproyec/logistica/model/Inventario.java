package pe.acmdiproyec.logistica.model;

import java.math.BigDecimal;

/**
 * Existencias actuales de un producto. Corresponde con la tabla
 * {@code public.inventario}.
 *
 * El stock lo mantiene la base de datos mediante disparadores; esta capa nunca
 * lo calcula ni lo escribe.
 */
public class Inventario {

    private Long idInventario;
    private Producto producto;
    private BigDecimal stock;

    public Long getIdInventario() {
        return idInventario;
    }

    public void setIdInventario(Long idInventario) {
        this.idInventario = idInventario;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    /**
     * Nivel de existencias respecto al stock minimo, acotado entre 0 y 100.
     * Es una propiedad de presentacion: no se almacena ni se calcula en la base
     * de datos. Cuando el producto no define stock minimo se considera completo.
     */
    public int getNivelPorcentaje() {
        if (stock == null) {
            return 0;
        }
        BigDecimal minimo = producto == null ? null : producto.getStockMinimo();
        if (minimo == null || minimo.signum() <= 0) {
            return stock.signum() > 0 ? 100 : 0;
        }
        BigDecimal nivel = stock.multiply(new BigDecimal(100))
                .divide(minimo, 0, java.math.RoundingMode.HALF_UP);
        int valor = nivel.intValue();
        return Math.max(0, Math.min(100, valor));
    }

    /** Indica si el saldo actual no supera al stock minimo del producto. */
    public boolean isStockBajo() {
        BigDecimal minimo = producto == null ? null : producto.getStockMinimo();
        return stock != null && minimo != null && stock.compareTo(minimo) <= 0;
    }
}
