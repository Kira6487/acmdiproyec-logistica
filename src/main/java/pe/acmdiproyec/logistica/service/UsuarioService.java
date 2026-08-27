package pe.acmdiproyec.logistica.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.dto.UsuarioFormDto;
import pe.acmdiproyec.logistica.dto.UsuarioResponseDto;
import pe.acmdiproyec.logistica.model.Rol;
import pe.acmdiproyec.logistica.model.Usuario;

@Service
public class UsuarioService {

    private final UsuarioDao usuarioDao;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioDao usuarioDao,
            PasswordEncoder passwordEncoder) {
        this.usuarioDao = usuarioDao;
        this.passwordEncoder = passwordEncoder;
    }

    public void crearUsuario(Usuario usuario, String password) {
        String passwordHash = passwordEncoder.encode(password);
        usuario.setPasswordHash(passwordHash);
        usuario.setActivo(true);

        usuarioDao.guardar(usuario);
    }

    public void actualizarUsuario(Usuario usuario, String password) {
        if (password != null && !password.isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(password));
        }

        usuarioDao.actualizar(usuario);
    }

    public void desactivarUsuario(Long id) {
        usuarioDao.desactivar(id);
    }

    public List<UsuarioResponseDto> listarUsuarios() {
        return usuarioDao.listarTodos().stream()
                .map(this::convertirRespuesta)
                .collect(Collectors.toList());
    }

    public Usuario buscarUsuario(Long id) {
        return usuarioDao.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public List<Rol> listarRolesActivos() {
        return usuarioDao.listarRolesActivos();
    }

    public void crearDesdeFormulario(UsuarioFormDto formulario) {
        crearUsuario(convertirEntidad(formulario), formulario.getPassword());
    }

    public void actualizarDesdeFormulario(Long id, UsuarioFormDto formulario) {
        Usuario usuario = buscarUsuario(id);
        usuario.setRol(crearRol(formulario.getIdRol()));
        usuario.setCodigo(formulario.getCodigo());
        usuario.setNombres(formulario.getNombres());
        usuario.setApellidos(formulario.getApellidos());
        usuario.setCorreo(formulario.getCorreo());
        usuario.setActivo(formulario.isActivo());
        actualizarUsuario(usuario, formulario.getPassword());
    }

    private Usuario convertirEntidad(UsuarioFormDto formulario) {
        Usuario usuario = new Usuario();
        usuario.setRol(crearRol(formulario.getIdRol()));
        usuario.setCodigo(formulario.getCodigo());
        usuario.setNombres(formulario.getNombres());
        usuario.setApellidos(formulario.getApellidos());
        usuario.setCorreo(formulario.getCorreo());
        return usuario;
    }

    private Rol crearRol(Long idRol) {
        Rol rol = new Rol();
        rol.setIdRol(idRol);
        return rol;
    }

    private UsuarioResponseDto convertirRespuesta(Usuario usuario) {
        UsuarioResponseDto respuesta = new UsuarioResponseDto();
        respuesta.setIdUsuario(usuario.getIdUsuario());
        respuesta.setCodigo(usuario.getCodigo());
        respuesta.setNombres(usuario.getNombres());
        respuesta.setApellidos(usuario.getApellidos());
        respuesta.setCorreo(usuario.getCorreo());
        respuesta.setActivo(usuario.isActivo());
        if (usuario.getRol() != null) {
            respuesta.setRolCodigo(usuario.getRol().getCodigo());
            respuesta.setRolNombre(usuario.getRol().getNombre());
        }
        return respuesta;
    }
}