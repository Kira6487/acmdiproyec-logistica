package pe.acmdiproyec.logistica.dao.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.ProductoDao;
import pe.acmdiproyec.logistica.model.Categoria;
import pe.acmdiproyec.logistica.model.Producto;
import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Implementacion JDBC de {@link ProductoDao} sobre la tabla
 * {@code public.productos}. Todas las consultas usan parametros posicionales.
 *
 * El alta de productos no inserta filas en {@code inventario}: esa tarea
 * corresponde al disparador {@code trg_productos_crear_inventario} de la base
 * de datos.
 */
@Repository
public class JdbcProductoDao implements ProductoDao {

    private static final String SELECT_BASE = """
        select
            p.id_producto,
            p.id_categoria,
            p.id_unidad_medida,
            p.codigo,
            p.nombre,
            p.descripcion,
            p.stock_minimo,
            p.activo,
            c.codigo as categoria_codigo,
            c.nombre as categoria_nombre,
            c.descripcion as categoria_descripcion,
            c.activo as categoria_activo,
            u.codigo as unidad_codigo,
            u.nombre as unidad_nombre,
            u.abreviatura as unidad_abreviatura,
            u.activo as unidad_activo
        from productos p
        inner join categorias c on c.id_categoria = p.id_categoria
        inner join unidades_medida u on u.id_unidad_medida = p.id_unidad_medida
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductoDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Producto> listarTodos() {
        return jdbcTemplate.query(
                SELECT_BASE + " order by p.nombre",
                (rs, rowNum) -> mapear(rs));
    }

    @Override
    public Optional<Producto> buscarPorId(Long idProducto) {
        List<Producto> productos = jdbcTemplate.query(
                SELECT_BASE + " where p.id_producto = ?",
                (rs, rowNum) -> mapear(rs),
                idProducto);
        return productos.stream().findFirst();
    }

    @Override
    public int guardar(Producto producto) {
        String sql = """
            insert into productos
                (id_categoria, id_unidad_medida, codigo, nombre, descripcion, stock_minimo, activo)
            values (?, ?, ?, ?, ?, ?, ?)
            """;
        return jdbcTemplate.update(
                sql,
                producto.getCategoria().getIdCategoria(),
                producto.getUnidadMedida().getIdUnidadMedida(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getStockMinimo(),
                producto.isActivo());
    }

    @Override
    public int actualizar(Producto producto) {
        String sql = """
            update productos
               set id_categoria = ?,
                   id_unidad_medida = ?,
                   codigo = ?,
                   nombre = ?,
                   descripcion = ?,
                   stock_minimo = ?,
                   activo = ?
             where id_producto = ?
            """;
        return jdbcTemplate.update(
                sql,
                producto.getCategoria().getIdCategoria(),
                producto.getUnidadMedida().getIdUnidadMedida(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getStockMinimo(),
                producto.isActivo(),
                producto.getIdProducto());
    }

    @Override
    public int eliminar(Long idProducto) {
        return jdbcTemplate.update(
                "delete from productos where id_producto = ?",
                idProducto);
    }

    @Override
    public int contarPorCodigo(String codigo, Long idProductoExcluido) {
        return contar(
                "select count(*) from productos where lower(codigo) = lower(?)",
                codigo,
                idProductoExcluido);
    }

    @Override
    public int contarPorNombre(String nombre, Long idProductoExcluido) {
        return contar(
                "select count(*) from productos where lower(nombre) = lower(?)",
                nombre,
                idProductoExcluido);
    }

    @Override
    public long contarMovimientosAsociados(Long idProducto) {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from movimiento_detalle where id_producto = ?",
                Long.class,
                idProducto);
        return total == null ? 0 : total;
    }

    @Override
    public BigDecimal obtenerStockActual(Long idProducto) {
        List<BigDecimal> stocks = jdbcTemplate.query(
                "select stock from inventario where id_producto = ?",
                (rs, rowNum) -> rs.getBigDecimal("stock"),
                idProducto);
        return stocks.stream().findFirst().orElse(BigDecimal.ZERO);
    }

    /**
     * Cuenta coincidencias sin distinguir mayusculas, excluyendo opcionalmente
     * el registro que se esta editando.
     */
    private int contar(String sql, String valor, Long idExcluido) {
        if (idExcluido != null) {
            Integer total = jdbcTemplate.queryForObject(
                    sql + " and id_producto <> ?",
                    Integer.class,
                    valor,
                    idExcluido);
            return total == null ? 0 : total;
        }
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, valor);
        return total == null ? 0 : total;
    }

    private Producto mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getLong("id_categoria"));
        categoria.setCodigo(rs.getString("categoria_codigo"));
        categoria.setNombre(rs.getString("categoria_nombre"));
        categoria.setDescripcion(rs.getString("categoria_descripcion"));
        categoria.setActivo(rs.getBoolean("categoria_activo"));

        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setIdUnidadMedida(rs.getLong("id_unidad_medida"));
        unidadMedida.setCodigo(rs.getString("unidad_codigo"));
        unidadMedida.setNombre(rs.getString("unidad_nombre"));
        unidadMedida.setAbreviatura(rs.getString("unidad_abreviatura"));
        unidadMedida.setActivo(rs.getBoolean("unidad_activo"));

        Producto producto = new Producto();
        producto.setIdProducto(rs.getLong("id_producto"));
        producto.setCategoria(categoria);
        producto.setUnidadMedida(unidadMedida);
        producto.setCodigo(rs.getString("codigo"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        producto.setActivo(rs.getBoolean("activo"));
        return producto;
    }
}
