package pe.acmdiproyec.logistica.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.UnidadMedidaDao;
import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Implementacion JDBC de {@link UnidadMedidaDao} sobre la tabla
 * {@code public.unidades_medida}. Todas las consultas usan parametros
 * posicionales.
 */
@Repository
public class JdbcUnidadMedidaDao implements UnidadMedidaDao {

    private static final String SELECT_BASE = """
        select
            id_unidad_medida,
            codigo,
            nombre,
            abreviatura,
            activo
        from unidades_medida
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcUnidadMedidaDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<UnidadMedida> listarTodas() {
        return jdbcTemplate.query(
                SELECT_BASE + " order by nombre",
                (rs, rowNum) -> mapear(rs));
    }

    @Override
    public Optional<UnidadMedida> buscarPorId(Long idUnidadMedida) {
        List<UnidadMedida> unidades = jdbcTemplate.query(
                SELECT_BASE + " where id_unidad_medida = ?",
                (rs, rowNum) -> mapear(rs),
                idUnidadMedida);
        return unidades.stream().findFirst();
    }

    @Override
    public int guardar(UnidadMedida unidadMedida) {
        String sql = """
            insert into unidades_medida (codigo, nombre, abreviatura, activo)
            values (?, ?, ?, ?)
            """;
        return jdbcTemplate.update(
                sql,
                unidadMedida.getCodigo(),
                unidadMedida.getNombre(),
                unidadMedida.getAbreviatura(),
                unidadMedida.isActivo());
    }

    @Override
    public int actualizar(UnidadMedida unidadMedida) {
        String sql = """
            update unidades_medida
               set codigo = ?,
                   nombre = ?,
                   abreviatura = ?,
                   activo = ?
             where id_unidad_medida = ?
            """;
        return jdbcTemplate.update(
                sql,
                unidadMedida.getCodigo(),
                unidadMedida.getNombre(),
                unidadMedida.getAbreviatura(),
                unidadMedida.isActivo(),
                unidadMedida.getIdUnidadMedida());
    }

    @Override
    public int eliminar(Long idUnidadMedida) {
        return jdbcTemplate.update(
                "delete from unidades_medida where id_unidad_medida = ?",
                idUnidadMedida);
    }

    @Override
    public int contarPorCodigo(String codigo, Long idUnidadMedidaExcluida) {
        return contar(
                "select count(*) from unidades_medida where lower(codigo) = lower(?)",
                codigo,
                idUnidadMedidaExcluida);
    }

    @Override
    public int contarPorNombre(String nombre, Long idUnidadMedidaExcluida) {
        return contar(
                "select count(*) from unidades_medida where lower(nombre) = lower(?)",
                nombre,
                idUnidadMedidaExcluida);
    }

    @Override
    public int contarPorAbreviatura(String abreviatura, Long idUnidadMedidaExcluida) {
        return contar(
                "select count(*) from unidades_medida where lower(abreviatura) = lower(?)",
                abreviatura,
                idUnidadMedidaExcluida);
    }

    @Override
    public long contarProductosAsociados(Long idUnidadMedida) {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from productos where id_unidad_medida = ?",
                Long.class,
                idUnidadMedida);
        return total == null ? 0 : total;
    }

    /**
     * Cuenta coincidencias sin distinguir mayusculas, excluyendo opcionalmente
     * el registro que se esta editando.
     */
    private int contar(String sql, String valor, Long idExcluido) {
        if (idExcluido != null) {
            Integer total = jdbcTemplate.queryForObject(
                    sql + " and id_unidad_medida <> ?",
                    Integer.class,
                    valor,
                    idExcluido);
            return total == null ? 0 : total;
        }
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, valor);
        return total == null ? 0 : total;
    }

    private UnidadMedida mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setIdUnidadMedida(rs.getLong("id_unidad_medida"));
        unidadMedida.setCodigo(rs.getString("codigo"));
        unidadMedida.setNombre(rs.getString("nombre"));
        unidadMedida.setAbreviatura(rs.getString("abreviatura"));
        unidadMedida.setActivo(rs.getBoolean("activo"));
        return unidadMedida;
    }
}
