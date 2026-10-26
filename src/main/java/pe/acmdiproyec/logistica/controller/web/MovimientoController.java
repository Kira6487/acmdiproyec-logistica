package pe.acmdiproyec.logistica.controller.web;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.acmdiproyec.logistica.dto.AnulacionMovimientoFormDto;
import pe.acmdiproyec.logistica.dto.MovimientoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.service.InventarioService;
import pe.acmdiproyec.logistica.service.MovimientoService;

/**
 * Movimientos de inventario: historial, detalle y registro de entradas y
 * salidas. El controlador solo recibe solicitudes, valida el formulario y
 * delega en {@link MovimientoService}; no contiene SQL.
 */
@Controller
public class MovimientoController {

    private static final String VISTA_LISTA = "movimientos/lista";
    private static final String VISTA_DETALLE = "movimientos/detalle";
    private static final String VISTA_FORMULARIO = "movimientos/formulario";

    private final MovimientoService movimientoService;
    private final InventarioService inventarioService;

    public MovimientoController(
            MovimientoService movimientoService,
            InventarioService inventarioService) {
        this.movimientoService = movimientoService;
        this.inventarioService = inventarioService;
    }

    @GetMapping("/movimientos")
    public String listar(
            @RequestParam(required = false) Long idProducto,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            Model model) {

        model.addAttribute("movimientos",
                movimientoService.listarMovimientos(idProducto, tipo, estado, desde, hasta));
        model.addAttribute("productos", inventarioService.listarTodo());
        model.addAttribute("tipos", List.of(MovimientoService.TIPO_ENTRADA, MovimientoService.TIPO_SALIDA));
        model.addAttribute("estados", List.of("VIGENTE", "ANULADO"));
        model.addAttribute("filtroIdProducto", idProducto);
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroDesde", desde);
        model.addAttribute("filtroHasta", hasta);
        return VISTA_LISTA;
    }

    @GetMapping("/movimientos/nuevo")
    public String nuevo(@RequestParam String tipo, Model model, RedirectAttributes redirect) {
        try {
            prepararFormulario(model, tipo);
        } catch (ReglaNegocioException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/movimientos";
        }
        return VISTA_FORMULARIO;
    }

    @PostMapping("/movimientos")
    public String crear(
            @RequestParam String tipo,
            @Valid @ModelAttribute("movimiento") MovimientoFormDto formulario,
            BindingResult resultado,
            Model model,
            Principal principal,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            try {
                prepararFormulario(model, tipo);
            } catch (ReglaNegocioException excepcion) {
                redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
                return "redirect:/movimientos";
            }
            return VISTA_FORMULARIO;
        }
        try {
            Long idMovimiento = movimientoService.registrar(formulario, principal.getName());
            redirect.addFlashAttribute("mensajeExito",
                    "Movimiento registrado correctamente.");
            return "redirect:/movimientos/" + idMovimiento;
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorMovimiento", excepcion.getMessage());
            try {
                prepararFormulario(model, tipo);
            } catch (ReglaNegocioException errorFormulario) {
                redirect.addFlashAttribute("mensajeError", errorFormulario.getMessage());
                return "redirect:/movimientos";
            }
            return VISTA_FORMULARIO;
        }
    }

    @GetMapping("/movimientos/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("movimiento", movimientoService.obtenerMovimiento(id));
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/movimientos";
        }
        model.addAttribute("anulacion", new AnulacionMovimientoFormDto());
        return VISTA_DETALLE;
    }

    /**
     * Anula un movimiento vigente. El usuario responsable se toma de la sesion
     * autenticada: no se acepta ningun identificador enviado desde el navegador.
     */
    @PostMapping("/movimientos/{id}/anular")
    public String anular(
            @PathVariable Long id,
            @Valid @ModelAttribute("anulacion") AnulacionMovimientoFormDto formulario,
            BindingResult resultado,
            Model model,
            Principal principal,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            try {
                model.addAttribute("movimiento", movimientoService.obtenerMovimiento(id));
            } catch (RecursoNoEncontradoException excepcion) {
                redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
                return "redirect:/movimientos";
            }
            return VISTA_DETALLE;
        }
        try {
            movimientoService.anular(id, formulario.getMotivoAnulacion(), principal.getName());
            redirect.addFlashAttribute("mensajeExito", "Movimiento anulado correctamente.");
        } catch (ReglaNegocioException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/movimientos";
        }
        return "redirect:/movimientos/" + id;
    }

    private void prepararFormulario(Model model, String tipo) {
        MovimientoFormDto formulario = (MovimientoFormDto) model.asMap().get("movimiento");
        if (formulario == null) {
            formulario = new MovimientoFormDto();
            formulario.getLineas().add(new pe.acmdiproyec.logistica.dto.MovimientoDetalleFormDto());
        }
        model.addAttribute("movimiento", formulario);
        model.addAttribute("tipo", tipo);
        model.addAttribute("motivos", movimientoService.listarMotivosPorTipo(tipo));
        model.addAttribute("productos", inventarioService.listarTodo());
        model.addAttribute("tituloFormulario",
                MovimientoService.TIPO_ENTRADA.equals(tipo) ? "Registrar entrada" : "Registrar salida");
    }
}
