package pe.acmdiproyec.logistica.model;

/**
 * Unidad de medida de los productos. Corresponde con la tabla
 * {@code public.unidades_medida} de la base de datos.
 */
public class UnidadMedida {

    private Long idUnidadMedida;
    private String codigo;
    private String nombre;
    private String abreviatura;
    private boolean activo;

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
