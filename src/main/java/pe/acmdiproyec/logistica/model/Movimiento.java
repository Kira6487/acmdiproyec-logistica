package pe.acmdiproyec.logistica.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Movimiento de inventario, cabecera del conjunto de {@link MovimientoDetalle}.
 * Corresponde con la tabla {@code public.movimientos}.
 *
 * Solo existen dos estados: {@code VIGENTE} y {@code ANULADO}; el esquema no
 * contempla borradores. La anulacion exige usuario, fecha y motivo.
 */
public class Movimiento {

    private Long idMovimiento;
    private String codigo;
    private MotivoMovimiento motivoMovimiento;
    private Usuario usuario;
    private OffsetDateTime fechaMovimiento;
    private String tipo;
    private String estado;
    private String observacion;
    private Usuario usuarioAnulacion;
    private OffsetDateTime fechaAnulacion;
    private String motivoAnulacion;
    private List<MovimientoDetalle> detalles = new ArrayList<>();

    public Long getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(Long idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public MotivoMovimiento getMotivoMovimiento() {
        return motivoMovimiento;
    }

    public void setMotivoMovimiento(MotivoMovimiento motivoMovimiento) {
        this.motivoMovimiento = motivoMovimiento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public OffsetDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(OffsetDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Usuario getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(Usuario usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public OffsetDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(OffsetDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public List<MovimientoDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<MovimientoDetalle> detalles) {
        this.detalles = detalles == null ? new ArrayList<>() : detalles;
    }

    /** Cantidad total de unidades involucradas en el movimiento. */
    public BigDecimal getTotalCantidad() {
        return detalles.stream()
                .map(MovimientoDetalle::getCantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
