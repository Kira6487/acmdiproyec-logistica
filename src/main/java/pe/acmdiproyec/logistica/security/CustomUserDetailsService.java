package pe.acmdiproyec.logistica.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.model.Usuario;
import pe.acmdiproyec.logistica.service.AutenticacionService;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AutenticacionService autenticacionService;

    public CustomUserDetailsService(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = autenticacionService.buscarUsuarioActivo(correo);
        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().getCodigo())
                .disabled(!usuario.isActivo())
                .build();
    }
}
