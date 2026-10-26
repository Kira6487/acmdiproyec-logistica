package pe.acmdiproyec.logistica.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import pe.acmdiproyec.logistica.dao.MotivoMovimientoDao;
import pe.acmdiproyec.logistica.dao.MovimientoDao;
import pe.acmdiproyec.logistica.dao.ProductoDao;
import pe.acmdiproyec.logistica.dao.UsuarioDao;
import pe.acmdiproyec.logistica.dto.MovimientoDetalleFormDto;
import pe.acmdiproyec.logistica.dto.MovimientoFormDto;
import pe.acmdiproyec.logistica.exception.RecursoNoEncontradoException;
import pe.acmdiproyec.logistica.exception.ReglaNegocioException;
import pe.acmdiproyec.logistica.model.MotivoMovimiento;
import pe.acmdiproyec.logistica.model.Movimiento;
import pe.acmdiproyec.logistica.model.MovimientoDetalle;
import pe.acmdiproyec.logistica.model.Producto;
import pe.acmdiproyec.logistica.model.Usuario;

/**
 * Pruebas de las reglas de negocio de los movimientos, con dobles de los DAO.
 *
 * Estas pruebas NO validan el comportamiento de los disparadores de
 * PostgreSQL ni la actualizacion real del stock: eso requiere una prueba de
 * integracion contra la base de datos.
 */
class MovimientoServiceTest {

    private static final String CORREO = "operador@acmdiproyec.test";
    private static final Long ID_MOTIVO = 5L;
    private static final Long ID_PRODUCTO = 7L;

    private MovimientoDao movimientoDao;
    private MotivoMovimientoDao motivoMovimientoDao;
    private ProductoDao productoDao;
    private UsuarioDao usuarioDao;
    private MovimientoService movimientoService;

    @BeforeEach
    void configurar() {
        movimientoDao = mock(MovimientoDao.class);
        motivoMovimientoDao = mock(MotivoMovimientoDao.class);
        productoDao = mock(ProductoDao.class);
        usuarioDao = mock(UsuarioDao.class);
        movimientoService = new MovimientoService(
                movimientoDao, motivoMovimientoDao, productoDao, usuarioDao);

        when(motivoMovimientoDao.buscarPorId(ID_MOTIVO))
                .thenReturn(Optional.of(motivo("ENTRADA")));
        when(usuarioDao.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(productoDao.buscarPorId(ID_PRODUCTO)).thenReturn(Optional.of(producto()));
    }

    // ------------------------------------------------------- registro valido

    @Test
    void registrarDebePersistirLaCabeceraYSusDetalles() {
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(42L);
        when(movimientoDao.guardarDetalle(any(MovimientoDetalle.class))).thenReturn(1);

        Long id = movimientoService.registrar(formulario(ID_PRODUCTO, "10.000"), CORREO);

        assertThat(id).isEqualTo(42L);
        verify(movimientoDao).guardar(any(Movimiento.class));
        verify(movimientoDao).guardarDetalle(any(MovimientoDetalle.class));
    }

    @Test
    void registrarDebeDerivarElTipoDelMotivoYNoDelFormulario() {
        when(motivoMovimientoDao.buscarPorId(ID_MOTIVO)).thenReturn(Optional.of(motivo("SALIDA")));
        when(productoDao.obtenerStockActual(ID_PRODUCTO)).thenReturn(new BigDecimal("50.000"));
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(1L);
        when(movimientoDao.guardarDetalle(any(MovimientoDetalle.class))).thenReturn(1);

        movimientoService.registrar(formulario(ID_PRODUCTO, "5.000"), CORREO);

        verify(movimientoDao).guardar(argMovimiento("SALIDA", "VIGENTE"));
    }

    @Test
    void registrarDebeAsignarElUsuarioAutenticado() {
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(1L);
        when(movimientoDao.guardarDetalle(any(MovimientoDetalle.class))).thenReturn(1);

        movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO);

        verify(movimientoDao).guardar(argMovimientoPorUsuario(CORREO));
    }

    @Test
    void registrarDebeGenerarUnCodigoDeMovimiento() {
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(1L);
        when(movimientoDao.guardarDetalle(any(MovimientoDetalle.class))).thenReturn(1);

        movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO);

        verify(movimientoDao).guardar(argMovimientoConCodigo());
    }

    // --------------------------------------------------- validacion de datos

    @Test
    void registrarDebeRechazarUnMotivoInexistente() {
        when(motivoMovimientoDao.buscarPorId(ID_MOTIVO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("motivo");
        verify(movimientoDao, never()).guardar(any());
    }

    @Test
    void registrarDebeRechazarUnMotivoInactivo() {
        MotivoMovimiento motivo = motivo("ENTRADA");
        motivo.setActivo(false);
        when(motivoMovimientoDao.buscarPorId(ID_MOTIVO)).thenReturn(Optional.of(motivo));

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void registrarDebeRechazarUnUsuarioInexistente() {
        when(usuarioDao.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("usuario");
        verify(movimientoDao, never()).guardar(any());
    }

    @Test
    void registrarDebeRechazarUnMovimientoSinLineas() {
        MovimientoFormDto formulario = new MovimientoFormDto();
        formulario.setIdMotivoMovimiento(ID_MOTIVO);

        assertThatThrownBy(() -> movimientoService.registrar(formulario, CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos un producto");
    }

    @Test
    void registrarDebeRechazarCantidadesNulasCeroONegativas() {
        for (String cantidad : new String[] { null, "0", "-1", "0.000" }) {
            MovimientoFormDto formulario = new MovimientoFormDto();
            formulario.setIdMotivoMovimiento(ID_MOTIVO);
            MovimientoDetalleFormDto linea = new MovimientoDetalleFormDto();
            linea.setIdProducto(ID_PRODUCTO);
            linea.setCantidad(cantidad == null ? null : new BigDecimal(cantidad));
            formulario.getLineas().add(linea);

            assertThatThrownBy(() -> movimientoService.registrar(formulario, CORREO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("mayor que cero");
        }
        verify(movimientoDao, never()).guardar(any());
    }

    @Test
    void registrarDebeRechazarUnProductoInexistente() {
        when(productoDao.buscarPorId(ID_PRODUCTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void registrarDebeRechazarProductosRepetidos() {
        MovimientoFormDto formulario = new MovimientoFormDto();
        formulario.setIdMotivoMovimiento(ID_MOTIVO);
        formulario.getLineas().add(linea(ID_PRODUCTO, "1.000"));
        formulario.getLineas().add(linea(ID_PRODUCTO, "2.000"));

        assertThatThrownBy(() -> movimientoService.registrar(formulario, CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("repetir");
    }

    // ------------------------------------------------------- reglas de stock

    @Test
    void registrarDebeRechazarUnaSalidaQueSuperaElStock() {
        when(motivoMovimientoDao.buscarPorId(ID_MOTIVO)).thenReturn(Optional.of(motivo("SALIDA")));
        when(productoDao.obtenerStockActual(ID_PRODUCTO)).thenReturn(new BigDecimal("3.000"));

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "5.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(movimientoDao, never()).guardar(any());
    }

    @Test
    void registrarDebePermitirUnaEntradaSinStockPrevio() {
        when(productoDao.obtenerStockActual(ID_PRODUCTO)).thenReturn(BigDecimal.ZERO);
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(1L);
        when(movimientoDao.guardarDetalle(any(MovimientoDetalle.class))).thenReturn(1);

        movimientoService.registrar(formulario(ID_PRODUCTO, "5.000"), CORREO);

        verify(movimientoDao).guardar(any(Movimiento.class));
    }

    @Test
    void registrarDebePermitirUnaSalidaQueCabeEnElStock() {
        when(motivoMovimientoDao.buscarPorId(ID_MOTIVO)).thenReturn(Optional.of(motivo("SALIDA")));
        when(productoDao.obtenerStockActual(ID_PRODUCTO)).thenReturn(new BigDecimal("5.000"));
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(1L);
        when(movimientoDao.guardarDetalle(any(MovimientoDetalle.class))).thenReturn(1);

        movimientoService.registrar(formulario(ID_PRODUCTO, "5.000"), CORREO);

        verify(movimientoDao).guardarDetalle(any(MovimientoDetalle.class));
    }

    // ------------------------------------------------- fallos de persistencia

    @Test
    void registrarDebeTraducirUnFalloAlInsertarLaCabecera() {
        when(movimientoDao.guardar(any(Movimiento.class)))
                .thenThrow(new DataIntegrityViolationException("fk"));

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se pudo registrar");
    }

    @Test
    void registrarDebeTraducirUnFalloAlInsertarUnDetalle() {
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(1L);
        when(movimientoDao.guardarDetalle(any(MovimientoDetalle.class)))
                .thenThrow(new DataIntegrityViolationException("stock"));

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("stock disponible");
    }

    @Test
    void registrarDebeFallarSiLaCabeceraNoGeneraIdentificador() {
        when(movimientoDao.guardar(any(Movimiento.class))).thenReturn(null);

        assertThatThrownBy(() -> movimientoService.registrar(formulario(ID_PRODUCTO, "1.000"), CORREO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    /**
     * El registro debe ejecutar cabecera y detalles en una sola transaccion;
     * si una linea falla, no debe quedar registrado ningun cambio.
     */
    @Test
    void registrarDebeSerTransaccional() throws Exception {
        Method metodo = MovimientoService.class
                .getDeclaredMethod("registrar", MovimientoFormDto.class, String.class);

        assertThat(metodo.isAnnotationPresent(Transactional.class))
                .as("El registro de movimientos debe ser transaccional")
                .isTrue();
        assertThat(RuntimeException.class.isAssignableFrom(ReglaNegocioException.class))
                .as("Las excepciones de negocio deben provocar la reapertura de la transaccion")
                .isTrue();
    }

    // ------------------------------------------------------- consultas

    @Test
    void listarMovimientosDebeAplicarLosFiltros() {
        when(movimientoDao.listarTodos(any(), any(), any(), any(), any()))
                .thenReturn(List.of(new Movimiento()));

        List<Movimiento> resultado =
                movimientoService.listarMovimientos(ID_PRODUCTO, "ENTRADA", "VIGENTE", "2026-01-01", "2026-01-31");

        assertThat(resultado).hasSize(1);
        verify(movimientoDao).listarTodos(
                eq(ID_PRODUCTO), eq("ENTRADA"), eq("VIGENTE"), any(), any());
    }

    @Test
    void listarMovimientosDebeRechazarUnaFechaConFormatoInvalido() {
        assertThatThrownBy(() ->
                movimientoService.listarMovimientos(null, null, null, "31/01/2026", null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("formato");
    }

    @Test
    void listarMotivosPorTipoDebeRechazarTiposInvalidos() {
        assertThatThrownBy(() -> movimientoService.listarMotivosPorTipo("AJUSTE"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tipo");
    }

    @Test
    void obtenerMovimientoDebeDevolverCabeceraYDetalles() {
        Movimiento movimiento = new Movimiento();
        movimiento.setIdMovimiento(9L);
        when(movimientoDao.buscarPorId(9L)).thenReturn(Optional.of(movimiento));
        when(movimientoDao.listarDetalles(9L)).thenReturn(List.of(new MovimientoDetalle()));

        Movimiento resultado = movimientoService.obtenerMovimiento(9L);

        assertThat(resultado.getDetalles()).hasSize(1);
    }

    @Test
    void obtenerMovimientoDebeFallarSiNoExiste() {
        assertThatThrownBy(() -> movimientoService.obtenerMovimiento(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ----------------------------------------------------------- auxiliares

    private MotivoMovimiento motivo(String tipo) {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setIdMotivoMovimiento(ID_MOTIVO);
        motivo.setCodigo("MOTIVO");
        motivo.setNombre("Motivo");
        motivo.setTipo(tipo);
        motivo.setActivo(true);
        return motivo;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(3L);
        usuario.setCorreo(CORREO);
        usuario.setNombres("Nombre");
        usuario.setApellidos("Apellido");
        usuario.setActivo(true);
        return usuario;
    }

    private Producto producto() {
        Producto producto = new Producto();
        producto.setIdProducto(ID_PRODUCTO);
        producto.setCodigo("DADO-2CM");
        producto.setNombre("Dado 2 cm");
        producto.setActivo(true);
        return producto;
    }

    private MovimientoDetalleFormDto linea(Long idProducto, String cantidad) {
        MovimientoDetalleFormDto linea = new MovimientoDetalleFormDto();
        linea.setIdProducto(idProducto);
        linea.setCantidad(new BigDecimal(cantidad));
        return linea;
    }

    private MovimientoFormDto formulario(Long idProducto, String cantidad) {
        MovimientoFormDto formulario = new MovimientoFormDto();
        formulario.setIdMotivoMovimiento(ID_MOTIVO);
        formulario.getLineas().add(linea(idProducto, cantidad));
        return formulario;
    }

    private Movimiento argMovimiento(String tipo, String estado) {
        return org.mockito.ArgumentMatchers.argThat(argument ->
                tipo.equals(argument.getTipo()) && estado.equals(argument.getEstado()));
    }

    private Movimiento argMovimientoPorUsuario(String correo) {
        return org.mockito.ArgumentMatchers.argThat(argument ->
                argument.getUsuario() != null
                        && correo.equals(argument.getUsuario().getCorreo())
                        && argument.getUsuario().getIdUsuario() != null);
    }

    private Movimiento argMovimientoConCodigo() {
        return org.mockito.ArgumentMatchers.argThat(argument ->
                argument.getCodigo() != null && argument.getCodigo().startsWith("MOV-"));
    }
}
