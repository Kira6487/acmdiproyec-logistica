package pe.acmdiproyec.logistica.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.acmdiproyec.logistica.service.PanelService;

/**
 * Panel logistico autenticado. El controlador solo recupera los indicadores
 * calculados por {@link PanelService}; no contiene logica de negocio.
 */
@Controller
public class HomeController {

    private final PanelService panelService;

    public HomeController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/panel")
    public String inicio(Model model) {
        model.addAttribute("panel", panelService.obtenerPanel());
        return "auth/index";
    }
}
