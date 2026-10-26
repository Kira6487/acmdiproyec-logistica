package pe.acmdiproyec.logistica.dao;

import java.util.List;
import java.util.Optional;

import pe.acmdiproyec.logistica.model.UnidadMedida;

/**
 * Acceso a datos de las unidades de medida (tabla
 * {@code public.unidades_medida}). Solo declara operaciones de persistencia:
 * las reglas de negocio corresponden a la capa de servicio.
 */
public interface UnidadMedidaDao {

    List<UnidadMedida> listarTodas();

    Optional<UnidadMedida> buscarPorId(Long idUnidadMedida);

    int guardar(UnidadMedida unidadMedida);

    int actualizar(UnidadMedida unidadMedida);

    int eliminar(Long idUnidadMedida);

    int contarPorCodigo(String codigo, Long idUnidadMedidaExcluida);

    int contarPorNombre(String nombre, Long idUnidadMedidaExcluida);

    int contarPorAbreviatura(String abreviatura, Long idUnidadMedidaExcluida);

    long contarProductosAsociados(Long idUnidadMedida);
}
