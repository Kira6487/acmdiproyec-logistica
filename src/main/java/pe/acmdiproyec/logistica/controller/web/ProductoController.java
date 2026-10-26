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
import pe.acmdiproyec.logistica.dto.ProductoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.service.CategoriaService;
import pe.acmdiproyec.logistica.service.ProductoService;
import pe.acmdiproyec.logistica.service.UnidadMedidaService;

/**
 * Mantenimiento de productos. El controlador solo recibe solicitudes, valida el
 * formulario y delega en {@link ProductoService}; no contiene SQL.
 */
@Controller
public class ProductoController {

    private static final String VISTA_LISTA = "maestros/productos/lista";
    private static final String VISTA_FORMULARIO = "maestros/productos/formulario";

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final UnidadMedidaService unidadMedidaService;

    public ProductoController(
            ProductoService productoService,
            CategoriaService categoriaService,
            UnidadMedidaService unidadMedidaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.unidadMedidaService = unidadMedidaService;
    }

    @GetMapping("/productos")
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return VISTA_LISTA;
    }

    @GetMapping("/productos/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new ProductoFormDto(), "Nuevo producto");
        return VISTA_FORMULARIO;
    }

    @PostMapping("/productos")
    public String crear(
            @Valid @ModelAttribute("producto") ProductoFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            model.addAttribute("tituloFormulario", "Nuevo producto");
            return VISTA_FORMULARIO;
        }
        try {
            productoService.crear(formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorProducto", excepcion.getMessage());
            model.addAttribute("tituloFormulario", "Nuevo producto");
            return VISTA_FORMULARIO;
        }
        redirect.addFlashAttribute("mensajeExito", "Producto registrado correctamente.");
        return "redirect:/productos";
    }

    @GetMapping("/productos/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            prepararFormulario(model, productoService.obtenerFormulario(id), "Editar producto");
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/productos";
        }
        return VISTA_FORMULARIO;
    }

    @PostMapping("/productos/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("producto") ProductoFormDto formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {

        if (resultado.hasErrors()) {
            formulario.setIdProducto(id);
            model.addAttribute("tituloFormulario", "Editar producto");
            return VISTA_FORMULARIO;
        }
        try {
            productoService.actualizar(id, formulario);
        } catch (ReglaNegocioException excepcion) {
            resultado.reject("errorProducto", excepcion.getMessage());
            formulario.setIdProducto(id);
            model.addAttribute("tituloFormulario", "Editar producto");
            return VISTA_FORMULARIO;
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
            return "redirect:/productos";
        }
        redirect.addFlashAttribute("mensajeExito", "Producto actualizado correctamente.");
        return "redirect:/productos";
    }

    @PostMapping("/productos/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            productoService.eliminar(id);
            redirect.addFlashAttribute("mensajeExito", "Producto eliminado correctamente.");
        } catch (ReglaNegocioException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        } catch (RecursoNoEncontradoException excepcion) {
            redirect.addFlashAttribute("mensajeError", excepcion.getMessage());
        }
        return "redirect:/productos";
    }

    private void prepararFormulario(Model model, ProductoFormDto formulario, String titulo) {
        model.addAttribute("producto", formulario);
        model.addAttribute("tituloFormulario", titulo);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("unidadesMedida", unidadMedidaService.listarTodas());
    }
}
