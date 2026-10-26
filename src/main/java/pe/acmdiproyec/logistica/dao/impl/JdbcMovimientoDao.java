package pe.acmdiproyec.logistica.dao.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.MovimientoDao;
import pe.acmdiproyec.logistica.model.Categoria;
import pe.acmdiproyec.logistica.model.MotivoMovimiento;
import pe.acmdiproyec.logistica.model.Movimiento;
import pe.acmdiproyec.logistica.model.MovimientoDetalle;
import pe.acmdiproyec.logistica.model.Producto;
import pe.acmdiproyec.logistica.model.UnidadMedida;
import pe.acmdiproyec.logistica.model.Usuario;

/**
 * Implementacion JDBC de {@link MovimientoDao} sobre las tablas
 * {@code public.movimientos} y {@code public.movimiento_detalle}.
 *
 * Esta clase NUNCA escribe sobre {@code public.inventario}: al insertar cada
 * detalle, el disparador {@code trg_movimiento_detalle_aplicar_inventario}
 * bloquea la fila de inventario, valida el saldo disponible y actualiza el
 * stock. Duplicar esa logica aqui provocaria descuadres.
 */
@Repository
public class JdbcMovimientoDao implements MovimientoDao {

    private static final String SELECT_MOVIMIENTO = """
        select
            m.id_movimiento,
            m.codigo,
            m.fecha_movimiento,
            m.tipo,
            m.estado,
            m.observacion,
            m.fecha_anulacion,
            m.motivo_anulacion,
            mm.id_motivo_movimiento,
            mm.codigo as motivo_codigo,
            mm.nombre as motivo_nombre,
            mm.tipo as motivo_tipo,
            mm.descripcion as motivo_descripcion,
            mm.activo as motivo_activo,
            u.id_usuario,
            u.nombres as usuario_nombres,
            u.apellidos as usuario_apellidos,
            u.correo as usuario_correo,
            ua.id_usuario as anulacion_id_usuario,
            ua.nombres as anulacion_nombres,
            ua.apellidos as anulacion_apellidos,
            ua.correo as anulacion_correo
        from movimientos m
        inner join motivos_movimiento mm on mm.id_motivo_movimiento = m.id_motivo_movimiento
        inner join usuarios u on u.id_usuario = m.id_usuario
        left join usuarios ua on ua.id_usuario = m.id_usuario_anulacion
        """;

    private static final String SELECT_DETALLE = """
        select
            d.id_movimiento_detalle,
            d.id_movimiento,
            d.cantidad,
            i.stock as stock_actual,
            p.id_producto,
            p.codigo as producto_codigo,
            p.nombre as producto_nombre,
            p.descripcion as producto_descripcion,
            p.stock_minimo as producto_stock_minimo,
            p.activo as producto_activo,
            c.id_categoria,
            c.nombre as categoria_nombre,
            um.id_unidad_medida,
            um.nombre as unidad_nombre,
            um.abreviatura as unidad_abreviatura
        from movimiento_detalle d
        inner join productos p on p.id_producto = d.id_producto
        inner join categorias c on c.id_categoria = p.id_categoria
        inner join unidades_medida um on um.id_unidad_medida = p.id_unidad_medida
        left join inventario i on i.id_producto = p.id_producto
        where d.id_movimiento = ?
        order by p.nombre
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcMovimientoDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Movimiento> listarTodos(
            Long idProducto,
            String tipo,
            String estado,
            OffsetDateTime desde,
            OffsetDateTime hasta) {

        StringBuilder sql = new StringBuilder(SELECT_MOVIMIENTO).append(" where 1 = 1");
        List<Object> parametros = new ArrayList<>();

        if (idProducto != null) {
            sql.append("""
                 and exists (select 1 from movimiento_detalle d
                              where d.id_movimiento = m.id_movimiento
                                and d.id_producto = ?)
                """);
            parametros.add(idProducto);
        }
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" and m.tipo = ?::tipo_movimiento");
            parametros.add(tipo);
        }
        if (estado != null && !estado.isBlank()) {
            sql.append(" and m.estado = ?::estado_movimiento");
            parametros.add(estado);
        }
        if (desde != null) {
            sql.append(" and m.fecha_movimiento >= ?");
            parametros.add(desde);
        }
        if (hasta != null) {
            sql.append(" and m.fecha_movimiento <= ?");
            parametros.add(hasta);
        }
        sql.append(" order by m.fecha_movimiento desc, m.id_movimiento desc");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> mapearMovimiento(rs),
                parametros.toArray());
    }

    @Override
    public Optional<Movimiento> buscarPorId(Long idMovimiento) {
        List<Movimiento> movimientos = jdbcTemplate.query(
                SELECT_MOVIMIENTO + " where m.id_movimiento = ?",
                (rs, rowNum) -> mapearMovimiento(rs),
                idMovimiento);
        return movimientos.stream().findFirst();
    }

    @Override
    public List<MovimientoDetalle> listarDetalles(Long idMovimiento) {
        return jdbcTemplate.query(
                SELECT_DETALLE,
                (rs, rowNum) -> mapearDetalle(rs),
                idMovimiento);
    }

    /**
     * Inserta la cabecera y devuelve el identificador generado; es necesario
     * para poder insertar despues sus detalles.
     */
    @Override
    public Long guardar(Movimiento movimiento) {
        String sql = """
            insert into movimientos
                (codigo, id_motivo_movimiento, id_usuario, tipo, observacion)
            values (?, ?, ?, ?::tipo_movimiento, ?)
            """;

        KeyHolder claves = new GeneratedKeyHolder();
        jdbcTemplate.update(conexion -> {
            PreparedStatement sentencia = conexion.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);
            sentencia.setString(1, movimiento.getCodigo());
            sentencia.setLong(2, movimiento.getMotivoMovimiento().getIdMotivoMovimiento());
            sentencia.setLong(3, movimiento.getUsuario().getIdUsuario());
            sentencia.setString(4, movimiento.getTipo());
            sentencia.setString(5, movimiento.getObservacion());
            return sentencia;
        }, claves);

        Number clave = claves.getKey();
        return clave == null ? null : clave.longValue();
    }

    /**
     * Inserta una linea de detalle. El disparador
     * {@code trg_movimiento_detalle_aplicar_inventario} es quien actualiza el
     * stock y quien rechaza salidas sin saldo suficiente.
     */
    @Override
    public int guardarDetalle(MovimientoDetalle detalle) {
        String sql = """
            insert into movimiento_detalle (id_movimiento, id_producto, cantidad)
            values (?, ?, ?)
            """;
        return jdbcTemplate.update(
                sql,
                detalle.getIdMovimiento(),
                detalle.getProducto().getIdProducto(),
                detalle.getCantidad());
    }

    /**
     * Anulacion condicional del movimiento.
     *
     * La clausula {@code where estado = 'VIGENTE'} es la que impide una doble
     * anulacion: si otro proceso ya anulo el movimiento, la sentencia no
     * modifica ninguna fila y el servicio lo detecta. Al cambiar el estado, el
     * disparador {@code trg_movimientos_revertir_anulacion} revierte el stock en
     * {@code inventario}; esta clase no interviene en ese calculo.
     */
    @Override
    public int anular(Long idMovimiento, Long idUsuarioAnulacion, String motivoAnulacion) {
        String sql = """
            update movimientos
               set estado = 'ANULADO',
                   id_usuario_anulacion = ?,
                   fecha_anulacion = now(),
                   motivo_anulacion = ?
             where id_movimiento = ?
               and estado = 'VIGENTE'
            """;
        return jdbcTemplate.update(sql, idUsuarioAnulacion, motivoAnulacion, idMovimiento);
    }

    private Movimiento mapearMovimiento(java.sql.ResultSet rs) throws java.sql.SQLException {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setIdMotivoMovimiento(rs.getLong("id_motivo_movimiento"));
        motivo.setCodigo(rs.getString("motivo_codigo"));
        motivo.setNombre(rs.getString("motivo_nombre"));
        motivo.setTipo(rs.getString("motivo_tipo"));
        motivo.setDescripcion(rs.getString("motivo_descripcion"));
        motivo.setActivo(rs.getBoolean("motivo_activo"));

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getLong("id_usuario"));
        usuario.setNombres(rs.getString("usuario_nombres"));
        usuario.setApellidos(rs.getString("usuario_apellidos"));
        usuario.setCorreo(rs.getString("usuario_correo"));

        long idUsuarioAnulacion = rs.getLong("anulacion_id_usuario");
        Usuario usuarioAnulacion = null;
        if (!rs.wasNull()) {
            usuarioAnulacion = new Usuario();
            usuarioAnulacion.setIdUsuario(idUsuarioAnulacion);
            usuarioAnulacion.setNombres(rs.getString("anulacion_nombres"));
            usuarioAnulacion.setApellidos(rs.getString("anulacion_apellidos"));
            usuarioAnulacion.setCorreo(rs.getString("anulacion_correo"));
        }

        Movimiento movimiento = new Movimiento();
        movimiento.setIdMovimiento(rs.getLong("id_movimiento"));
        movimiento.setCodigo(rs.getString("codigo"));
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setUsuario(usuario);
        movimiento.setFechaMovimiento(rs.getObject("fecha_movimiento", OffsetDateTime.class));
        movimiento.setTipo(rs.getString("tipo"));
        movimiento.setEstado(rs.getString("estado"));
        movimiento.setObservacion(rs.getString("observacion"));
        movimiento.setUsuarioAnulacion(usuarioAnulacion);
        movimiento.setFechaAnulacion(rs.getObject("fecha_anulacion", OffsetDateTime.class));
        movimiento.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        return movimiento;
    }

    private MovimientoDetalle mapearDetalle(java.sql.ResultSet rs) throws java.sql.SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getLong("id_categoria"));
        categoria.setNombre(rs.getString("categoria_nombre"));

        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setIdUnidadMedida(rs.getLong("id_unidad_medida"));
        unidadMedida.setNombre(rs.getString("unidad_nombre"));
        unidadMedida.setAbreviatura(rs.getString("unidad_abreviatura"));

        Producto producto = new Producto();
        producto.setIdProducto(rs.getLong("id_producto"));
        producto.setCodigo(rs.getString("producto_codigo"));
        producto.setNombre(rs.getString("producto_nombre"));
        producto.setDescripcion(rs.getString("producto_descripcion"));
        producto.setStockMinimo(rs.getBigDecimal("producto_stock_minimo"));
        producto.setActivo(rs.getBoolean("producto_activo"));
        producto.setCategoria(categoria);
        producto.setUnidadMedida(unidadMedida);

        BigDecimal stockActual = rs.getBigDecimal("stock_actual");

        MovimientoDetalle detalle = new MovimientoDetalle();
        detalle.setIdMovimientoDetalle(rs.getLong("id_movimiento_detalle"));
        detalle.setIdMovimiento(rs.getLong("id_movimiento"));
        detalle.setProducto(producto);
        detalle.setCantidad(rs.getBigDecimal("cantidad"));
        detalle.setStockActual(stockActual);
        return detalle;
    }
}
