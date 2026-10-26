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
import pe.acmdiproyec.logistica.dto.MotivoMovimientoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.MotivoMovimiento;
import pe.acmdiproyec.logistica.service.MotivoMovimientoService;

/**
 * Mantenimiento de motivos de movimiento, uno de los datos maestros del
 * sistema. El controlador solo recibe solicitudes, valida el formulario y
 * delega en {@link MotivoMovimientoService}; no contiene SQL.
 */
@Controller
public class MotivoMovimientoController {

    private static final String VISTA_LISTA = "maestros/motivos/lista";
    private static final String VISTA_FORMULARIO = "maestros/motivos/formulario";

    private final MotivoMovimientoService motivoMovimientoService;

    public MotivoMovimientoController(MotivoMovimientoService motivoMovimientoService) {
        this.motivoMovimientoService = motivoMovimientoService;
    }

    @GetMapping("/motivos")
    public String listar(Model model) {
        model.addAttribute("motivos", motivoMovimientoService.listarTodos());
        return VISTA_LISTA;
    }

    @GetMapping("/motivos/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new MotivoMovimientoFormDto(), "Nuevo motivo de movimiento");
        return VISTA_FORMULARIO;
    }

    @PostMapping("/motivos")
    public String crear(
            @Valid @ModelAttribute("motivo") MotivoMovimientoFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            model.addAttribute("tituloFormulario", "Nuevo motivo de movimiento");
            return VISTA_FORMULARIO;
        }
        try {
            motivoMovimientoService.crear(formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorMotivo", excepcion.getMessage());
            model.addAttribute("tituloFormulario", "Nuevo motivo de movimiento");
            return VISTA_FORMULARIO;
        }
        redirect.addFlashAttribute("mensajeExito", "Motivo de movimiento registrado correctamente.");
        return "redirect:/motivos";
    }

    @GetMapping("/motivos/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            prepararFormulario(
                    model,
                    motivoMovimientoService.obtenerFormulario(id),
                    "Editar motivo de movimiento");
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/motivos";
        }
        return VISTA_FORMULARIO;
    }

    @PostMapping("/motivos/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("motivo") MotivoMovimientoFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            formulario.setIdMotivoMovimiento(id);
            model.addAttribute("tituloFormulario", "Editar motivo de movimiento");
            return VISTA_FORMULARIO;
        }
        try {
            motivoMovimientoService.actualizar(id, formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorMotivo", excepcion.getMessage());
            formulario.setIdMotivoMovimiento(id);
            model.addAttribute("tituloFormulario", "Editar motivo de movimiento");
            return VISTA_FORMULARIO;
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/motivos";
        }
        redirect.addFlashAttribute("mensajeExito", "Motivo de movimiento actualizado correctamente.");
        return "redirect:/motivos";
    }

    @PostMapping("/motivos/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            motivoMovimientoService.eliminar(id);
            redirect.addFlashAttribute("mensajeExito", "Motivo de movimiento eliminado correctamente.");
        } catch (ReglaNegocioException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        }
        return "redirect:/motivos";
    }

    private void prepararFormulario(Model model, MotivoMovimientoFormDto formulario, String titulo) {
        model.addAttribute("motivo", formulario);
        model.addAttribute("tituloFormulario", titulo);
        model.addAttribute("tipos", java.util.List.of(
                MotivoMovimiento.TIPO_ENTRADA, MotivoMovimiento.TIPO_SALIDA));
    }
}
