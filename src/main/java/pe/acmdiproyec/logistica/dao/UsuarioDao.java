package pe.acmdiproyec.logistica.dao;
import java.util.List;
import java.util.Optional;

import pe.acmdiproyec.logistica.model.Rol;
import pe.acmdiproyec.logistica.model.Usuario;

public interface UsuarioDao {

    Optional<Usuario> buscarPorCorreo(String correo);

    Optional<Usuario> buscarPorId(Long id);

    List<Usuario> listarTodos();

    List<Rol> listarRolesActivos();

    int guardar(Usuario usuario);

    int actualizar(Usuario usuario);

    int desactivar(Long id);
}