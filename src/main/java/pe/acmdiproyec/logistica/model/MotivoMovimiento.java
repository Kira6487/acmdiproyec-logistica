package pe.acmdiproyec.logistica.model;

/**
 * Motivo de un movimiento de inventario. Corresponde con la tabla
 * {@code public.motivos_movimiento}. El {@code tipo} define si el movimiento es
 * de entrada o de salida y debe coincidir con el del movimiento.
 */
public class MotivoMovimiento {

    private Long idMotivoMovimiento;
    private String codigo;
    private String nombre;
    private String tipo;
    private String descripcion;
    private boolean activo;

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
