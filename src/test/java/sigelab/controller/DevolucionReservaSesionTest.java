package sigelab.controller;

import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import sigelab.enums.CondicionDevolucion;
import sigelab.model.*;
import sigelab.repository.DevolucionRepository.PrestamoPendiente;
import sigelab.repository.ReservaRepository;
import sigelab.service.*;
import static org.junit.jupiter.api.Assertions.*;

class DevolucionReservaSesionTest {
    @Test void devolucionesSiempreUsaActorDeSesionParaLecturasYEscritura() {
        List<Integer> actores = new ArrayList<>();
        DevolucionController c = new DevolucionController(new DevolucionService() {
            public List<PrestamoPendiente> listarPrestamosPendientes(int actor) { actores.add(actor); return List.of(); }
            public List<Pendiente> buscarPendientes(int prestamo,int actor) { actores.add(actor); return List.of(); }
            public List<Devolucion> registrar(int prestamo,Map<Integer,CondicionDevolucion> lote,String obs,int actor) {
                actores.add(actor); assertEquals(41,prestamo); return List.of();
            }
        },42);
        c.listarPrestamosPendientes(); c.buscarPendientes(41); c.registrar(41,Map.of(101,CondicionDevolucion.BUENO),"");
        assertEquals(List.of(42,42,42),actores);
    }
    @Test void reservaDistingueSolicitanteDeActorYNoPermiteCambiarSesionConIdDeReserva() {
        List<Integer> actores = new ArrayList<>();
        ReservaController c = new ReservaController(new ReservaService() {
            public Reserva crear(int solicitante,int equipo,LocalDate inicio,LocalDate fin,String obs,int actor) {
                assertEquals(10,solicitante); actores.add(actor); return new Reserva();
            }
            public List<ReservaRepository.Resumen> listar(int actor) { actores.add(actor); return List.of(); }
            public Optional<Reserva> buscarPorId(int id,int actor) { actores.add(actor); return Optional.empty(); }
            public void cancelar(int id,int actor) { actores.add(actor); }
            public List<ReservaRepository.Solicitante> listarSolicitantes(int actor) { actores.add(actor); return List.of(); }
            public List<ReservaRepository.EquipoReservable> listarEquipos(int actor) { actores.add(actor); return List.of(); }
        },42);
        c.crear(10,7,LocalDate.now(),LocalDate.now(),""); c.listar(); c.buscarPorId(999); c.cancelar(999);
        c.listarSolicitantes(); c.listarEquipos();
        assertEquals(Collections.nCopies(6,42),actores);
    }
}
