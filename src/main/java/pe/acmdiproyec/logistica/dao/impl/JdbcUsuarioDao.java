package pe.acmdiproyec.logistica.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.model.Rol;
import pe.acmdiproyec.logistica.model.Usuario;

@Repository
public class JdbcUsuarioDao implements UsuarioDao {

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
            from public.usuarios u
            inner join public.roles r on r.id_rol = u.id_rol
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcUsuarioDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        List<Usuario> usuarios = jdbcTemplate.query(
                SELECT_BASE + "where lower(u.correo) = lower(?)",
                (rs, rowNum) -> mapearUsuario(rs),
                correo);
        return usuarios.stream().findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        List<Usuario> usuarios = jdbcTemplate.query(
                SELECT_BASE + "where u.id_usuario = ?",
                (rs, rowNum) -> mapearUsuario(rs),
                id);
        return usuarios.stream().findFirst();
    }

    @Override
    public List<Usuario> listarTodos() {
        return jdbcTemplate.query(
                SELECT_BASE + "order by u.apellidos, u.nombres",
                (rs, rowNum) -> mapearUsuario(rs));
    }

    @Override
    public List<Rol> listarRolesActivos() {
        String sql = """
                select id_rol, codigo, nombre, descripcion, activo
                from public.roles
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
                insert into public.usuarios
                    (id_rol, codigo, nombres, apellidos, correo, password_hash, activo)
                values (?, ?, ?, ?, ?, ?, ?)
                """;
        return jdbcTemplate.update(sql,
                usuario.getRol().getIdRol(), usuario.getCodigo(), usuario.getNombres(),
                usuario.getApellidos(), usuario.getCorreo(), usuario.getPasswordHash(),
                usuario.isActivo());
    }

    @Override
    public int actualizar(Usuario usuario) {
        String sql = """
                update public.usuarios
                set id_rol = ?,
                    codigo = ?,
                    nombres = ?,
                    apellidos = ?,
                    correo = ?,
                    password_hash = ?,
                    activo = ?
                where id_usuario = ?
                """;
        return jdbcTemplate.update(sql,
                usuario.getRol().getIdRol(), usuario.getCodigo(), usuario.getNombres(),
                usuario.getApellidos(), usuario.getCorreo(), usuario.getPasswordHash(),
                usuario.isActivo(), usuario.getIdUsuario());
    }

    @Override
    public int desactivar(Long id) {
        return jdbcTemplate.update(
                "update public.usuarios set activo = false where id_usuario = ?", id);
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
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
