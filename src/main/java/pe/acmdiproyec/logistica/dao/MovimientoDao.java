package pe.acmdiproyec.logistica.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import pe.acmdiproyec.logistica.model.Movimiento;
import pe.acmdiproyec.logistica.model.MovimientoDetalle;

/**
 * Acceso a datos de los movimientos de inventario (tablas
 * {@code public.movimientos} y {@code public.movimiento_detalle}).
 *
 * Solo se inserta la cabecera y sus detalles: el stock lo actualiza el
 * disparador {@code trg_movimiento_detalle_aplicar_inventario}. Por eso no
 * existe ninguna operacion de escritura sobre {@code inventario}.
 */
public interface MovimientoDao {

    /**
     * Historial de movimientos con filtros opcionales. Los parametros nulos no
     * restringen la busqueda.
     */
    List<Movimiento> listarTodos(
            Long idProducto,
            String tipo,
            String estado,
            OffsetDateTime desde,
            OffsetDateTime hasta);

    Optional<Movimiento> buscarPorId(Long idMovimiento);

    List<MovimientoDetalle> listarDetalles(Long idMovimiento);

    /**
     * Inserta la cabecera y devuelve el identificador generado, necesario para
     * insertar a continuacion sus lineas de detalle.
     */
    Long guardar(Movimiento movimiento);

    int guardarDetalle(MovimientoDetalle detalle);
}
