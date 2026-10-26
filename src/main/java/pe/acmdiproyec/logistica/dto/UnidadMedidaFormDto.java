package pe.acmdiproyec.logistica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario de alta y edicion de unidades de medida. Las restricciones
 * reflejan las columnas de {@code public.unidades_medida}:
 * {@code codigo varchar(20) not null unique}, {@code nombre varchar(100) not
 * null unique} y {@code abreviatura varchar(20) not null unique}.
 */
public class UnidadMedidaFormDto {

    private Long idUnidadMedida;

    @NotBlank(message = "El codigo es obligatorio.")
    @Size(max = 20, message = "El codigo no puede superar 20 caracteres.")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    private String nombre;

    @NotBlank(message = "La abreviatura es obligatoria.")
    @Size(max = 20, message = "La abreviatura no puede superar 20 caracteres.")
    private String abreviatura;

    private boolean activo = true;

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

    public String getAbreviatura() {
        return abreviatura;
    }

    public void setAbreviatura(String abreviatura) {
        this.abreviatura = abreviatura;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
