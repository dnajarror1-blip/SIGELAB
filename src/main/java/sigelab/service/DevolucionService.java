package sigelab.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import sigelab.config.DatabaseConfig;
import sigelab.enums.*;
import sigelab.exception.*;
import sigelab.model.*;
import sigelab.repository.*;

public class DevolucionService {
    private final DevolucionRepository devoluciones;
    private final PrestamoRepository prestamos;
    private final DetallePrestamoRepository detalles;
    private final EquipoRepository equipos;
    private final PermisoService permisos;
    private final AuditService auditoria;
    private final ConexionDevolucion conexiones;
    private final Clock reloj;

    @FunctionalInterface interface ConexionDevolucion { Connection abrir() throws SQLException; }
    @FunctionalInterface private interface Operacion<T> { T ejecutar(Connection c) throws SQLException; }
    public record Pendiente(DetallePrestamo detalle, Equipo equipo) { }

    public DevolucionService() {
        this(new DevolucionRepository(), new PrestamoRepository(), new DetallePrestamoRepository(),
                new EquipoRepository(), new PermisoService(), new AuditService(),
                DatabaseConfig::getConnection, Clock.systemDefaultZone());
    }

    DevolucionService(DevolucionRepository devoluciones, PrestamoRepository prestamos,
            DetallePrestamoRepository detalles, EquipoRepository equipos, PermisoService permisos,
            AuditService auditoria, ConexionDevolucion conexiones, Clock reloj) {
        this.devoluciones = devoluciones; this.prestamos = prestamos; this.detalles = detalles;
        this.equipos = equipos; this.permisos = permisos; this.auditoria = auditoria;
        this.conexiones = conexiones; this.reloj = reloj;
    }

    public List<DevolucionRepository.PrestamoPendiente> listarPrestamosPendientes(int actor) {
        return enTransaccion(actor, devoluciones::listarPrestamosPendientes);
    }

    public List<Pendiente> buscarPendientes(int idPrestamo, int actor) {
        validarId(idPrestamo);
        return enTransaccion(actor, c -> {
            Prestamo p = prestamos.buscarParaDevolucion(c, idPrestamo)
                    .orElseThrow(() -> new BusinessException("El préstamo no existe."));
            if (p.getEstado() != EstadoPrestamo.ACTIVO) return List.of();
            List<Pendiente> lista = new ArrayList<>();
            for (DetallePrestamo d : detalles.buscarPendientes(c, idPrestamo)) {
                Equipo e = equipos.buscarPorId(d.getIdEquipo(), c)
                        .orElseThrow(() -> new BusinessException("El equipo del préstamo no existe."));
                lista.add(new Pendiente(d, e));
            }
            return lista;
        });
    }

    /** Una condición por detalle. La fecha y el registrador siempre los determina el servicio. */
    public List<Devolucion> registrar(int idPrestamo, Map<Integer, CondicionDevolucion> condiciones,
                                      String observaciones, int actor) {
        validarId(idPrestamo);
        if (condiciones == null || condiciones.isEmpty())
            throw new BusinessException("Seleccione al menos un equipo pendiente.");
        Map<Integer, CondicionDevolucion> lote = new HashMap<>(condiciones);
        for (var entrada : lote.entrySet()) {
            if (entrada.getKey() == null || entrada.getKey() <= 0)
                throw new BusinessException("El detalle seleccionado no es válido.");
            if (entrada.getValue() == null) throw new BusinessException("Seleccione una condición válida.");
        }
        String obs = observaciones == null ? null : observaciones.trim();
        if (obs != null && obs.length() > 250)
            throw new BusinessException("Las observaciones no pueden superar los 250 caracteres.");
        return enTransaccion(actor, c -> {
            Prestamo prestamo = prestamos.buscarParaDevolucion(c, idPrestamo)
                    .orElseThrow(() -> new BusinessException("El préstamo no existe."));
            if (prestamo.getEstado() != EstadoPrestamo.ACTIVO)
                throw new BusinessException("El préstamo ya está finalizado.");
            List<DetallePrestamo> seleccionados = new ArrayList<>();
            for (int id : new TreeSet<>(lote.keySet())) {
                DetallePrestamo d = detalles.buscarPorId(c, id)
                        .orElseThrow(() -> new BusinessException("El detalle seleccionado no existe."));
                if (d.getIdPrestamo() != idPrestamo)
                    throw new BusinessException("El equipo no pertenece al préstamo seleccionado.");
                if (devoluciones.existeParaDetalle(c, id))
                    throw new BusinessException("El equipo ya fue devuelto en este préstamo.");
                seleccionados.add(d);
            }
            seleccionados.sort(Comparator.comparingInt(DetallePrestamo::getIdEquipo));
            // Se valida todo el lote antes de escribir y se conserva el orden de equipos de Préstamos.
            for (DetallePrestamo d : seleccionados) {
                Equipo e = equipos.buscarPorId(d.getIdEquipo(), c)
                        .orElseThrow(() -> new BusinessException("El equipo no existe."));
                if (e.getEstado() != EstadoEquipo.PRESTADO)
                    throw new BusinessException("El estado actual del equipo no permite devolverlo. Actualice la consulta.");
            }
            LocalDate hoy = LocalDate.now(reloj);
            List<Devolucion> resultado = new ArrayList<>();
            for (DetallePrestamo d : seleccionados) {
                CondicionDevolucion condicion = lote.get(d.getIdDetallePrestamo());
                Devolucion devolucion = new Devolucion(d.getIdDetallePrestamo(), actor, hoy, condicion, obs);
                devolucion.setIdDevolucion(devoluciones.guardar(c, devolucion));
                equipos.actualizarEstado(d.getIdEquipo(), condicion == CondicionDevolucion.BUENO
                        ? EstadoEquipo.DISPONIBLE : EstadoEquipo.DANADO, c);
                auditoria.registrar(actor, "EQUIPO_DEVUELTO", "Devolucion", devolucion.getIdDevolucion(),
                        "Préstamo " + idPrestamo + ", detalle " + d.getIdDetallePrestamo()
                                + ", equipo " + d.getIdEquipo() + ", condición " + condicion + ".", c);
                resultado.add(devolucion);
            }
            if (detalles.buscarPendientes(c, idPrestamo).isEmpty())
                prestamos.finalizarSinPendientes(c, idPrestamo);
            return List.copyOf(resultado);
        });
    }

    private void validarId(int id) {
        if (id <= 0) throw new BusinessException("Seleccione un préstamo válido.");
    }

    private <T> T enTransaccion(int actor, Operacion<T> operacion) {
        if (actor <= 0) throw new BusinessException("Debe iniciar sesión.");
        try (Connection c = conexiones.abrir()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                permisos.validarAcceso(c, actor, Modulo.RESERVAS_PRESTAMOS);
                T resultado = operacion.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
                if (e instanceof BusinessException negocio) throw negocio;
                throw new DataAccessException("No se pudo completar la devolución. Compruebe su estado antes de reintentar.", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar para procesar la devolución.", e);
        }
    }
}
