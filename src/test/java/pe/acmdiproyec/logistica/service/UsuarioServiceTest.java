package pe.acmdiproyec.logistica.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.model.Rol;
import pe.acmdiproyec.logistica.model.Usuario;

class UsuarioServiceTest {

    @Test
    void actualizaElHashCuandoSeCambiaLaContrasena() {
        Usuario usuario = usuarioExistente();
        UsuarioDaoGrabador dao = new UsuarioDaoGrabador();
        UsuarioService service = new UsuarioService(dao, new BCryptPasswordEncoder());

        service.actualizarUsuario(usuario, "nueva-clave");

        assertThat(dao.usuarioActualizado).isSameAs(usuario);
        assertThat(new BCryptPasswordEncoder().matches("nueva-clave", usuario.getPasswordHash())).isTrue();
    }

    private Usuario usuarioExistente() {
        Rol rol = new Rol();
        rol.setIdRol(1L);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setRol(rol);
        usuario.setPasswordHash("hash-anterior");
        return usuario;
    }

    private static class UsuarioDaoGrabador implements UsuarioDao {
        private Usuario usuarioActualizado;

        @Override
        public Optional<Usuario> buscarPorCorreo(String correo) {
            return Optional.empty();
        }

        @Override
        public Optional<Usuario> buscarPorId(Long id) {
            return Optional.empty();
        }

        @Override
        public List<Usuario> listarTodos() {
            return List.of();
        }

        @Override
        public List<Rol> listarRolesActivos() {
            return List.of();
        }

        @Override
        public int guardar(Usuario usuario) {
            return 1;
        }

        @Override
        public int actualizar(Usuario usuario) {
            usuarioActualizado = usuario;
            return 1;
        }

        @Override
        public int desactivar(Long id) {
            return 1;
        }
    }
}
