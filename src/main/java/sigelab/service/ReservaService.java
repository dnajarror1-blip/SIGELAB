package sigelab.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoReserva;
import sigelab.exception.*;
import sigelab.model.Reserva;
import sigelab.repository.*;

public class ReservaService {
    private final ReservaRepository reservas;
    private final EquipoRepository equipos;
    private final PermisoService permisos;
    private final AuditService auditoria;
    private final ConexionReserva conexiones;
    private final Clock reloj;
    @FunctionalInterface interface ConexionReserva { Connection abrir() throws SQLException; }
    @FunctionalInterface private interface Operacion<T> { T ejecutar(Connection c) throws SQLException; }

    public ReservaService() {
        this(new ReservaRepository(), new EquipoRepository(), new PermisoService(), new AuditService(),
                DatabaseConfig::getConnection, Clock.systemDefaultZone());
    }
    ReservaService(ReservaRepository reservas, EquipoRepository equipos, PermisoService permisos,
            AuditService auditoria, ConexionReserva conexiones, Clock reloj) {
        this.reservas = reservas; this.equipos = equipos; this.permisos = permisos;
        this.auditoria = auditoria; this.conexiones = conexiones; this.reloj = reloj;
    }

    public Reserva crear(int solicitante, int equipo, LocalDate inicio, LocalDate fin, String observaciones, int actor) {
        if (solicitante<=0 || equipo<=0) throw new BusinessException("Seleccione un solicitante y un equipo válidos.");
        if (inicio==null || fin==null || inicio.getYear()<1 || inicio.getYear()>9999 || fin.getYear()<1 || fin.getYear()>9999)
            throw new BusinessException("Las fechas deben estar entre los años 1 y 9999.");
        if (fin.isBefore(inicio)) throw new BusinessException("La fecha final no puede ser anterior a la inicial.");
        String obs = observaciones==null ? null : observaciones.trim();
        if (obs!=null && obs.length()>250) throw new BusinessException("Las observaciones no pueden superar los 250 caracteres.");
        return enTransaccion(actor,c -> {
            if (!reservas.existeSolicitanteActivo(c,solicitante))
                throw new BusinessException("El solicitante no existe o está inactivo.");
            if (equipos.buscarPorId(equipo,c).isEmpty()) throw new BusinessException("El equipo no existe.");
            // El estado físico actual no predice disponibilidad futura. Reservar nunca lo modifica.
            if (reservas.existeSolapamiento(c,equipo,inicio,fin))
                throw new BusinessException("El equipo ya tiene una reserva activa en esas fechas.");
            Reserva r = new Reserva(solicitante,equipo,inicio,fin,EstadoReserva.ACTIVA,LocalDateTime.now(reloj),obs);
            r.setIdReserva(reservas.guardar(c,r));
            auditoria.registrar(actor,"RESERVA_CREADA","Reserva",r.getIdReserva(),
                    "Reserva del equipo " + equipo + " para el usuario " + solicitante + ": " + inicio + " a " + fin + ".",c);
            return r;
        });
    }

    public List<ReservaRepository.Resumen> listar(int actor) { return enTransaccion(actor,reservas::listar); }
    public Optional<Reserva> buscarPorId(int idReserva,int actor) {
        validarId(idReserva);
        return enTransaccion(actor,c -> reservas.buscarPorId(c,idReserva));
    }
    public List<ReservaRepository.Solicitante> listarSolicitantes(int actor) {
        return enTransaccion(actor,reservas::listarSolicitantesActivos);
    }
    public List<ReservaRepository.EquipoReservable> listarEquipos(int actor) {
        return enTransaccion(actor,reservas::listarEquipos);
    }

    public void cancelar(int idReserva,int actor) {
        validarId(idReserva);
        enTransaccion(actor,c -> {
            Reserva r = reservas.buscarParaCancelar(c,idReserva)
                    .orElseThrow(() -> new BusinessException("La reserva no existe."));
            if (r.getEstado()!=EstadoReserva.ACTIVA) throw new BusinessException("Solo se puede cancelar una reserva activa.");
            reservas.cancelar(c,idReserva);
            auditoria.registrar(actor,"RESERVA_CANCELADA","Reserva",idReserva,
                    "Cancelación de la reserva del equipo " + r.getIdEquipo() + ".",c);
            return null;
        });
    }

    private void validarId(int id) { if (id<=0) throw new BusinessException("Seleccione una reserva válida."); }
    private <T> T enTransaccion(int actor,Operacion<T> operacion) {
        if (actor<=0) throw new BusinessException("Debe iniciar sesión.");
        try (Connection c = conexiones.abrir()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                permisos.validarAcceso(c,actor,Modulo.RESERVAS_PRESTAMOS);
                T resultado = operacion.ejecutar(c);
                c.commit(); return resultado;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
                if (e instanceof BusinessException negocio) throw negocio;
                throw new DataAccessException("No se pudo completar la reserva. Compruebe su estado antes de reintentar.",e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar para procesar la reserva.",e);
        }
    }
}
