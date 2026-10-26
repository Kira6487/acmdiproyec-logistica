package pe.acmdiproyec.logistica.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario de alta y edicion de productos. Las restricciones reflejan las
 * columnas de {@code public.productos} y de la migracion que agrega
 * {@code stock_minimo}:
 * {@code codigo varchar(50) not null unique}, {@code nombre varchar(150) not
 * null unique}, {@code descripcion varchar(500)},
 * {@code stock_minimo numeric(14,3) not null default 0} y las claves foraneas
 * obligatorias {@code id_categoria} e {@code id_unidad_medida}.
 */
public class ProductoFormDto {

    private Long idProducto;

    @NotNull(message = "La categoria es obligatoria.")
    private Long idCategoria;

    @NotNull(message = "La unidad de medida es obligatoria.")
    private Long idUnidadMedida;

    @NotBlank(message = "El codigo es obligatorio.")
    @Size(max = 50, message = "El codigo no puede superar 50 caracteres.")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres.")
    private String nombre;

    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres.")
    private String descripcion;

    @NotNull(message = "El stock minimo es obligatorio.")
    @DecimalMin(value = "0", message = "El stock minimo no puede ser negativo.")
    @Digits(integer = 11, fraction = 3,
            message = "El stock minimo debe ser un numero con hasta 3 decimales.")
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    private boolean activo = true;

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public Long getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Long idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Long getIdUnidadMedida() {
        return idUnidadMedida;
    }

    public void setIdUnidadMedida(Long idUnidadMedida) {
        this.idUnidadMedida = idUnidadMedida;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
