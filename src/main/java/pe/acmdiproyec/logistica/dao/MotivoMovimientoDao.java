package pe.acmdiproyec.logistica.dao;

import java.util.List;
import java.util.Optional;

import pe.acmdiproyec.logistica.model.MotivoMovimiento;

/**
 * Acceso a datos de los motivos de movimiento (tabla
 * {@code public.motivos_movimiento}). Solo declara operaciones de
 * persistencia; las reglas de negocio corresponden a la capa de servicio.
 */
public interface MotivoMovimientoDao {

    List<MotivoMovimiento> listarTodos();

    List<MotivoMovimiento> listarPorTipo(String tipo);

    Optional<MotivoMovimiento> buscarPorId(Long idMotivoMovimiento);

    int guardar(MotivoMovimiento motivoMovimiento);

    int actualizar(MotivoMovimiento motivoMovimiento);

    int eliminar(Long idMotivoMovimiento);

    int contarPorCodigo(String codigo, Long idMotivoMovimientoExcluido);

    /**
     * La restriccion {@code uq_motivos_movimiento_nombre_tipo} exige que la
     * combinacion nombre + tipo sea unica.
     */
    int contarPorNombreYTipo(String nombre, String tipo, Long idMotivoMovimientoExcluido);

    /** Movimientos que utilizan este motivo. */
    long contarMovimientosAsociados(Long idMotivoMovimiento);
}
