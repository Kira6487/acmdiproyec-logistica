package pe.acmdiproyec.logistica.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.acmdiproyec.logistica.dto.UnidadMedidaFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.service.UnidadMedidaService;

/**
 * Mantenimiento de unidades de medida. El controlador solo recibe solicitudes,
 * valida el formulario y delega en {@link UnidadMedidaService}; no contiene
 * SQL.
 */
@Controller
public class UnidadMedidaController {

    private static final String VISTA_LISTA = "maestros/unidades-medida/lista";
    private static final String VISTA_FORMULARIO = "maestros/unidades-medida/formulario";

    private final UnidadMedidaService unidadMedidaService;

    public UnidadMedidaController(UnidadMedidaService unidadMedidaService) {
        this.unidadMedidaService = unidadMedidaService;
    }

    @GetMapping("/unidades-medida")
    public String listar(Model model) {
        model.addAttribute("unidadesMedida", unidadMedidaService.listarTodas());
        return VISTA_LISTA;
    }

    @GetMapping("/unidades-medida/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new UnidadMedidaFormDto(), "Nueva unidad de medida");
        return VISTA_FORMULARIO;
    }

    @PostMapping("/unidades-medida")
    public String crear(
            @Valid @ModelAttribute("unidadMedida") UnidadMedidaFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            model.addAttribute("tituloFormulario", "Nueva unidad de medida");
            return VISTA_FORMULARIO;
        }
        try {
            unidadMedidaService.crear(formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorUnidadMedida", excepcion.getMessage());
            model.addAttribute("tituloFormulario", "Nueva unidad de medida");
            return VISTA_FORMULARIO;
        }
        redirect.addFlashAttribute("mensajeExito", "Unidad de medida registrada correctamente.");
        return "redirect:/unidades-medida";
    }

    @GetMapping("/unidades-medida/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            prepararFormulario(
                    model, unidadMedidaService.obtenerFormulario(id), "Editar unidad de medida");
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/unidades-medida";
        }
        return VISTA_FORMULARIO;
    }

    @PostMapping("/unidades-medida/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("unidadMedida") UnidadMedidaFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            formulario.setIdUnidadMedida(id);
            model.addAttribute("tituloFormulario", "Editar unidad de medida");
            return VISTA_FORMULARIO;
        }
        try {
            unidadMedidaService.actualizar(id, formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorUnidadMedida", excepcion.getMessage());
            formulario.setIdUnidadMedida(id);
            model.addAttribute("tituloFormulario", "Editar unidad de medida");
            return VISTA_FORMULARIO;
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/unidades-medida";
        }
        redirect.addFlashAttribute("mensajeExito", "Unidad de medida actualizada correctamente.");
        return "redirect:/unidades-medida";
    }

    @PostMapping("/unidades-medida/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            unidadMedidaService.eliminar(id);
            redirect.addFlashAttribute("mensajeExito", "Unidad de medida eliminada correctamente.");
        } catch (ReglaNegocioException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        }
        return "redirect:/unidades-medida";
    }

    private void prepararFormulario(Model model, UnidadMedidaFormDto formulario, String titulo) {
        model.addAttribute("unidadMedida", formulario);
        model.addAttribute("tituloFormulario", titulo);
    }
}
