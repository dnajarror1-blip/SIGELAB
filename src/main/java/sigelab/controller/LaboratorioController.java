package sigelab.controller;

import sigelab.enums.EstadoLaboratorio;
import sigelab.exception.BusinessException;
import sigelab.model.Laboratorio;
import sigelab.service.LaboratorioService;

import java.util.List;
import java.util.Optional;

public class LaboratorioController {

    private final LaboratorioService service;
    private final boolean soloLectura;

    public LaboratorioController() {
        this(false);
    }

    public LaboratorioController(boolean soloLectura) {
        this(soloLectura, new LaboratorioService());
    }

    LaboratorioController(boolean soloLectura, LaboratorioService service) {
        this.soloLectura = soloLectura;
        this.service = service;
    }


    // Registra un laboratorio.
    public Laboratorio crear(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) {

        verificarEscritura();
        return service.crear(
                codigo,
                nombre,
                ubicacion,
                capacidad,
                estado
        );
    }


    // Lista todos los laboratorios.
    public List<Laboratorio> listar() {

        return service.listar();
    }


    // Lista solo laboratorios activos.
    public List<Laboratorio> listarActivos() {

        return service.listarActivos();
    }


    // Busca por ID.
    public Optional<Laboratorio> buscarPorId(
            int idLaboratorio
    ) {

        return service.buscarPorId(
                idLaboratorio
        );
    }


    // Busca por código.
    public Optional<Laboratorio> buscarPorCodigo(
            String codigo
    ) {

        return service.buscarPorCodigo(
                codigo
        );
    }


    // Actualiza un laboratorio.
    public void actualizar(
            int idLaboratorio,
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) {

        verificarEscritura();
        service.actualizar(
                idLaboratorio,
                codigo,
                nombre,
                ubicacion,
                capacidad,
                estado
        );
    }


    // Elimina un laboratorio.
    public void eliminar(
            int idLaboratorio
    ) {

        verificarEscritura();
        service.eliminar(
                idLaboratorio
        );
    }
    private void verificarEscritura() {
        if (soloLectura) {
            throw new BusinessException("Solo tienes permiso para consultar laboratorios");
        }
    }
}
