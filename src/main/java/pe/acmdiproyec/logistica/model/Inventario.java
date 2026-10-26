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
}
