package pe.acmdiproyec.logistica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario de alta y edicion de motivos de movimiento. Las restricciones
 * reflejan las columnas de {@code public.motivos_movimiento}:
 * {@code codigo varchar(30) not null unique}, {@code nombre varchar(100) not
 * null}, {@code tipo public.tipo_movimiento not null} y
 * {@code descripcion varchar(250)}.
 *
 * El tipo solo admite los valores del enum {@code tipo_movimiento}
 * ({@code ENTRADA} o {@code SALIDA}); la validacion de pertenencia al enum la
 * realiza el servicio.
 */
public class MotivoMovimientoFormDto {

    private Long idMotivoMovimiento;

    @NotBlank(message = "El codigo es obligatorio.")
    @Size(max = 30, message = "El codigo no puede superar 30 caracteres.")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El tipo de movimiento es obligatorio.")
    private String tipo;

    @Size(max = 250, message = "La descripcion no puede superar 250 caracteres.")
    private String descripcion;

    private boolean activo = true;

    public Long getIdMotivoMovimiento() {
        return idMotivoMovimiento;
    }

    public void setIdMotivoMovimiento(Long idMotivoMovimiento) {
        this.idMotivoMovimiento = idMotivoMovimiento;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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
