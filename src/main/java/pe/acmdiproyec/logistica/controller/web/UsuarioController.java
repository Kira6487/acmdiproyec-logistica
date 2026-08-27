package pe.acmdiproyec.logistica.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import pe.acmdiproyec.logistica.dto.UsuarioFormDto;
import pe.acmdiproyec.logistica.service.UsuarioService;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        return "usuarios/lista";
    }

    @GetMapping("/usuarios/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new UsuarioFormDto());
        model.addAttribute("roles", usuarioService.listarRolesActivos());
        return "usuarios/formulario";
    }

    @PostMapping("/usuarios")
    public String crear(UsuarioFormDto formulario) {
        usuarioService.crearDesdeFormulario(formulario);
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.buscarUsuario(id));
        return "usuarios/detalle";
    }

    @GetMapping("/usuarios/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.obtenerFormulario(id));
        model.addAttribute("roles", usuarioService.listarRolesActivos());
        return "usuarios/formulario";
    }

    @PostMapping("/usuarios/{id}")
    public String actualizar(@PathVariable Long id, UsuarioFormDto formulario) {
        usuarioService.actualizarDesdeFormulario(id, formulario);
        return "redirect:/usuarios/" + id;
    }

    @PostMapping("/usuarios/{id}/desactivar")
    public String desactivar(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return "redirect:/usuarios";
    }
}
