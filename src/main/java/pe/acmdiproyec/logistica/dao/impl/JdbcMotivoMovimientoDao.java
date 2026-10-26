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
