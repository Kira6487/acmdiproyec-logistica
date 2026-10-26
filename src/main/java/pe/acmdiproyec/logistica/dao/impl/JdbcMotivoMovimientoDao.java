package pe.acmdiproyec.logistica.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.MotivoMovimientoDao;
import pe.acmdiproyec.logistica.model.MotivoMovimiento;

/**
 * Implementacion JDBC de {@link MotivoMovimientoDao} sobre la tabla
 * {@code public.motivos_movimiento}. Todas las consultas usan parametros
 * posicionales.
 */
@Repository
public class JdbcMotivoMovimientoDao implements MotivoMovimientoDao {

    private static final String SELECT_BASE = """
        select
            id_motivo_movimiento,
            codigo,
            nombre,
            tipo,
            descripcion,
            activo
        from motivos_movimiento
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcMotivoMovimientoDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MotivoMovimiento> listarTodos() {
        return jdbcTemplate.query(
                SELECT_BASE + " order by tipo, nombre",
                (rs, rowNum) -> mapear(rs));
    }

    @Override
    public List<MotivoMovimiento> listarPorTipo(String tipo) {
        return jdbcTemplate.query(
                SELECT_BASE + " where activo = true and tipo = ?::tipo_movimiento order by nombre",
                (rs, rowNum) -> mapear(rs),
                tipo);
    }

    @Override
    public Optional<MotivoMovimiento> buscarPorId(Long idMotivoMovimiento) {
        List<MotivoMovimiento> motivos = jdbcTemplate.query(
                SELECT_BASE + " where id_motivo_movimiento = ?",
                (rs, rowNum) -> mapear(rs),
                idMotivoMovimiento);
        return motivos.stream().findFirst();
    }

    @Override
    public int guardar(MotivoMovimiento motivoMovimiento) {
        String sql = """
            insert into motivos_movimiento (codigo, nombre, tipo, descripcion, activo)
            values (?, ?, ?::tipo_movimiento, ?, ?)
            """;
        return jdbcTemplate.update(
                sql,
                motivoMovimiento.getCodigo(),
                motivoMovimiento.getNombre(),
                motivoMovimiento.getTipo(),
                motivoMovimiento.getDescripcion(),
                motivoMovimiento.isActivo());
    }

    @Override
    public int actualizar(MotivoMovimiento motivoMovimiento) {
        String sql = """
            update motivos_movimiento
               set codigo = ?,
                   nombre = ?,
                   tipo = ?::tipo_movimiento,
                   descripcion = ?,
                   activo = ?
             where id_motivo_movimiento = ?
            """;
        return jdbcTemplate.update(
                sql,
                motivoMovimiento.getCodigo(),
                motivoMovimiento.getNombre(),
                motivoMovimiento.getTipo(),
                motivoMovimiento.getDescripcion(),
                motivoMovimiento.isActivo(),
                motivoMovimiento.getIdMotivoMovimiento());
    }

    @Override
    public int eliminar(Long idMotivoMovimiento) {
        return jdbcTemplate.update(
                "delete from motivos_movimiento where id_motivo_movimiento = ?",
                idMotivoMovimiento);
    }

    @Override
    public int contarPorCodigo(String codigo, Long idMotivoMovimientoExcluido) {
        return contar(
                "select count(*) from motivos_movimiento where lower(codigo) = lower(?)",
                codigo,
                idMotivoMovimientoExcluido);
    }

    @Override
    public int contarPorNombreYTipo(String nombre, String tipo, Long idMotivoMovimientoExcluido) {
        String sql = """
            select count(*) from motivos_movimiento
             where lower(nombre) = lower(?)
               and tipo = ?::tipo_movimiento
            """;
        if (idMotivoMovimientoExcluido != null) {
            Integer total = jdbcTemplate.queryForObject(
                    sql + " and id_motivo_movimiento <> ?",
                    Integer.class,
                    nombre,
                    tipo,
                    idMotivoMovimientoExcluido);
            return total == null ? 0 : total;
        }
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, nombre, tipo);
        return total == null ? 0 : total;
    }

    @Override
    public long contarMovimientosAsociados(Long idMotivoMovimiento) {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from movimientos where id_motivo_movimiento = ?",
                Long.class,
                idMotivoMovimiento);
        return total == null ? 0 : total;
    }

    /**
     * Cuenta coincidencias sin distinguir mayusculas, excluyendo opcionalmente
     * el registro que se esta editando.
     */
    private int contar(String sql, String valor, Long idExcluido) {
        if (idExcluido != null) {
            Integer total = jdbcTemplate.queryForObject(
                    sql + " and id_motivo_movimiento <> ?",
                    Integer.class,
                    valor,
                    idExcluido);
            return total == null ? 0 : total;
        }
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, valor);
        return total == null ? 0 : total;
    }

    private MotivoMovimiento mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setIdMotivoMovimiento(rs.getLong("id_motivo_movimiento"));
        motivo.setCodigo(rs.getString("codigo"));
        motivo.setNombre(rs.getString("nombre"));
        motivo.setTipo(rs.getString("tipo"));
        motivo.setDescripcion(rs.getString("descripcion"));
        motivo.setActivo(rs.getBoolean("activo"));
        return motivo;
    }
}
