package pe.acmdiproyec.logistica.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
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
 * Reglas de negocio de los movimientos de inventario.
 *
 * El registro se hace en UNA transaccion que inserta primero la cabecera y
 * despues sus lineas de detalle. El stock NO se calcula ni se escribe aqui: el
 * disparador {@code trg_movimiento_detalle_aplicar_inventario} bloquea la fila
 * de inventario, valida el saldo disponible y actualiza el stock. Confiar esa
 * tarea a la base de datos evita que dos salidas simultaneas dejen existencias
 * negativas.
 *
 * El tipo del movimiento (ENTRADA o SALIDA) se deriva del motivo elegido y no
 * se toma del navegador, de modo que la clave foranea compuesta
 * {@code (id_motivo_movimiento, tipo)} siempre es coherente.
 */
@Service
public class MovimientoService {

    /** Unicos tipos que admite el enum {@code tipo_movimiento}. */
    public static final String TIPO_ENTRADA = MotivoMovimiento.TIPO_ENTRADA;
    public static final String TIPO_SALIDA = MotivoMovimiento.TIPO_SALIDA;

    /** Unicos estados que admite el enum {@code estado_movimiento}. */
    public static final String ESTADO_VIGENTE = "VIGENTE";
    public static final String ESTADO_ANULADO = "ANULADO";

    private static final DateTimeFormatter FORMATO_CODIGO =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MovimientoDao movimientoDao;
    private final MotivoMovimientoDao motivoMovimientoDao;
    private final ProductoDao productoDao;
    private final UsuarioDao usuarioDao;

    public MovimientoService(
            MovimientoDao movimientoDao,
            MotivoMovimientoDao motivoMovimientoDao,
            ProductoDao productoDao,
            UsuarioDao usuarioDao) {
        this.movimientoDao = movimientoDao;
        this.motivoMovimientoDao = motivoMovimientoDao;
        this.productoDao = productoDao;
        this.usuarioDao = usuarioDao;
    }

    public List<Movimiento> listarMovimientos(
            Long idProducto, String tipo, String estado, String desde, String hasta) {
        return movimientoDao.listarTodos(
                idProducto,
                tipo,
                estado,
                convertirFecha(desde, "desde"),
                convertirFecha(hasta, "hasta"));
    }

    /** Movimiento con sus lineas y el stock actual de cada producto. */
    public Movimiento obtenerMovimiento(Long idMovimiento) {
        Movimiento movimiento = buscarMovimiento(idMovimiento);
        movimiento.setDetalles(movimientoDao.listarDetalles(idMovimiento));
        return movimiento;
    }

    public List<MotivoMovimiento> listarMotivosPorTipo(String tipo) {
        validarTipo(tipo);
        return motivoMovimientoDao.listarPorTipo(tipo);
    }

    /**
     * Registra un movimiento de inventario con sus lineas.
     *
     * La operacion completa es una sola transaccion: si una linea es rechazada
     * por la base de datos, no queda registrado ningun cambio.
     */
    @Transactional
    public Long registrar(MovimientoFormDto formulario, String correoUsuario) {
        normalizar(formulario);
        MotivoMovimiento motivo = buscarMotivo(formulario.getIdMotivoMovimiento());
        validarLineas(formulario);
        Usuario usuario = buscarUsuario(correoUsuario);
        validarStockParaSalidas(motivo.getTipo(), formulario);

        Movimiento movimiento = new Movimiento();
        movimiento.setCodigo(generarCodigo());
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setUsuario(usuario);
        movimiento.setTipo(motivo.getTipo());
        movimiento.setEstado("VIGENTE");
        movimiento.setObservacion(formulario.getObservacion());

        Long idMovimiento;
        try {
            idMovimiento = movimientoDao.guardar(movimiento);
        } catch (DataAccessException excepcion) {
            throw new ReglaNegocioException(
                    "No se pudo registrar el movimiento. Intente de nuevo.");
        }
        if (idMovimiento == null) {
            throw new ReglaNegocioException("No se pudo registrar el movimiento.");
        }

        movimiento.setIdMovimiento(idMovimiento);
        for (MovimientoDetalleFormDto linea : formulario.getLineas()) {
            MovimientoDetalle detalle = new MovimientoDetalle();
            detalle.setIdMovimiento(idMovimiento);
            Producto producto = new Producto();
            producto.setIdProducto(linea.getIdProducto());
            detalle.setProducto(producto);
            detalle.setCantidad(linea.getCantidad());
            try {
                movimientoDao.guardarDetalle(detalle);
            } catch (DataAccessException excepcion) {
                throw new ReglaNegocioException(
                        "No se pudo registrar el movimiento. Verifique que las cantidades "
                                + "no superen el stock disponible del producto.");
            }
        }
        return idMovimiento;
    }

    /**
     * Anula un movimiento que esta en estado {@code VIGENTE}.
     *
     * La reversión del stock la realiza el disparador
     * {@code trg_movimientos_revertir_anulacion} al producirse la transicion de
     * estado. Este servicio NO toca {@code inventario} ni vuelve a sumar o
     * restar cantidades: si lo hiciera, el efecto se revertiria dos veces.
     *
     * La actualizacion es condicional sobre {@code estado = 'VIGENTE'}, de modo
     * que dos solicitudes concurrentes no pueden anular el mismo movimiento dos
     * veces; la segunda no modifica ninguna fila y se rechaza con un mensaje
     * claro. Toda la operacion es una sola sentencia atomica dentro de una
     * transaccion.
     */
    @Transactional
    public void anular(Long idMovimiento, String motivoAnulacion, String correoUsuario) {
        String motivo = normalizarMotivoAnulacion(motivoAnulacion);

        Movimiento movimiento = buscarMovimiento(idMovimiento);
        if (ESTADO_ANULADO.equals(movimiento.getEstado())) {
            throw new ReglaNegocioException("El movimiento ya fue anulado.");
        }

        Usuario usuario = buscarUsuario(correoUsuario);

        int filas;
        try {
            filas = movimientoDao.anular(idMovimiento, usuario.getIdUsuario(), motivo);
        } catch (DataAccessException excepcion) {
            throw new ReglaNegocioException(
                    "No se pudo anular el movimiento. Compruebe que la anulacion no deje "
                            + "existencias negativas en el inventario.");
        }
        if (filas != 1) {
            throw new ReglaNegocioException(
                    "El movimiento ya no estaba vigente y no se puede anular dos veces.");
        }
    }

    // ------------------------------------------------------------ validaciones

    private MotivoMovimiento buscarMotivo(Long idMotivoMovimiento) {
        MotivoMovimiento motivo = motivoMovimientoDao.buscarPorId(idMotivoMovimiento)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El motivo indicado no existe. Seleccione un motivo valido."));
        if (!motivo.isActivo()) {
            throw new ReglaNegocioException(
                    "El motivo indicado esta inactivo. Seleccione un motivo activo.");
        }
        return motivo;
    }

    private Usuario buscarUsuario(String correoUsuario) {
        return usuarioDao.buscarPorCorreo(correoUsuario)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No se pudo identificar al usuario responsable del movimiento."));
    }

    private Movimiento buscarMovimiento(Long idMovimiento) {
        return movimientoDao.buscarPorId(idMovimiento)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El movimiento indicado no existe."));
    }

    /**
     * El motivo de la anulacion es obligatorio y su longitud no puede superar la
     * de la columna {@code motivo_anulacion varchar(500)}.
     */
    private String normalizarMotivoAnulacion(String motivoAnulacion) {
        if (motivoAnulacion == null || motivoAnulacion.isBlank()) {
            throw new ReglaNegocioException("El motivo de la anulacion es obligatorio.");
        }
        String motivo = motivoAnulacion.trim();
        if (motivo.length() > 500) {
            throw new ReglaNegocioException(
                    "El motivo de la anulacion no puede superar 500 caracteres.");
        }
        return motivo;
    }

    /**
     * Un movimiento necesita al menos una linea, sin productos repetidos y con
     * cantidades estrictamente positivas.
     */
    private void validarLineas(MovimientoFormDto formulario) {
        if (formulario.getLineas().isEmpty()) {
            throw new ReglaNegocioException(
                    "Debe agregar al menos un producto al movimiento.");
        }
        Set<Long> productos = new HashSet<>();
        for (MovimientoDetalleFormDto linea : formulario.getLineas()) {
            if (linea.getIdProducto() == null) {
                throw new ReglaNegocioException("Debe seleccionar un producto en cada linea.");
            }
            if (!productos.add(linea.getIdProducto())) {
                throw new ReglaNegocioException(
                        "No se puede repetir el mismo producto en un movimiento.");
            }
            if (linea.getCantidad() == null || linea.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
            }
            if (!productoDao.buscarPorId(linea.getIdProducto()).isPresent()) {
                throw new ReglaNegocioException(
                        "Uno de los productos seleccionados no existe.");
            }
        }
    }

    /**
     * Comprobacion anticipada para ofrecer un mensaje claro. La garantia real la
     * da el disparador de la base de datos, que bloquea la fila de inventario y
     * vuelve a validar el saldo dentro de la misma transaccion.
     */
    private void validarStockParaSalidas(String tipo, MovimientoFormDto formulario) {
        if (!TIPO_SALIDA.equals(tipo)) {
            return;
        }
        for (MovimientoDetalleFormDto linea : formulario.getLineas()) {
            BigDecimal stock = productoDao.obtenerStockActual(linea.getIdProducto());
            if (stock.compareTo(linea.getCantidad()) < 0) {
                throw new ReglaNegocioException(
                        "Stock insuficiente para el producto seleccionado. Disponible: "
                                + stock + ", solicitado: " + linea.getCantidad() + ".");
            }
        }
    }

    private void validarTipo(String tipo) {
        if (!TIPO_ENTRADA.equals(tipo) && !TIPO_SALIDA.equals(tipo)) {
            throw new ReglaNegocioException("El tipo de movimiento indicado no es valido.");
        }
    }

    private void normalizar(MovimientoFormDto formulario) {
        if (formulario.getObservacion() != null) {
            String observacion = formulario.getObservacion().trim();
            formulario.setObservacion(observacion.isEmpty() ? null : observacion);
        }
    }

    private java.time.OffsetDateTime convertirFecha(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(valor)
                    .atStartOfDay()
                    .atOffset(java.time.ZoneOffset.UTC);
        } catch (java.time.format.DateTimeParseException excepcion) {
            throw new ReglaNegocioException(
                    "La fecha '" + campo + "' no tiene un formato valido. Use AAAA-MM-DD.");
        }
    }

    /**
     * Codigo unico del movimiento. La unicidad la garantiza la restriccion
     * {@code unique} de la columna {@code codigo}.
     */
    private String generarCodigo() {
        String marca = FORMATO_CODIGO.format(LocalDateTime.now());
        String aleatorio = UUID.randomUUID().toString().substring(0, 8);
        return ("MOV-" + marca + "-" + aleatorio).toUpperCase();
    }
}
