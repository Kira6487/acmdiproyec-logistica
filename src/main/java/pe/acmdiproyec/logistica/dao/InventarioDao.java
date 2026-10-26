package pe.acmdiproyec.logistica.dao;

import java.util.List;

import pe.acmdiproyec.logistica.model.Inventario;

/**
 * Consulta de existencias actuales (tabla {@code public.inventario}). Es de
 * solo lectura: el stock lo mantienen los disparadores de la base de datos.
 */
public interface InventarioDao {

    List<Inventario> listarTodo();
}
