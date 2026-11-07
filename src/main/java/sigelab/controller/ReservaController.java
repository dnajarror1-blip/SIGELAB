package sigelab.controller;

import java.time.LocalDate;
import java.util.*;
import sigelab.model.Reserva;
import sigelab.repository.ReservaRepository;
import sigelab.service.ReservaService;

public class ReservaController {
    private final ReservaService servicio;
    private final int actor;
    public ReservaController(int actor) { this(new ReservaService(),actor); }
    public ReservaController(ReservaService servicio,int actor) { this.servicio = servicio; this.actor = actor; }
    public Reserva crear(int solicitante,int equipo,LocalDate inicio,LocalDate fin,String obs) {
        return servicio.crear(solicitante,equipo,inicio,fin,obs,actor);
    }
    public List<ReservaRepository.Resumen> listar() { return servicio.listar(actor); }
    public Optional<Reserva> buscarPorId(int idReserva) { return servicio.buscarPorId(idReserva,actor); }
    public void cancelar(int idReserva) { servicio.cancelar(idReserva,actor); }
    public List<ReservaRepository.Solicitante> listarSolicitantes() { return servicio.listarSolicitantes(actor); }
    public List<ReservaRepository.EquipoReservable> listarEquipos() { return servicio.listarEquipos(actor); }
}
