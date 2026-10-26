package pe.acmdiproyec.logistica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario de anulacion de un movimiento de inventario.
 *
 * El motivo es obligatorio porque la restriccion
 * {@code ck_movimientos_anulacion} de {@code public.movimientos} exige que
 * {@code motivo_anulacion} no sea nulo ni este en blanco cuando el estado es
 * {@code ANULADO}. La longitud se ajusta a la columna
 * {@code motivo_anulacion varchar(500)}.
 */
public class AnulacionMovimientoFormDto {

    @NotBlank(message = "El motivo de la anulacion es obligatorio.")
    @Size(max = 500, message = "El motivo de la anulacion no puede superar 500 caracteres.")
    private String motivoAnulacion;

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
}
