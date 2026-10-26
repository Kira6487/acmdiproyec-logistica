package pe.acmdiproyec.logistica.dao;

import java.util.List;
import java.util.Optional;

import pe.acmdiproyec.logistica.model.MotivoMovimiento;

/**
 * Acceso a datos de los motivos de movimiento (tabla
 * {@code public.motivos_movimiento}).
 */
public interface MotivoMovimientoDao {

    List<MotivoMovimiento> listarTodos();

    List<MotivoMovimiento> listarPorTipo(String tipo);

    Optional<MotivoMovimiento> buscarPorId(Long idMotivoMovimiento);
}
