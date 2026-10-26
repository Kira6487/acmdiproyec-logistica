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
import pe.acmdiproyec.logistica.dto.CategoriaFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.service.CategoriaService;

/**
 * Mantenimiento de categorias de productos. El controlador solo recibe
 * solicitudes, valida el formulario y delega en {@link CategoriaService}; no
 * contiene SQL.
 */
@Controller
public class CategoriaController {

    private static final String VISTA_LISTA = "maestros/categorias/lista";
    private static final String VISTA_FORMULARIO = "maestros/categorias/formulario";

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/categorias")
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        return VISTA_LISTA;
    }

    @GetMapping("/categorias/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new CategoriaFormDto(), "Nueva categoria");
        return VISTA_FORMULARIO;
    }

    @PostMapping("/categorias")
    public String crear(
            @Valid @ModelAttribute("categoria") CategoriaFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            model.addAttribute("tituloFormulario", "Nueva categoria");
            return VISTA_FORMULARIO;
        }
        try {
            categoriaService.crear(formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorCategoria", excepcion.getMessage());
            model.addAttribute("tituloFormulario", "Nueva categoria");
            return VISTA_FORMULARIO;
        }
        redirect.addFlashAttribute("mensajeExito", "Categoria registrada correctamente.");
        return "redirect:/categorias";
    }

    @GetMapping("/categorias/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            prepararFormulario(model, categoriaService.obtenerFormulario(id), "Editar categoria");
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/categorias";
        }
        return VISTA_FORMULARIO;
    }

    @PostMapping("/categorias/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoria") CategoriaFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            formulario.setIdCategoria(id);
            model.addAttribute("tituloFormulario", "Editar categoria");
            return VISTA_FORMULARIO;
        }
        try {
            categoriaService.actualizar(id, formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorCategoria", excepcion.getMessage());
            formulario.setIdCategoria(id);
            model.addAttribute("tituloFormulario", "Editar categoria");
            return VISTA_FORMULARIO;
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/categorias";
        }
        redirect.addFlashAttribute("mensajeExito", "Categoria actualizada correctamente.");
        return "redirect:/categorias";
    }

    @PostMapping("/categorias/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            categoriaService.eliminar(id);
            redirect.addFlashAttribute("mensajeExito", "Categoria eliminada correctamente.");
        } catch (ReglaNegocioException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        }
        return "redirect:/categorias";
    }

    private void prepararFormulario(Model model, CategoriaFormDto formulario, String titulo) {
        model.addAttribute("categoria", formulario);
        model.addAttribute("tituloFormulario", titulo);
    }
}
