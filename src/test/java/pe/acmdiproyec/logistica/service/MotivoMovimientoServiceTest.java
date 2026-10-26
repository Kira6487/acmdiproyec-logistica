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

import pe.acmdiproyec.logistica.dao.MotivoMovimientoDao;
import pe.acmdiproyec.logistica.dto.MotivoMovimientoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.MotivoMovimiento;

/**
 * Pruebas de las reglas de negocio de los motivos de movimiento, con un doble
 * de {@link MotivoMovimientoDao}. No requieren base de datos.
 */
class MotivoMovimientoServiceTest {

    private static final Long ID_EXISTENTE = 4L;
    private static final Long ID_INEXISTENTE = 99L;

    private MotivoMovimientoDao motivoMovimientoDao;
    private MotivoMovimientoService motivoMovimientoService;

    @BeforeEach
    void configurar() {
        motivoMovimientoDao = mock(MotivoMovimientoDao.class);
        motivoMovimientoService = new MotivoMovimientoService(motivoMovimientoDao);
    }

    @Test
    void listarTodosDebeDevolverLosRegistrosDelDao() {
        when(motivoMovimientoDao.listarTodos()).thenReturn(List.of(motivo(ID_EXISTENTE, "COMPRA", "Compra", "ENTRADA")));

        List<MotivoMovimiento> resultado = motivoMovimientoService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTipo()).isEqualTo("ENTRADA");
    }

    @Test
    void listarPorTipoDebeRechazarTiposQueNoEstanEnElEnum() {
        assertThatThrownBy(() -> motivoMovimientoService.listarPorTipo("AJUSTE"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ENTRADA o SALIDA");
    }

    @Test
    void crearDebePersistirUnMotivoValido() {
        when(motivoMovimientoDao.guardar(any(MotivoMovimiento.class))).thenReturn(1);

        motivoMovimientoService.crear(formulario("COMPRA", "Compra", "ENTRADA", "Ingreso por compra"));

        verify(motivoMovimientoDao).guardar(argMotivo(null, "COMPRA", "Compra", "ENTRADA", "Ingreso por compra"));
    }

    @Test
    void crearDebeRechazarUnTipoInvalido() {
        assertThatThrownBy(() -> motivoMovimientoService.crear(formulario("COMPRA", "Compra", "OTRO", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ENTRADA o SALIDA");
        verify(motivoMovimientoDao, never()).guardar(any());
    }

    @Test
    void crearDebeRechazarUnCodigoDuplicado() {
        when(motivoMovimientoDao.contarPorCodigo(eq("COMPRA"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> motivoMovimientoService.crear(formulario("COMPRA", "Compra", "ENTRADA", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("codigo");
        verify(motivoMovimientoDao, never()).guardar(any());
    }

    @Test
    void crearDebeRechazarUnNombreYTipoDuplicados() {
        when(motivoMovimientoDao.contarPorNombreYTipo(eq("Compra"), eq("ENTRADA"), isNull())).thenReturn(1);

        assertThatThrownBy(() -> motivoMovimientoService.crear(formulario("OTRO", "Compra", "ENTRADA", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombre y tipo");
    }

    @Test
    void crearDebePermitirElMismoNombreEnTiposDistintos() {
        when(motivoMovimientoDao.contarPorNombreYTipo(eq("Ajuste"), eq("SALIDA"), isNull())).thenReturn(0);
        when(motivoMovimientoDao.guardar(any(MotivoMovimiento.class))).thenReturn(1);

        motivoMovimientoService.crear(formulario("AJUSTE_SALIDA", "Ajuste", "SALIDA", null));

        verify(motivoMovimientoDao).guardar(any(MotivoMovimiento.class));
    }

    @Test
    void crearDebeTraducirUnErrorDeRestriccionDelDao() {
        when(motivoMovimientoDao.guardar(any(MotivoMovimiento.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> motivoMovimientoService.crear(formulario("COMPRA", "Compra", "ENTRADA", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void crearDebeFallarCuandoElDaoNoInserta() {
        when(motivoMovimientoDao.guardar(any(MotivoMovimiento.class))).thenReturn(0);

        assertThatThrownBy(() -> motivoMovimientoService.crear(formulario("COMPRA", "Compra", "ENTRADA", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("registrar");
    }

    @Test
    void actualizarDebePersistirLosCambios() {
        when(motivoMovimientoDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(motivo(ID_EXISTENTE, "COMPRA", "Compra", "ENTRADA")));
        when(motivoMovimientoDao.actualizar(any(MotivoMovimiento.class))).thenReturn(1);

        motivoMovimientoService.actualizar(ID_EXISTENTE, formulario("COMPRA_MAYOR", "Compra mayor", "ENTRADA", null));

        verify(motivoMovimientoDao)
                .actualizar(argMotivo(ID_EXISTENTE, "COMPRA_MAYOR", "Compra mayor", "ENTRADA", null));
    }

    @Test
    void actualizarDebeExcluirElRegistroActualAlValidarDuplicados() {
        when(motivoMovimientoDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(motivo(ID_EXISTENTE, "COMPRA", "Compra", "ENTRADA")));
        when(motivoMovimientoDao.actualizar(any(MotivoMovimiento.class))).thenReturn(1);

        motivoMovimientoService.actualizar(ID_EXISTENTE, formulario("COMPRA", "Compra", "ENTRADA", null));

        verify(motivoMovimientoDao).contarPorCodigo("COMPRA", ID_EXISTENTE);
        verify(motivoMovimientoDao).contarPorNombreYTipo("Compra", "ENTRADA", ID_EXISTENTE);
    }

    @Test
    void actualizarDebeFallarCuandoElMotivoNoExiste() {
        assertThatThrownBy(() -> motivoMovimientoService.actualizar(
                ID_INEXISTENTE, formulario("COMPRA", "Compra", "ENTRADA", null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(motivoMovimientoDao, never()).actualizar(any());
    }

    @Test
    void obtenerFormularioDebeFallarCuandoElMotivoNoExiste() {
        assertThatThrownBy(() -> motivoMovimientoService.obtenerFormulario(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void obtenerFormularioDebeDevolverLosDatosDelRegistro() {
        when(motivoMovimientoDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(motivo(ID_EXISTENTE, "COMPRA", "Compra", "ENTRADA")));

        MotivoMovimientoFormDto formulario = motivoMovimientoService.obtenerFormulario(ID_EXISTENTE);

        assertThat(formulario.getIdMotivoMovimiento()).isEqualTo(ID_EXISTENTE);
        assertThat(formulario.getCodigo()).isEqualTo("COMPRA");
        assertThat(formulario.getTipo()).isEqualTo("ENTRADA");
    }

    @Test
    void eliminarDebeQuitarElMotivoSinMovimientos() {
        when(motivoMovimientoDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(motivo(ID_EXISTENTE, "COMPRA", "Compra", "ENTRADA")));
        when(motivoMovimientoDao.contarMovimientosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(motivoMovimientoDao.eliminar(ID_EXISTENTE)).thenReturn(1);

        motivoMovimientoService.eliminar(ID_EXISTENTE);

        verify(motivoMovimientoDao).eliminar(ID_EXISTENTE);
    }

    @Test
    void eliminarDebeBloquearElMotivoUtilizadoEnMovimientos() {
        when(motivoMovimientoDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(motivo(ID_EXISTENTE, "COMPRA", "Compra", "ENTRADA")));
        when(motivoMovimientoDao.contarMovimientosAsociados(ID_EXISTENTE)).thenReturn(2L);

        assertThatThrownBy(() -> motivoMovimientoService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("movimientos registrados");
        verify(motivoMovimientoDao, never()).eliminar(anyLong());
    }

    @Test
    void eliminarDebeTraducirUnaViolacionDeClaveForanea() {
        when(motivoMovimientoDao.buscarPorId(ID_EXISTENTE))
                .thenReturn(Optional.of(motivo(ID_EXISTENTE, "COMPRA", "Compra", "ENTRADA")));
        when(motivoMovimientoDao.contarMovimientosAsociados(ID_EXISTENTE)).thenReturn(0L);
        when(motivoMovimientoDao.eliminar(ID_EXISTENTE))
                .thenThrow(new DataIntegrityViolationException("fk violation"));

        assertThatThrownBy(() -> motivoMovimientoService.eliminar(ID_EXISTENTE))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("movimientos registrados");
    }

    @Test
    void eliminarDebeFallarCuandoElMotivoNoExiste() {
        assertThatThrownBy(() -> motivoMovimientoService.eliminar(ID_INEXISTENTE))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(motivoMovimientoDao, never()).eliminar(anyLong());
    }

    // ----------------------------------------------------------- auxiliares

    private MotivoMovimiento motivo(Long id, String codigo, String nombre, String tipo) {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setIdMotivoMovimiento(id);
        motivo.setCodigo(codigo);
        motivo.setNombre(nombre);
        motivo.setTipo(tipo);
        motivo.setActivo(true);
        return motivo;
    }

    private MotivoMovimientoFormDto formulario(
            String codigo, String nombre, String tipo, String descripcion) {
        MotivoMovimientoFormDto formulario = new MotivoMovimientoFormDto();
        formulario.setCodigo(codigo);
        formulario.setNombre(nombre);
        formulario.setTipo(tipo);
        formulario.setDescripcion(descripcion);
        formulario.setActivo(true);
        return formulario;
    }

    private MotivoMovimiento argMotivo(
            Long id, String codigo, String nombre, String tipo, String descripcion) {
        return argThat(argument ->
                Objects.equals(argument.getIdMotivoMovimiento(), id)
                        && Objects.equals(argument.getCodigo(), codigo)
                        && Objects.equals(argument.getNombre(), nombre)
                        && Objects.equals(argument.getTipo(), tipo)
                        && Objects.equals(argument.getDescripcion(), descripcion));
    }
}
