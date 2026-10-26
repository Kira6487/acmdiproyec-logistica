package pe.acmdiproyec.logistica.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario de registro de un movimiento de inventario.
 *
 * El {@code tipo} del movimiento (ENTRADA o SALIDA) no se acepta del navegador:
 * se deriva del motivo seleccionado, de modo que la clave foranea compuesta
 * {@code (id_motivo_movimiento, tipo)} siempre es coherente.
 */
public class MovimientoFormDto {

    @NotNull(message = "El motivo del movimiento es obligatorio.")
    private Long idMotivoMovimiento;

    @Size(max = 500, message = "La observacion no puede superar 500 caracteres.")
    private String observacion;

    @Valid
    private List<MovimientoDetalleFormDto> lineas = new ArrayList<>();

    public Long getIdMotivoMovimiento() {
        return idMotivoMovimiento;
    }

    public void setIdMotivoMovimiento(Long idMotivoMovimiento) {
        this.idMotivoMovimiento = idMotivoMovimiento;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public List<MovimientoDetalleFormDto> getLineas() {
        return lineas;
    }

    public void setLineas(List<MovimientoDetalleFormDto> lineas) {
        this.lineas = lineas == null ? new ArrayList<>() : lineas;
    }
}
