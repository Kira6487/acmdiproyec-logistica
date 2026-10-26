package pe.acmdiproyec.logistica.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.CategoriaDao;
import pe.acmdiproyec.logistica.model.Categoria;

/**
 * Implementacion JDBC de {@link CategoriaDao} sobre la tabla
 * {@code public.categorias}. Todas las consultas usan parametros
 * posicionales.
 */
@Repository
public class JdbcCategoriaDao implements CategoriaDao {

    private static final String SELECT_BASE = """
        select
            id_categoria,
            codigo,
            nombre,
            descripcion,
            activo
        from categorias
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCategoriaDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Categoria> listarTodas() {
        return jdbcTemplate.query(
                SELECT_BASE + " order by nombre",
                (rs, rowNum) -> mapear(rs));
    }

    @Override
    public Optional<Categoria> buscarPorId(Long idCategoria) {
        List<Categoria> categorias = jdbcTemplate.query(
                SELECT_BASE + " where id_categoria = ?",
                (rs, rowNum) -> mapear(rs),
                idCategoria);
        return categorias.stream().findFirst();
    }

    @Override
    public int guardar(Categoria categoria) {
        String sql = """
            insert into categorias (codigo, nombre, descripcion, activo)
            values (?, ?, ?, ?)
            """;
        return jdbcTemplate.update(
                sql,
                categoria.getCodigo(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.isActivo());
    }

    @Override
    public int actualizar(Categoria categoria) {
        String sql = """
            update categorias
               set codigo = ?,
                   nombre = ?,
                   descripcion = ?,
                   activo = ?
             where id_categoria = ?
            """;
        return jdbcTemplate.update(
                sql,
                categoria.getCodigo(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.isActivo(),
                categoria.getIdCategoria());
    }

    @Override
    public int eliminar(Long idCategoria) {
        return jdbcTemplate.update(
                "delete from categorias where id_categoria = ?",
                idCategoria);
    }

    @Override
    public int contarPorCodigo(String codigo, Long idCategoriaExcluida) {
        return contar(
                "select count(*) from categorias where lower(codigo) = lower(?)",
                codigo,
                idCategoriaExcluida);
    }

    @Override
    public int contarPorNombre(String nombre, Long idCategoriaExcluida) {
        return contar(
                "select count(*) from categorias where lower(nombre) = lower(?)",
                nombre,
                idCategoriaExcluida);
    }

    @Override
    public long contarProductosAsociados(Long idCategoria) {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from productos where id_categoria = ?",
                Long.class,
                idCategoria);
        return total == null ? 0 : total;
    }

    /**
     * Cuenta coincidencias sin distinguir mayusculas, excluyendo opcionalmente
     * el registro que se esta editando.
     */
    private int contar(String sql, String valor, Long idExcluido) {
        if (idExcluido != null) {
            Integer total = jdbcTemplate.queryForObject(
                    sql + " and id_categoria <> ?",
                    Integer.class,
                    valor,
                    idExcluido);
            return total == null ? 0 : total;
        }
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, valor);
        return total == null ? 0 : total;
    }

    private Categoria mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getLong("id_categoria"));
        categoria.setCodigo(rs.getString("codigo"));
        categoria.setNombre(rs.getString("nombre"));
        categoria.setDescripcion(rs.getString("descripcion"));
        categoria.setActivo(rs.getBoolean("activo"));
        return categoria;
    }
}
