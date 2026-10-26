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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import pe.acmdiproyec.logistica.dao.UnidadMedidaDao;
import pe.acmdiproyec.logistica.dto.UnidadMedidaFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Pruebas de las reglas de negocio de unidades de medida, con un doble de
 * {@link UnidadMedidaDao}. No requieren base de datos.
 */
class UnidadMedidaServiceTest {

    private static final Long ID_EXISTENTE = 5L;
    private static final Long ID_INEXISTENTE = 99L;

    private UnidadMedidaDao unidadMedidaDao;
    private UnidadMedidaService unidadMedidaService;

    @BeforeEach
    void configurar() {
        unidadMedidaDao = mock(UnidadMedidaDao.class);
        unidadMedidaService = new UnidadMedidaService(unidadMedidaDao);
    }

    @Test
    void listarTodasDebeDevolverLosRegistrosDelDao() {
        when(unidadMedidaDao.listarTodas()).thenReturn(List.of(unidad(ID_EXISTENTE, "UNIDAD", "Unidad", "und")));

        List<UnidadMedida> resultado = unidadMedidaService.listarTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getAbreviatura()).isEqualTo("und");
    }

    @Test
    void crearDebePersistirUnaUnidadValida() {
        when(unidadMedidaDao.guardar(any(UnidadMedida.class))).thenReturn(1);

        unidadMedidaService.crear(formulario("UNIDAD", "Unidad", "und"));

        verify(unidadMedidaDao).guardar(argUnidad("UNIDAD", "Unidad", "und"));
    }

    @Test
    void crearDebeRechazarUnCodigoDuplicado() {
        when(unidadMedidaDao.contarPorCodigo(eq("UNIDAD"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> unidadMedidaService.crear(formulario("UNIDAD", "Otra", "otra")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("codigo");
        verify(unidadMedidaDao, never()).guardar(any(UnidadMedida.class));
    }

    @Test
    void crearDebeRechazarUnNombreDuplicado() {
        when(unidadMedidaDao.contarPorNombre(eq("Unidad"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> unidadMedidaService.crear(formulario("OTRO", "Unidad", "otra")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void crearDebeRechazarUnaAbreviaturaDuplicada() {
        when(unidadMedidaDao.contarPorAbreviatura(eq("und"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> unidadMedidaService.crear(formulario("OTRO", "Otra", "und")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("abreviatura");
    }

    @Test
    void crearDebeTraducirUnErrorDeRestriccionDelDao() {
        when(unidadMedidaDao.guardar(any(UnidadMedida.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> unidadMedidaService.crear(formulario("UNIDAD", "Unidad", "und")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void crearDebeFallarCuandoElDaoNoInserta() {
        when(unidadMedidaDao.guardar(any(UnidadMedida.class))).thenReturn(0);

        assertThatThrownBy(() -> unidadMedidaService.crear(formulario("UNIDAD", "Unidad", "und")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("registrar");
    }

    @Test
    void actualizarDebePersistirLosCambios() {
        when(unidadMedidaDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(unidad(ID_EXISTENTE, "UNIDAD", "Unidad", "und")));
        when(unidadMedidaDao.actualizar(any(UnidadMedida.class))).thenReturn(1);

        unidadMedidaService.actualizar(ID_EXISTENTE, formulario("KG", "Kilogramo", "kg"));

        verify(unidadMedidaDao).actualizar(argUnidad(ID_EXISTENTE, "KG", "Kilogramo", "kg"));
    }

    @Test
    void actualizarDebeExcluirElRegistroActualAlValidarDuplicados() {
        when(unidadMedidaDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(unidad(ID_EXISTENTE, "UNIDAD", "Unidad", "und")));
        when(unidadMedidaDao.actualizar(any(UnidadMedida.class))).thenReturn(1);

        unidadMedidaService.actualizar(ID_EXISTENTE, formulario("UNIDAD", "Unidad", "und"));

        verify(unidadMedidaDao).contarPorCodigo("UNIDAD", ID_EXISTENTE);
        verify(unidadMedidaDao).contarPorNombre("Unidad", ID_EXISTENTE);
        verify(unidadMedidaDao).contarPorAbreviatura("und", ID_EXISTENTE);
    }

    @Test
    void actualizarDebeFallarCuandoLaUnidadNoExiste() {
        assertThatThrownBy(() -> unidadMedidaService.actualizar(ID_INEXISTENTE, formulario("KG", "Kilogramo", "kg")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(unidadMedidaDao, never()).actualizar(any(UnidadMedida.class));
    }

    @Test
    void obtenerFormularioDebeFallarCuandoLaUnidadNoExiste() {
        assertThatThrownBy(() -> unidadMedidaService.obtenerFormulario(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void eliminarDebeQuitarLaUnidadSinProductos() {
        when(unidadMedidaDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(unidad(ID_EXISTENTE, "UNIDAD", "Unidad", "und")));
        when(unidadMedidaDao.contarProductosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(unidadMedidaDao.eliminar(ID_EXISTENTE)).thenReturn(1);

        unidadMedidaService.eliminar(ID_EXISTENTE);

        verify(unidadMedidaDao).eliminar(ID_EXISTENTE);
    }

    @Test
    void eliminarDebeBloquearLaUnidadConProductosAsociados() {
        when(unidadMedidaDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(unidad(ID_EXISTENTE, "UNIDAD", "Unidad", "und")));
        when(unidadMedidaDao.contarProductosAsociados(ID_EXISTENTE)).thenReturn(2L);

        assertThatThrownBy(() -> unidadMedidaService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("productos asociados");
        verify(unidadMedidaDao, never()).eliminar(anyLong());
    }

    @Test
    void eliminarDebeTraducirUnaViolacionDeClaveForanea() {
        when(unidadMedidaDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(unidad(ID_EXISTENTE, "UNIDAD", "Unidad", "und")));
        when(unidadMedidaDao.contarProductosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(unidadMedidaDao.eliminar(ID_EXISTENTE))
                .thenThrow(new DataIntegrityViolationException("fk violation"));

        assertThatThrownBy(() -> unidadMedidaService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("productos asociados");
    }

    @Test
    void eliminarDebeFallarCuandoLaUnidadNoExiste() {
        assertThatThrownBy(() -> unidadMedidaService.eliminar(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(unidadMedidaDao, never()).eliminar(anyLong());
    }

    // ----------------------------------------------------------- auxiliares

    private UnidadMedida unidad(Long id, String codigo, String nombre, String abreviatura) {
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setIdUnidadMedida(id);
        unidadMedida.setCodigo(codigo);
        unidadMedida.setNombre(nombre);
        unidadMedida.setAbreviatura(abreviatura);
        unidadMedida.setActivo(true);
        return unidadMedida;
    }

    private UnidadMedidaFormDto formulario(String codigo, String nombre, String abreviatura) {
        UnidadMedidaFormDto formulario = new UnidadMedidaFormDto();
        formulario.setCodigo(codigo);
        formulario.setNombre(nombre);
        formulario.setAbreviatura(abreviatura);
        formulario.setActivo(true);
        return formulario;
    }

    private UnidadMedida argUnidad(String codigo, String nombre, String abreviatura) {
        return argUnidad(null, codigo, nombre, abreviatura);
    }

    private UnidadMedida argUnidad(Long id, String codigo, String nombre, String abreviatura) {
        return argThat(argument ->
                Objects.equals(argument.getIdUnidadMedida(), id)
                        && Objects.equals(argument.getCodigo(), codigo)
                        && Objects.equals(argument.getNombre(), nombre)
                        && Objects.equals(argument.getAbreviatura(), abreviatura));
    }
}
