package pe.acmdiproyec.logistica.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.model.Usuario;

@Service
public class AutenticacionService {

    private final UsuarioDao usuarioDao;

    public AutenticacionService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public Usuario buscarUsuarioActivo(String correo) {
        return usuarioDao.buscarPorCorreo(correo)
                .filter(usuario -> usuario.isActivo())
                .filter(usuario -> usuario.getRol() != null)
                .filter(usuario -> usuario.getRol().isActivo())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado o inactivo"
                        ));
    }
}