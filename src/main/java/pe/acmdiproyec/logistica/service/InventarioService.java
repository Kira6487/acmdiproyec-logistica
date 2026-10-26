package pe.acmdiproyec.logistica.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pe.acmdiproyec.logistica.dao.InventarioDao;
import pe.acmdiproyec.logistica.model.Inventario;

/**
 * Consulta de existencias actuales. Es de solo lectura: el stock lo mantienen
 * los disparadores de la base de datos y esta capa nunca lo calcula.
 */
@Service
public class InventarioService {

    private final InventarioDao inventarioDao;

    public InventarioService(InventarioDao inventarioDao) {
        this.inventarioDao = inventarioDao;
    }

    public List<Inventario> listarTodo() {
        return inventarioDao.listarTodo();
    }
}
