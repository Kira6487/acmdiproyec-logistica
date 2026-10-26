package pe.acmdiproyec.logistica.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import pe.acmdiproyec.logistica.dao.CategoriaDao;
import pe.acmdiproyec.logistica.dto.CategoriaFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.Categoria;

/**
 * Pruebas de las reglas de negocio de categorias, con un doble de
 * {@link CategoriaDao}. No requieren base de datos.
 */
class CategoriaServiceTest {

    private static final Long ID_EXISTENTE = 7L;
    private static final Long ID_INEXISTENTE = 99L;

    private CategoriaDao categoriaDao;
    private CategoriaService categoriaService;

    @BeforeEach
    void configurar() {
        categoriaDao = mock(CategoriaDao.class);
        categoriaService = new CategoriaService(categoriaDao);
    }

    // ------------------------------------------------------------ listado

    @Test
    void listarTodasDebeDevolverLosRegistrosDelDao() {
        when(categoriaDao.listarTodas()).thenReturn(List.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));

        List<Categoria> resultado = categoriaService.listarTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("DADOS");
    }

    // ------------------------------------------------------------- alta

    @Test
    void crearDebePersistirUnaCategoriaValida() {
        when(categoriaDao.guardar(any(Categoria.class))).thenReturn(1);

        categoriaService.crear(formulario("DADOS", "Dados de concreto", "Descripcion"));

        verify(categoriaDao).guardar(any(Categoria.class));
    }

    @Test
    void crearDebeRecortarLosEspaciosYConvertirDescripcionVaciaEnNula() {
        when(categoriaDao.guardar(any(Categoria.class))).thenReturn(1);

        categoriaService.crear(formulario("  DADOS  ", "  Dados  ", "   "));

        verify(categoriaDao).guardar(argCategoria("DADOS", "Dados", null));
    }

    @Test
    void crearDebeRechazarUnCodigoDuplicado() {
        when(categoriaDao.contarPorCodigo(eq("DADOS"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> categoriaService.crear(formulario("DADOS", "Otro", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("codigo");
        verify(categoriaDao, never()).guardar(any(Categoria.class));
    }

    @Test
    void crearDebeRechazarUnNombreDuplicado() {
        when(categoriaDao.contarPorNombre(eq("Dados"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> categoriaService.crear(formulario("OTRO", "Dados", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombre");
        verify(categoriaDao, never()).guardar(any(Categoria.class));
    }

    @Test
    void crearDebeTraducirUnErrorDeRestriccionDelDao() {
        when(categoriaDao.guardar(any(Categoria.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> categoriaService.crear(formulario("DADOS", "Dados", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void crearDebeFallarCuandoElDaoNoInserta() {
        when(categoriaDao.guardar(any(Categoria.class))).thenReturn(0);

        assertThatThrownBy(() -> categoriaService.crear(formulario("DADOS", "Dados", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("registrar");
    }

    // --------------------------------------------------------- edicion

    @Test
    void actualizarDebePersistirLosCambios() {
        when(categoriaDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));
        when(categoriaDao.actualizar(any(Categoria.class))).thenReturn(1);

        categoriaService.actualizar(ID_EXISTENTE, formulario("DADOS-2", "Dados 2", "Nueva"));

        verify(categoriaDao).actualizar(argCategoria(ID_EXISTENTE, "DADOS-2", "Dados 2", "Nueva"));
    }

    @Test
    void actualizarDebeExcluirElRegistroActualAlValidarDuplicados() {
        when(categoriaDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));
        when(categoriaDao.contarPorCodigo(eq("DADOS"), eq(ID_EXISTENTE))).thenReturn(0);
        when(categoriaDao.contarPorNombre(eq("Dados"), eq(ID_EXISTENTE))).thenReturn(0);
        when(categoriaDao.actualizar(any(Categoria.class))).thenReturn(1);

        categoriaService.actualizar(ID_EXISTENTE, formulario("DADOS", "Dados", null));

        verify(categoriaDao).contarPorCodigo("DADOS", ID_EXISTENTE);
        verify(categoriaDao).contarPorNombre("Dados", ID_EXISTENTE);
    }

    @Test
    void actualizarDebeRechazarUnDuplicado() {
        when(categoriaDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));
        when(categoriaDao.contarPorNombre(eq("Repetido"), eq(ID_EXISTENTE))).thenReturn(1);

        assertThatThrownBy(() -> categoriaService.actualizar(ID_EXISTENTE, formulario("DADOS", "Repetido", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombre");
        verify(categoriaDao, never()).actualizar(any(Categoria.class));
    }

    @Test
    void actualizarDebeFallarCuandoLaCategoriaNoExiste() {
        assertThatThrownBy(() -> categoriaService.actualizar(ID_INEXISTENTE, formulario("DADOS", "Dados", null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(categoriaDao, never()).actualizar(any(Categoria.class));
    }

    @Test
    void obtenerFormularioDebeFallarCuandoLaCategoriaNoExiste() {
        assertThatThrownBy(() -> categoriaService.obtenerFormulario(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void obtenerFormularioDebeDevolverLosDatosDelRegistro() {
        when(categoriaDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));

        CategoriaFormDto formulario = categoriaService.obtenerFormulario(ID_EXISTENTE);

        assertThat(formulario.getIdCategoria()).isEqualTo(ID_EXISTENTE);
        assertThat(formulario.getCodigo()).isEqualTo("DADOS");
        assertThat(formulario.getNombre()).isEqualTo("Dados");
    }

    // -------------------------------------------------------- eliminacion

    @Test
    void eliminarDebeQuitarLaCategoriaSinProductos() {
        when(categoriaDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));
        when(categoriaDao.contarProductosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(categoriaDao.eliminar(ID_EXISTENTE)).thenReturn(1);

        categoriaService.eliminar(ID_EXISTENTE);

        verify(categoriaDao).eliminar(ID_EXISTENTE);
    }

    @Test
    void eliminarDebeBloquearLaCategoriaConProductosAsociados() {
        when(categoriaDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));
        when(categoriaDao.contarProductosAsociados(ID_EXISTENTE)).thenReturn(3L);

        assertThatThrownBy(() -> categoriaService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("productos asociados");
        verify(categoriaDao, never()).eliminar(anyLong());
    }

    @Test
    void eliminarDebeTraducirUnaViolacionDeClaveForanea() {
        when(categoriaDao.buscarPorId(ID_EXISTENTE)).thenReturn(Optional.of(categoria(ID_EXISTENTE, "DADOS", "Dados")));
        when(categoriaDao.contarProductosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(categoriaDao.eliminar(ID_EXISTENTE))
                .thenThrow(new DataIntegrityViolationException("fk violation"));

        assertThatThrownBy(() -> categoriaService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("productos asociados");
    }

    @Test
    void eliminarDebeFallarCuandoLaCategoriaNoExiste() {
        assertThatThrownBy(() -> categoriaService.eliminar(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(categoriaDao, never()).eliminar(anyLong());
    }

    // ----------------------------------------------------------- auxiliares

    private Categoria categoria(Long id, String codigo, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(id);
        categoria.setCodigo(codigo);
        categoria.setNombre(nombre);
        categoria.setActivo(true);
        return categoria;
    }

    private CategoriaFormDto formulario(String codigo, String nombre, String descripcion) {
        CategoriaFormDto formulario = new CategoriaFormDto();
        formulario.setCodigo(codigo);
        formulario.setNombre(nombre);
        formulario.setDescripcion(descripcion);
        formulario.setActivo(true);
        return formulario;
    }

    private Categoria argCategoria(String codigo, String nombre, String descripcion) {
        return argCategoria(null, codigo, nombre, descripcion);
    }

    private Categoria argCategoria(Long id, String codigo, String nombre, String descripcion) {
        return org.mockito.ArgumentMatchers.argThat(argument ->
                java.util.Objects.equals(argument.getIdCategoria(), id)
                        && java.util.Objects.equals(argument.getCodigo(), codigo)
                        && java.util.Objects.equals(argument.getNombre(), nombre)
                        && java.util.Objects.equals(argument.getDescripcion(), descripcion));
    }
}
