package pe.acmdiproyec.logistica.dao;

import java.util.List;
import java.util.Optional;

import pe.acmdiproyec.logistica.model.Categoria;

/**
 * Acceso a datos de las categorias de productos (tabla
 * {@code public.categorias}). Solo declara operaciones de persistencia: las
 * reglas de negocio corresponden a la capa de servicio.
 */
public interface CategoriaDao {

    List<Categoria> listarTodas();

    Optional<Categoria> buscarPorId(Long idCategoria);

    int guardar(Categoria categoria);

    int actualizar(Categoria categoria);

    int eliminar(Long idCategoria);

    int contarPorCodigo(String codigo, Long idCategoriaExcluida);

    int contarPorNombre(String nombre, Long idCategoriaExcluida);

    long contarProductosAsociados(Long idCategoria);
}
