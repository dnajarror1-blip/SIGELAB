package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoPrestamo;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.model.*;
import sigelab.repository.*;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba el servicio real con colaboradores en memoria; nunca accede a SQL Server. */
class PrestamoTransaccionTest {
    @Test
    void cp04ConfirmaDisponibleConDetalleEstadoYAuditoria() {
        Escenario e = new Escenario();
        Prestamo p = prestamo();
        assertEquals(41, e.servicio.crearPrestamo(p, List.of(1)));
        assertEquals(41, p.getIdPrestamo());
        assertEquals(EstadoPrestamo.ACTIVO, p.getEstado());
        assertEquals(EstadoEquipo.PRESTADO, e.persistidos.get(1));
        assertEquals(List.of(1), e.detallesConfirmados);
        assertTrue(e.encabezadoConfirmado);
        assertTrue(e.auditoriaConfirmada);
        assertEquals(1, e.commits);
        assertEquals(0, e.rollbacks);
        assertTrue(e.cerrada);
    }

    @Test
    void cp05RechazaSegundoPrestamoDelMismoEquipo() {
        Escenario e = new Escenario();
        e.servicio.crearPrestamo(prestamo(), List.of(1));
        assertThrows(BusinessException.class,
                () -> e.servicio.crearPrestamo(prestamo(), List.of(1)));
        assertEquals(1, e.commits);
        assertEquals(1, e.rollbacks);
        assertEquals(List.of(1), e.detallesConfirmados);
    }

    @Test void cp06RechazaDanadoAlConfirmar() { rechazar(EstadoEquipo.DANADO); }
    @Test void cp07RechazaMantenimientoAlConfirmar() { rechazar(EstadoEquipo.MANTENIMIENTO); }
    @Test void cp08RechazaBajaAlConfirmar() { rechazar(EstadoEquipo.BAJA); }

    private void rechazar(EstadoEquipo estado) {
        Escenario e = new Escenario();
        e.persistidos.put(1, estado);
        assertThrows(BusinessException.class,
                () -> e.servicio.crearPrestamo(prestamo(), List.of(1)));
        assertEquals(0, e.commits);
        assertEquals(1, e.rollbacks);
        assertFalse(e.encabezadoConfirmado);
        assertEquals(estado, e.persistidos.get(1));
    }

    @Test
    void variosEquiposQuedanPrestados() {
        Escenario e = new Escenario();
        e.servicio.crearPrestamo(prestamo(), List.of(2, 1));
        assertEquals(List.of(1, 2), e.detallesConfirmados);
        assertTrue(e.persistidos.values().stream().allMatch(s -> s == EstadoEquipo.PRESTADO));
    }

    @Test
    void falloDespuesDelSegundoCambioRevierteTodosLosEquipos() {
        comprobarRollback("estado");
    }

    @Test void falloDeDetalleRevierteEncabezado() { comprobarRollback("detalle"); }
    @Test void falloDeAuditoriaRevierteTodaLaOperacion() { comprobarRollback("auditoria"); }
    @Test void falloDeCommitEjecutaRollback() { comprobarRollback("commit"); }

    private void comprobarRollback(String punto) {
        Escenario e = new Escenario();
        e.fallo = punto;
        Prestamo p = prestamo();
        assertThrows(DataAccessException.class,
                () -> e.servicio.crearPrestamo(p, List.of(1, 2)));
        assertEquals(1, e.rollbacks);
        assertEquals(0, e.commits);
        assertNull(p.getIdPrestamo());
        assertFalse(e.encabezadoConfirmado);
        assertFalse(e.auditoriaConfirmada);
        assertTrue(e.detallesConfirmados.isEmpty());
        assertTrue(e.persistidos.values().stream().allMatch(s -> s == EstadoEquipo.DISPONIBLE));
        if (punto.equals("estado")) assertEquals(2, e.actualizaciones);
        assertTrue(e.cerrada);
    }

    @Test
    void rechazaVacioYDuplicadosSinAbrirConexion() {
        Escenario e = new Escenario();
        assertThrows(BusinessException.class, () -> e.servicio.crearPrestamo(prestamo(), List.of()));
        assertThrows(BusinessException.class, () -> e.servicio.crearPrestamo(prestamo(), List.of(1, 1)));
        assertEquals(0, e.aperturas);
    }

    @Test
    void rechazaSolicitanteORegistradorInexistenteOInactivo() {
        for (int id : List.of(10, 20)) {
            Escenario e = new Escenario();
            e.usuarioInvalido = id;
            assertThrows(BusinessException.class,
                    () -> e.servicio.crearPrestamo(prestamo(), List.of(1)));
            assertEquals(1, e.rollbacks);
            assertFalse(e.encabezadoConfirmado);
        }
    }

    @Test
    void estudiantePuedeSerSolicitantePeroNoRegistrador() {
        Escenario e = new Escenario();
        e.rolSolicitante = "ESTUDIANTE";
        e.servicio.crearPrestamo(prestamo(), List.of(1));
        Escenario rechazado = new Escenario();
        rechazado.rolRegistrador = "ESTUDIANTE";
        assertThrows(BusinessException.class,
                () -> rechazado.servicio.crearPrestamo(prestamo(), List.of(1)));
        assertEquals(0, rechazado.commits);
    }

    @Test
    void equipoInexistenteRevierteSinCrearEncabezado() {
        Escenario e = new Escenario();
        assertThrows(BusinessException.class,
                () -> e.servicio.crearPrestamo(prestamo(), List.of(3)));
        assertEquals(1, e.rollbacks);
        assertFalse(e.encabezadoConfirmado);
    }

    private static Prestamo prestamo() {
        return new Prestamo(10, 20, LocalDate.of(2026, 10, 29),
                LocalDate.of(2026, 10, 30), null, "Prueba segura");
    }

    private static class Escenario {
        final Map<Integer, EstadoEquipo> persistidos = new HashMap<>(Map.of(
                1, EstadoEquipo.DISPONIBLE, 2, EstadoEquipo.DISPONIBLE));
        final Map<Integer, EstadoEquipo> pendientes = new HashMap<>();
        final List<Integer> detalles = new ArrayList<>();
        final List<Integer> detallesConfirmados = new ArrayList<>();
        boolean encabezado, auditoria, encabezadoConfirmado, auditoriaConfirmada, cerrada;
        boolean autoCommit = true;
        int aislamiento, commits, rollbacks, aperturas, actualizaciones, usuarioInvalido = -1;
        String fallo = "", rolSolicitante = "ESTUDIANTE", rolRegistrador = "ENCARGADO";
        final Connection conn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setTransactionIsolation" -> aislamiento = (int) args[0];
                        case "setAutoCommit" -> {
                            autoCommit = (boolean) args[0];
                            pendientes.clear(); pendientes.putAll(persistidos);
                            detalles.clear(); encabezado = false; auditoria = false;
                        }
                        case "commit" -> {
                            if (fallo.equals("commit")) throw new SQLException("Fallo simulado");
                            commits++;
                            persistidos.clear(); persistidos.putAll(pendientes);
                            detallesConfirmados.addAll(detalles);
                            encabezadoConfirmado = encabezado;
                            auditoriaConfirmada = auditoria;
                        }
                        case "rollback" -> { rollbacks++; pendientes.clear(); detalles.clear(); }
                        case "close" -> cerrada = true;
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
                    return null;
                });
        final PrestamoService servicio = new PrestamoService(
                new EquipoRepository() {
                    @Override public Optional<Equipo> buscarPorId(int id, Connection c) {
                        comprobarConexion(c);
                        if (!pendientes.containsKey(id)) return Optional.empty();
                        Equipo equipo = new Equipo();
                        equipo.setIdEquipo(id); equipo.setEstado(pendientes.get(id));
                        return Optional.of(equipo);
                    }
                    @Override public void actualizarEstado(int id, EstadoEquipo estado, Connection c)
                            throws SQLException {
                        comprobarConexion(c);
                        pendientes.put(id, estado);
                        actualizaciones++;
                        if (fallo.equals("estado") && id == 2) throw new SQLException("Fallo simulado");
                    }
                },
                new PrestamoRepository() {
                    @Override public Optional<Rol> buscarRolDeUsuarioActivo(Connection c, int id) {
                        comprobarConexion(c);
                        return id == usuarioInvalido ? Optional.empty()
                                : Optional.of(new Rol(id == 10 ? rolSolicitante : rolRegistrador));
                    }
                    @Override public int guardar(Connection c, Prestamo p) {
                        comprobarConexion(c);
                        assertEquals(EstadoPrestamo.ACTIVO, p.getEstado());
                        encabezado = true;
                        return 41;
                    }
                },
                new DetallePrestamoRepository() {
                    @Override public void guardar(Connection c, DetallePrestamo d) {
                        comprobarConexion(c);
                        assertEquals(41, d.getIdPrestamo());
                        detalles.add(d.getIdEquipo());
                        if (fallo.equals("detalle")) throw new DataAccessException("Fallo simulado");
                    }
                },
                new AuditService() {
                    @Override public void registrar(Integer usuario, String accion, String entidad,
                            Integer registro, String descripcion, Connection c) {
                        comprobarConexion(c);
                        assertEquals(20, usuario);
                        assertEquals(41, registro);
                        assertEquals("PRESTAMO_CREADO", accion);
                        assertEquals("Prestamo", entidad);
                        auditoria = true;
                        if (fallo.equals("auditoria")) throw new IllegalStateException("Fallo simulado");
                    }
                }, () -> { aperturas++; cerrada = false; return conn; }, null);

        private void comprobarConexion(Connection c) {
            assertSame(conn, c, "Todos los repositorios deben recibir la misma conexión");
            assertFalse(autoCommit);
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, aislamiento);
        }
    }
}
