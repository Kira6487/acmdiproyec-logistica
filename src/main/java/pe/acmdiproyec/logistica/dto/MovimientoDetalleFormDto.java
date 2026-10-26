package pe.acmdiproyec.logistica.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Linea del formulario de registro de un movimiento. {@code cantidad} se ajusta
 * al tipo {@code numeric(14,3)} de {@code public.movimiento_detalle}, cuya
 * restriccion exige un valor estrictamente positivo.
 */
public class MovimientoDetalleFormDto {

    @NotNull(message = "Debe seleccionar un producto.")
    private Long idProducto;

    @NotNull(message = "La cantidad es obligatoria.")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero.")
    @Digits(integer = 11, fraction = 3,
            message = "La cantidad debe ser un numero con hasta 3 decimales.")
    private BigDecimal cantidad;

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }
}
