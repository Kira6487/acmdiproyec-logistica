package pe.acmdiproyec.logistica.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.model.Rol;
import pe.acmdiproyec.logistica.model.Usuario;

@Repository
public class JdbcUsuarioDao implements UsuarioDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUsuarioDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SELECT_BASE = """
        select
            u.id_usuario,
            u.id_rol,
            u.codigo,
            u.nombres,
            u.apellidos,
            u.correo,
            u.password_hash,
            u.activo,
            r.codigo as rol_codigo,
            r.nombre as rol_nombre,
            r.descripcion as rol_descripcion,
            r.activo as rol_activo
        from usuarios u
        inner join roles r on r.id_rol = u.id_rol
        """;

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        String sql = SELECT_BASE + """
            where lower(u.correo) = lower(?)
            """;

        List<Usuario> usuarios = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapearUsuario(rs),
                correo
        );

        return usuarios.stream().findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        String sql = SELECT_BASE + " where u.id_usuario = ?";

        List<Usuario> usuarios = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapearUsuario(rs),
                id
        );

        return usuarios.stream().findFirst();
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = SELECT_BASE + " order by u.apellidos, u.nombres";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapearUsuario(rs)
        );
    }

    @Override
    public List<Rol> listarRolesActivos() {
        String sql = """
            select id_rol, codigo, nombre, descripcion, activo
            from roles
            where activo = true
            order by nombre
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Rol rol = new Rol();
            rol.setIdRol(rs.getLong("id_rol"));
            rol.setCodigo(rs.getString("codigo"));
            rol.setNombre(rs.getString("nombre"));
            rol.setDescripcion(rs.getString("descripcion"));
            rol.setActivo(rs.getBoolean("activo"));
            return rol;
        });
    }

    @Override
    public int guardar(Usuario usuario) {
        String sql = """
            insert into usuarios
                (id_rol, codigo, nombres, apellidos, correo, password_hash, activo)
            values (?, ?, ?, ?, ?, ?, ?)
            """;

        return jdbcTemplate.update(
                sql,
                usuario.getRol().getIdRol(),
                usuario.getCodigo(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getPasswordHash(),
                usuario.isActivo()
        );
    }

    @Override
    public int actualizar(Usuario usuario) {
        String sql = """
            update usuarios
            set id_rol = ?,
                codigo = ?,
                nombres = ?,
                apellidos = ?,
                correo = ?,
                activo = ?
            where id_usuario = ?
            """;

        return jdbcTemplate.update(
                sql,
                usuario.getRol().getIdRol(),
                usuario.getCodigo(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.isActivo(),
                usuario.getIdUsuario()
        );
    }

    @Override
    public int desactivar(Long id) {
        return jdbcTemplate.update(
                "update usuarios set activo = false where id_usuario = ?",
                id
        );
    }

    private Usuario mapearUsuario(
        java.sql.ResultSet rs) throws java.sql.SQLException {

    Rol rol = new Rol();
    rol.setIdRol(rs.getLong("id_rol"));
    rol.setCodigo(rs.getString("rol_codigo"));
    rol.setNombre(rs.getString("rol_nombre"));
    rol.setDescripcion(rs.getString("rol_descripcion"));
    rol.setActivo(rs.getBoolean("rol_activo"));

    Usuario usuario = new Usuario();
    usuario.setIdUsuario(rs.getLong("id_usuario"));
    usuario.setRol(rol);
    usuario.setCodigo(rs.getString("codigo"));
    usuario.setNombres(rs.getString("nombres"));
    usuario.setApellidos(rs.getString("apellidos"));
    usuario.setCorreo(rs.getString("correo"));
    usuario.setPasswordHash(rs.getString("password_hash"));
    usuario.setActivo(rs.getBoolean("activo"));

    return usuario;
}
}
