package pe.acmdiproyec.logistica.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Pruebas de las reglas de validacion de los formularios de datos maestros.
 * Son la unica fuente de verdad de las validaciones de campos obligatorios y
 * de longitud, por lo que no se repiten en el controlador ni en el servicio.
 */
class ValidacionFormulariosTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    // -------------------------------------------------------- categorias

    @Test
    void categoriaValidaNoTieneViolaciones() {
        CategoriaFormDto formulario = new CategoriaFormDto();
        formulario.setCodigo("DADOS");
        formulario.setNombre("Dados de concreto");
        formulario.setDescripcion("Descripcion valida");

        assertThat(validador.validate(formulario)).isEmpty();
    }

    @Test
    void categoriaRechazaCamposObligatoriosVacios() {
        CategoriaFormDto formulario = new CategoriaFormDto();
        formulario.setCodigo("   ");
        formulario.setNombre(null);
        formulario.setDescripcion("   ");

        Set<ConstraintViolation<CategoriaFormDto>> violaciones = validador.validate(formulario);

        assertThat(camposConError(violaciones))
                .containsExactlyInAnyOrder("codigo", "nombre");
    }

    @Test
    void categoriaRechazaCodigosMayoresDe30Caracteres() {
        CategoriaFormDto formulario = new CategoriaFormDto();
        formulario.setCodigo("X".repeat(31));
        formulario.setNombre("Nombre");

        assertThat(camposConError(validador.validate(formulario))).contains("codigo");
    }

    @Test
    void categoriaRechazaNombresMayoresDe100Caracteres() {
        CategoriaFormDto formulario = new CategoriaFormDto();
        formulario.setCodigo("CODIGO");
        formulario.setNombre("X".repeat(101));

        assertThat(camposConError(validador.validate(formulario))).contains("nombre");
    }

    @Test
    void categoriaRechazaDescripcionesMayoresDe250Caracteres() {
        CategoriaFormDto formulario = new CategoriaFormDto();
        formulario.setCodigo("CODIGO");
        formulario.setNombre("Nombre");
        formulario.setDescripcion("X".repeat(251));

        assertThat(camposConError(validador.validate(formulario))).contains("descripcion");
    }

    // ------------------------------------------------- unidades de medida

    @Test
    void unidadMedidaValidaNoTieneViolaciones() {
        UnidadMedidaFormDto formulario = new UnidadMedidaFormDto();
        formulario.setCodigo("UNIDAD");
        formulario.setNombre("Unidad");
        formulario.setAbreviatura("und");

        assertThat(validador.validate(formulario)).isEmpty();
    }

    @Test
    void unidadMedidaRechazaCamposObligatoriosVacios() {
        UnidadMedidaFormDto formulario = new UnidadMedidaFormDto();
        formulario.setCodigo("");
        formulario.setNombre(" ");
        formulario.setAbreviatura(null);

        Set<ConstraintViolation<UnidadMedidaFormDto>> violaciones = validador.validate(formulario);

        assertThat(camposConError(violaciones))
                .containsExactlyInAnyOrder("codigo", "nombre", "abreviatura");
    }

    @Test
    void unidadMedidaRechazaCodigosYAbreviaturasMayoresDe20Caracteres() {
        UnidadMedidaFormDto formulario = new UnidadMedidaFormDto();
        formulario.setCodigo("X".repeat(21));
        formulario.setNombre("Unidad");
        formulario.setAbreviatura("X".repeat(21));

        assertThat(camposConError(validador.validate(formulario)))
                .containsExactlyInAnyOrder("codigo", "abreviatura");
    }

    @Test
    void unidadMedidaRechazaNombresMayoresDe100Caracteres() {
        UnidadMedidaFormDto formulario = new UnidadMedidaFormDto();
        formulario.setCodigo("CODIGO");
        formulario.setNombre("X".repeat(101));
        formulario.setAbreviatura("ab");

        assertThat(camposConError(validador.validate(formulario))).contains("nombre");
    }

    // ------------------------------------------------------------- productos

    @Test
    void productoValidoNoTieneViolaciones() {
        ProductoFormDto formulario = new ProductoFormDto();
        formulario.setIdCategoria(1L);
        formulario.setIdUnidadMedida(2L);
        formulario.setCodigo("DADO-2CM");
        formulario.setNombre("Dado separador de 2 cm");
        formulario.setDescripcion("Prefabricado de concreto");
        formulario.setStockMinimo(new java.math.BigDecimal("20.000"));

        assertThat(validador.validate(formulario)).isEmpty();
    }

    @Test
    void productoRechazaCamposObligatoriosVacios() {
        ProductoFormDto formulario = new ProductoFormDto();
        formulario.setIdCategoria(null);
        formulario.setIdUnidadMedida(null);
        formulario.setCodigo("   ");
        formulario.setNombre("");
        formulario.setStockMinimo(null);

        Set<ConstraintViolation<ProductoFormDto>> violaciones = validador.validate(formulario);

        assertThat(camposConError(violaciones))
                .containsExactlyInAnyOrder(
                        "idCategoria", "idUnidadMedida", "codigo", "nombre", "stockMinimo");
    }

    @Test
    void productoRechazaCodigosMayoresDe50Caracteres() {
        ProductoFormDto formulario = productoValido();
        formulario.setCodigo("X".repeat(51));

        assertThat(camposConError(validador.validate(formulario))).contains("codigo");
    }

    @Test
    void productoRechazaNombresMayoresDe150Caracteres() {
        ProductoFormDto formulario = productoValido();
        formulario.setNombre("X".repeat(151));

        assertThat(camposConError(validador.validate(formulario))).contains("nombre");
    }

    @Test
    void productoRechazaDescripcionesMayoresDe500Caracteres() {
        ProductoFormDto formulario = productoValido();
        formulario.setDescripcion("X".repeat(501));

        assertThat(camposConError(validador.validate(formulario))).contains("descripcion");
    }

    @Test
    void productoRechazaStockMinimoNegativo() {
        ProductoFormDto formulario = productoValido();
        formulario.setStockMinimo(new java.math.BigDecimal("-0.001"));

        assertThat(camposConError(validador.validate(formulario))).contains("stockMinimo");
    }

    @Test
    void productoRechazaStockMinimoConDemasiadosDecimales() {
        ProductoFormDto formulario = productoValido();
        formulario.setStockMinimo(new java.math.BigDecimal("1.2345"));

        assertThat(camposConError(validador.validate(formulario))).contains("stockMinimo");
    }

    private ProductoFormDto productoValido() {
        ProductoFormDto formulario = new ProductoFormDto();
        formulario.setIdCategoria(1L);
        formulario.setIdUnidadMedida(2L);
        formulario.setCodigo("DADO-2CM");
        formulario.setNombre("Dado separador de 2 cm");
        formulario.setStockMinimo(new java.math.BigDecimal("20.000"));
        return formulario;
    }

    private <T> Set<String> camposConError(Set<ConstraintViolation<T>> violaciones) {
        return violaciones.stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
