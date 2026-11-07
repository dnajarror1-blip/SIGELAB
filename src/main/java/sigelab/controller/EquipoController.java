package sigelab.controller;

import sigelab.exception.BusinessException;
import sigelab.model.Equipo;
import sigelab.model.Laboratorio;
import sigelab.model.TipoEquipo;
import sigelab.service.EquipoService;
import sigelab.service.LaboratorioService;
import sigelab.service.TipoEquipoService;

import java.util.List;
import java.util.Optional;

public class EquipoController {

    private final EquipoService equipoService;
    private final TipoEquipoService tipoEquipoService;
    private final LaboratorioService laboratorioService;
    private final boolean soloLectura;

    public EquipoController() {
        this(false);
    }

    public EquipoController(boolean soloLectura) {
        this(soloLectura, new EquipoService());
    }

    EquipoController(boolean soloLectura, EquipoService equipoService) {
        this.soloLectura = soloLectura;
        this.equipoService = equipoService;
        this.tipoEquipoService = new TipoEquipoService();
        this.laboratorioService = new LaboratorioService();
    }


    public Equipo crear(
            String codigoInventario,
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String numeroSerie,
            int idLaboratorio,
            String observaciones
    ) {

        verificarEscritura();
        return equipoService.crear(
                codigoInventario,
                nombre,
                idTipoEquipo,
                marca,
                modelo,
                numeroSerie,
                idLaboratorio,
                observaciones
        );
    }


    public List<Equipo> listar() {
        return equipoService.listar();
    }


    public List<Equipo> listarDisponibles() {
        return equipoService.listarDisponibles();
    }


    public Optional<Equipo> buscarPorId(
            int idEquipo
    ) {

        return equipoService.buscarPorId(
                idEquipo
        );
    }


    public Optional<Equipo> buscarPorCodigoInventario(
            String codigo
    ) {

        return equipoService.buscarPorCodigoInventario(
                codigo
        );
    }


    public void actualizar(
            int idEquipo,
            String codigoInventario,
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String numeroSerie,
            int idLaboratorio,
            String observaciones
    ) {

        verificarEscritura();
        equipoService.actualizar(
                idEquipo,
                codigoInventario,
                nombre,
                idTipoEquipo,
                marca,
                modelo,
                numeroSerie,
                idLaboratorio,
                observaciones
        );
    }


    public void eliminar(
            int idEquipo
    ) {

        verificarEscritura();
        equipoService.eliminar(
                idEquipo
        );
    }


    // Datos para los ComboBox.
    public List<TipoEquipo> listarTiposEquipo() {
        return tipoEquipoService.listar();
    }


    public List<Laboratorio> listarLaboratorios() {
        return laboratorioService.listar();
    }

    private void verificarEscritura() {
        if (soloLectura) {
            throw new BusinessException("Solo tienes permiso para consultar equipos");
        }
    }
}
