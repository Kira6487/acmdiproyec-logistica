package pe.acmdiproyec.logistica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario de alta y edicion de categorias. Las restricciones reflejan las
 * columnas de {@code public.categorias}: {@code codigo varchar(30) not null
 * unique}, {@code nombre varchar(100) not null unique} y
 * {@code descripcion varchar(250)}.
 */
public class CategoriaFormDto {

    private Long idCategoria;

    @NotBlank(message = "El codigo es obligatorio.")
    @Size(max = 30, message = "El codigo no puede superar 30 caracteres.")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    private String nombre;

    @Size(max = 250, message = "La descripcion no puede superar 250 caracteres.")
    private String descripcion;

    private boolean activo = true;

    public Long getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Long idCategoria) {
        this.idCategoria = idCategoria;
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

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
