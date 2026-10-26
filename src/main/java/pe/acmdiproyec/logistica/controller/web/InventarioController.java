package pe.acmdiproyec.logistica.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.acmdiproyec.logistica.service.InventarioService;

/**
 * Consulta de existencias actuales del almacen. Es de solo lectura.
 */
@Controller
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/inventario")
    public String listar(Model model) {
        model.addAttribute("existencias", inventarioService.listarTodo());
        return "inventario/lista";
    }
}
