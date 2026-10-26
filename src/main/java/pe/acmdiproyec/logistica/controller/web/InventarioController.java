package pe.acmdiproyec.logistica.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.acmdiproyec.logistica.service.CategoriaService;
import pe.acmdiproyec.logistica.service.InventarioService;

/**
 * Consulta de existencias actuales del almacen, con filtros por nombre o
 * codigo, categoria y estado de stock. Es de solo lectura.
 */
@Controller
public class InventarioController {

    private final InventarioService inventarioService;
    private final CategoriaService categoriaService;

    public InventarioController(
            InventarioService inventarioService,
            CategoriaService categoriaService) {
        this.inventarioService = inventarioService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/inventario")
    public String listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) String estado,
            Model model) {

        model.addAttribute("existencias", inventarioService.listarFiltrado(q, idCategoria, estado));
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("filtroQ", q);
        model.addAttribute("filtroIdCategoria", idCategoria);
        model.addAttribute("filtroEstado", estado);
        return "inventario/lista";
    }
}
