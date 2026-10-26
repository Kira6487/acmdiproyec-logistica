package pe.acmdiproyec.logistica.dao.impl;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.InventarioDao;
import pe.acmdiproyec.logistica.model.Categoria;
import pe.acmdiproyec.logistica.model.Inventario;
import pe.acmdiproyec.logistica.model.Producto;
import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Implementacion JDBC de {@link InventarioDao}. Solo lectura: el stock lo
 * mantienen los disparadores de la base de datos.
 */
@Repository
public class JdbcInventarioDao implements InventarioDao {

    private static final String SELECT_BASE = """
        select
            i.id_inventario,
            i.stock,
            p.id_producto,
            p.codigo,
            p.nombre,
            p.descripcion,
            p.stock_minimo,
            p.activo,
            c.id_categoria,
            c.nombre as categoria_nombre,
            u.id_unidad_medida,
            u.nombre as unidad_nombre,
            u.abreviatura as unidad_abreviatura
        from inventario i
        inner join productos p on p.id_producto = i.id_producto
        inner join categorias c on c.id_categoria = p.id_categoria
        inner join unidades_medida u on u.id_unidad_medida = p.id_unidad_medida
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInventarioDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Inventario> listarTodo() {
        return jdbcTemplate.query(
                SELECT_BASE + " order by p.nombre",
                (rs, rowNum) -> {
                    Categoria categoria = new Categoria();
                    categoria.setIdCategoria(rs.getLong("id_categoria"));
                    categoria.setNombre(rs.getString("categoria_nombre"));

                    UnidadMedida unidadMedida = new UnidadMedida();
                    unidadMedida.setIdUnidadMedida(rs.getLong("id_unidad_medida"));
                    unidadMedida.setNombre(rs.getString("unidad_nombre"));
                    unidadMedida.setAbreviatura(rs.getString("unidad_abreviatura"));

                    Producto producto = new Producto();
                    producto.setIdProducto(rs.getLong("id_producto"));
                    producto.setCodigo(rs.getString("codigo"));
                    producto.setNombre(rs.getString("nombre"));
                    producto.setDescripcion(rs.getString("descripcion"));
                    producto.setStockMinimo(rs.getBigDecimal("stock_minimo"));
                    producto.setActivo(rs.getBoolean("activo"));
                    producto.setCategoria(categoria);
                    producto.setUnidadMedida(unidadMedida);

                    Inventario inventario = new Inventario();
                    inventario.setIdInventario(rs.getLong("id_inventario"));
                    inventario.setProducto(producto);
                    inventario.setStock(rs.getBigDecimal("stock"));
                    return inventario;
                });
    }
}
