package pe.acmdiproyec.logistica.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import pe.acmdiproyec.logistica.dao.CategoriaDao;
import pe.acmdiproyec.logistica.dao.ProductoDao;
import pe.acmdiproyec.logistica.dao.UnidadMedidaDao;
import pe.acmdiproyec.logistica.dto.ProductoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.Categoria;
import pe.acmdiproyec.logistica.model.Producto;
import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Pruebas de las reglas de negocio de productos, con dobles de los DAO. No
 * requieren base de datos.
 */
class ProductoServiceTest {

    private static final Long ID_EXISTENTE = 3L;
    private static final Long ID_INEXISTENTE = 99L;
    private static final Long ID_CATEGORIA = 11L;
    private static final Long ID_UNIDAD = 22L;

    private ProductoDao productoDao;
    private CategoriaDao categoriaDao;
    private UnidadMedidaDao unidadMedidaDao;
    private ProductoService productoService;

    @BeforeEach
    void configurar() {
        productoDao = mock(ProductoDao.class);
        categoriaDao = mock(CategoriaDao.class);
        unidadMedidaDao = mock(UnidadMedidaDao.class);
        productoService = new ProductoService(productoDao, categoriaDao, unidadMedidaDao);

        when(categoriaDao.buscarPorId(ID_CATEGORIA)).thenReturn(Optional.of(categoria()));
        when(unidadMedidaDao.buscarPorId(ID_UNIDAD)).thenReturn(Optional.of(unidad()));
    }

    // ------------------------------------------------------------ listado

    @Test
    void listarTodosDebeDevolverLosRegistrosDelDao() {
        when(productoDao.listarTodos()).thenReturn(List.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));

        List<Producto> resultado = productoService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("DADO-2CM");
    }

    @Test
    void listarTodosDebeIncluirLaCategoriaYLaUnidadDelProducto() {
        when(productoDao.listarTodos()).thenReturn(List.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));

        Producto resultado = productoService.listarTodos().get(0);

        assertThat(resultado.getCategoria().getNombre()).isEqualTo("Dados");
        assertThat(resultado.getUnidadMedida().getAbreviatura()).isEqualTo("und");
    }

    // ------------------------------------------------------------- alta

    @Test
    void crearDebePersistirUnProductoValido() {
        when(productoDao.guardar(any(Producto.class))).thenReturn(1);

        productoService.crear(formulario("DADO-2CM", "Dado 2 cm"));

        verify(productoDao).guardar(argProducto(null, "DADO-2CM", "Dado 2 cm", null));
    }

    @Test
    void crearDebeRecortarLosEspaciosYConvertirDescripcionVaciaEnNula() {
        when(productoDao.guardar(any(Producto.class))).thenReturn(1);

        ProductoFormDto formulario = formulario("  DADO-2CM  ", "  Dado 2 cm  ");
        formulario.setDescripcion("   ");
        productoService.crear(formulario);

        verify(productoDao).guardar(argProducto(null, "DADO-2CM", "Dado 2 cm", null));
    }

    @Test
    void crearDebeRechazarUnCodigoDuplicado() {
        when(productoDao.contarPorCodigo(eq("DADO-2CM"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> productoService.crear(formulario("DADO-2CM", "Otro")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("codigo");
        verify(productoDao, never()).guardar(any(Producto.class));
    }

    @Test
    void crearDebeRechazarUnNombreDuplicado() {
        when(productoDao.contarPorNombre(eq("Dado 2 cm"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> productoService.crear(formulario("OTRO", "Dado 2 cm")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void crearDebeRechazarUnaCategoriaInexistente() {
        when(categoriaDao.buscarPorId(ID_CATEGORIA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crear(formulario("DADO-2CM", "Dado 2 cm")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("categoria");
        verify(productoDao, never()).guardar(any(Producto.class));
    }

    @Test
    void crearDebeRechazarUnaUnidadDeMedidaInexistente() {
        when(unidadMedidaDao.buscarPorId(ID_UNIDAD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crear(formulario("DADO-2CM", "Dado 2 cm")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("unidad de medida");
        verify(productoDao, never()).guardar(any(Producto.class));
    }

    @Test
    void crearDebeTraducirUnErrorDeRestriccionDelDao() {
        when(productoDao.guardar(any(Producto.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> productoService.crear(formulario("DADO-2CM", "Dado 2 cm")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void crearDebeFallarCuandoElDaoNoInserta() {
        when(productoDao.guardar(any(Producto.class))).thenReturn(0);

        assertThatThrownBy(() -> productoService.crear(formulario("DADO-2CM", "Dado 2 cm")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("registrar");
    }

    // --------------------------------------------------------- edicion

    @Test
    void actualizarDebePersistirLosCambios() {
        when(productoDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));
        when(productoDao.actualizar(any(Producto.class))).thenReturn(1);

        productoService.actualizar(ID_EXISTENTE, formulario("DADO-3CM", "Dado 3 cm"));

        verify(productoDao).actualizar(argProducto(ID_EXISTENTE, "DADO-3CM", "Dado 3 cm", null));
    }

    @Test
    void actualizarDebeExcluirElProductoActualAlValidarDuplicados() {
        when(productoDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));
        when(productoDao.actualizar(any(Producto.class))).thenReturn(1);

        productoService.actualizar(ID_EXISTENTE, formulario("DADO-2CM", "Dado 2 cm"));

        verify(productoDao).contarPorCodigo("DADO-2CM", ID_EXISTENTE);
        verify(productoDao).contarPorNombre("Dado 2 cm", ID_EXISTENTE);
    }

    @Test
    void actualizarDebeFallarCuandoElProductoNoExiste() {
        assertThatThrownBy(() -> productoService.actualizar(ID_INEXISTENTE, formulario("DADO-2CM", "Dado 2 cm")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(productoDao, never()).actualizar(any(Producto.class));
    }

    @Test
    void obtenerFormularioDebeFallarCuandoElProductoNoExiste() {
        assertThatThrownBy(() -> productoService.obtenerFormulario(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void obtenerFormularioDebeDevolverLosDatosDelRegistro() {
        when(productoDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));

        ProductoFormDto formulario = productoService.obtenerFormulario(ID_EXISTENTE);

        assertThat(formulario.getIdProducto()).isEqualTo(ID_EXISTENTE);
        assertThat(formulario.getIdCategoria()).isEqualTo(ID_CATEGORIA);
        assertThat(formulario.getIdUnidadMedida()).isEqualTo(ID_UNIDAD);
        assertThat(formulario.getStockMinimo()).isEqualByComparingTo("20.000");
    }

    // -------------------------------------------------------- eliminacion

    @Test
    void eliminarDebeQuitarUnProductoSinMovimientosNiExistencias() {
        when(productoDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));
        when(productoDao.contarMovimientosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(productoDao.obtenerStockActual(ID_EXISTENTE)).thenReturn(BigDecimal.ZERO);
        when(productoDao.eliminar(ID_EXISTENTE)).thenReturn(1);

        productoService.eliminar(ID_EXISTENTE);

        verify(productoDao).eliminar(ID_EXISTENTE);
    }

    @Test
    void eliminarDebeBloquearUnProductoConMovimientos() {
        when(productoDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));
        when(productoDao.contarMovimientosAsociados(ID_EXISTENTE)).thenReturn(2L);

        assertThatThrownBy(() -> productoService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("movimientos registrados");
        verify(productoDao, never()).eliminar(anyLong());
    }

    @Test
    void eliminarDebeBloquearUnProductoConExistencias() {
        when(productoDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));
        when(productoDao.contarMovimientosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(productoDao.obtenerStockActual(ID_EXISTENTE)).thenReturn(new BigDecimal("12.500"));

        assertThatThrownBy(() -> productoService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("existencias disponibles");
        verify(productoDao, never()).eliminar(anyLong());
    }

    @Test
    void eliminarDebeTraducirLaViolacionDeClaveForaneaDelInventario() {
        when(productoDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(producto(ID_EXISTENTE, "DADO-2CM", "Dado 2 cm")));
        when(productoDao.contarMovimientosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(productoDao.obtenerStockActual(ID_EXISTENTE)).thenReturn(BigDecimal.ZERO);
        when(productoDao.eliminar(ID_EXISTENTE))
                .thenThrow(new DataIntegrityViolationException("fk inventario"));

        assertThatThrownBy(() -> productoService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("registro de inventario");
    }

    @Test
    void eliminarDebeFallarCuandoElProductoNoExiste() {
        assertThatThrownBy(() -> productoService.eliminar(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(productoDao, never()).eliminar(anyLong());
    }

    // ----------------------------------------------------------- auxiliares

    private Categoria categoria() {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(ID_CATEGORIA);
        categoria.setCodigo("DADOS");
        categoria.setNombre("Dados");
        categoria.setActivo(true);
        return categoria;
    }

    private UnidadMedida unidad() {
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setIdUnidadMedida(ID_UNIDAD);
        unidadMedida.setCodigo("UNIDAD");
        unidadMedida.setNombre("Unidad");
        unidadMedida.setAbreviatura("und");
        unidadMedida.setActivo(true);
        return unidadMedida;
    }

    private Producto producto(Long id, String codigo, String nombre) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setCategoria(categoria());
        producto.setUnidadMedida(unidad());
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setStockMinimo(new BigDecimal("20.000"));
        producto.setActivo(true);
        return producto;
    }

    private ProductoFormDto formulario(String codigo, String nombre) {
        ProductoFormDto formulario = new ProductoFormDto();
        formulario.setIdCategoria(ID_CATEGORIA);
        formulario.setIdUnidadMedida(ID_UNIDAD);
        formulario.setCodigo(codigo);
        formulario.setNombre(nombre);
        formulario.setStockMinimo(new BigDecimal("20.000"));
        formulario.setActivo(true);
        return formulario;
    }

    private Producto argProducto(Long id, String codigo, String nombre, String descripcion) {
        return argThat(argument ->
                Objects.equals(argument.getIdProducto(), id)
                        && Objects.equals(argument.getCodigo(), codigo)
                        && Objects.equals(argument.getNombre(), nombre)
                        && Objects.equals(argument.getDescripcion(), descripcion));
    }
}
