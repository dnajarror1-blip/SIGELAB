package sigelab.controller;

import sigelab.model.TipoEquipo;
import sigelab.service.TipoEquipoService;

import java.util.List;
import java.util.Optional;

public class TipoEquipoController {

    private final TipoEquipoService service;

    public TipoEquipoController() {
        this.service = new TipoEquipoService();
    }

    // Registra un nuevo tipo de equipo.
    public TipoEquipo crear(String nombre, String descripcion) {
        return service.crear(nombre, descripcion);
    }

    // Devuelve todos los tipos registrados.
    public List<TipoEquipo> listar() {
        return service.listar();
    }

    // Busca un tipo de equipo por ID.
    public Optional<TipoEquipo> buscarPorId(int idTipoEquipo) {
        return service.buscarPorId(idTipoEquipo);
    }

    // Actualiza un tipo de equipo.
    public void actualizar(
            int idTipoEquipo,
            String nombre,
            String descripcion
    ) {
        service.actualizar(
                idTipoEquipo,
                nombre,
                descripcion
        );
    }

    // Elimina un tipo de equipo.
    public void eliminar(int idTipoEquipo) {
        service.eliminar(idTipoEquipo);
    }
}
