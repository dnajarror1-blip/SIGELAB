package sigelab.service;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sigelab.enums.*;
import sigelab.exception.*;
import sigelab.model.*;
import sigelab.repository.*;
import static org.junit.jupiter.api.Assertions.*;

/** Servicio real, permisos reales y escritura provisional confirmada solo por commit. */
class DevolucionTransaccionTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);

    @Test void d1BuenoFinalizaConFechaRealYAuditoria() {
        Escenario e = new Escenario(1);
        List<Devolucion> resultado = e.devolver(101, CondicionDevolucion.BUENO);
        assertEquals(1, resultado.size());
        assertEquals(LocalDate.of(2026,10,30), resultado.getFirst().getFechaDevolucion());
        assertEquals(42, resultado.getFirst().getIdUsuario());
        assertEquals(EstadoEquipo.DISPONIBLE, e.equipos.get(1));
        assertEquals(EstadoPrestamo.FINALIZADO, e.estado);
        assertEquals(1, e.devoluciones.size()); assertEquals(List.of("EQUIPO_DEVUELTO"), e.eventos);
        assertEquals(1, e.commits); assertEquals(0,e.rollbacks); assertTrue(e.cerrada);
    }
    @Test void d2DanadoNoSeVuelveDisponibleNiEntraAMantenimiento() {
        Escenario e = new Escenario(1); e.devolver(101,CondicionDevolucion.DANADO);
        assertEquals(EstadoEquipo.DANADO,e.equipos.get(1));
        assertEquals(CondicionDevolucion.DANADO,e.devoluciones.get(101).getCondicion());
    }
    @Test void d3ParcialConservaActivoYOtroPendiente() {
        Escenario e = new Escenario(2); e.devolver(101,CondicionDevolucion.BUENO);
        assertEquals(EstadoPrestamo.ACTIVO,e.estado); assertEquals(EstadoEquipo.PRESTADO,e.equipos.get(2));
        assertEquals(List.of(102),e.servicio.buscarPendientes(41,42).stream()
                .map(p -> p.detalle().getIdDetallePrestamo()).toList());
    }
    @Test void d4UltimaDevolucionFinaliza() {
        Escenario e = new Escenario(2); e.devolver(101,CondicionDevolucion.BUENO);
        e.devolver(102,CondicionDevolucion.DANADO);
        assertEquals(EstadoPrestamo.FINALIZADO,e.estado); assertEquals(2,e.devoluciones.size());
        assertEquals(2,e.eventos.size());
    }
    @Test void d5DobleDevolucionSeRechazaEnServicioAunquePrestamoSigaActivo() {
        Escenario e = new Escenario(2); e.devolver(101,CondicionDevolucion.BUENO);
        assertThrows(BusinessException.class, () -> e.devolver(101,CondicionDevolucion.DANADO));
        assertEquals(1,e.devoluciones.size()); assertEquals(1,e.eventos.size()); assertEquals(1,e.commits);
        assertEquals(EstadoEquipo.DISPONIBLE,e.equipos.get(1));
    }
    @Test void d6DetalleDeOtroPrestamo() {
        Escenario e = new Escenario(1); e.detalles.put(103,detalle(103,99,3));
        assertThrows(BusinessException.class, () -> e.devolver(103,CondicionDevolucion.BUENO)); e.sinCambios();
    }
    @Test void d7PrestamoInexistente() {
        Escenario e = new Escenario(1);
        assertThrows(BusinessException.class, () -> e.servicio.registrar(99,Map.of(101,CondicionDevolucion.BUENO),"",42));
        e.sinCambios();
    }
    @Test void d8DetalleInexistente() {
        Escenario e = new Escenario(1);
        assertThrows(BusinessException.class, () -> e.devolver(999,CondicionDevolucion.BUENO)); e.sinCambios();
    }
    @Test void d9CondicionNulaSeRechazaAntesDeConectar() {
        Escenario e = new Escenario(1); Map<Integer,CondicionDevolucion> lote = new HashMap<>(); lote.put(101,null);
        assertThrows(BusinessException.class, () -> e.servicio.registrar(41,lote,"",42)); assertEquals(0,e.aperturas);
    }
    @ParameterizedTest @ValueSource(strings={"insertar","equipo","prestamo","auditoria","commit"})
    void d10aD13FalloEnCadaPasoRevierteTodo(String paso) {
        Escenario e = new Escenario(1); e.fallo = paso;
        DataAccessException error = assertThrows(DataAccessException.class, () -> e.devolver(101,CondicionDevolucion.BUENO));
        assertFalse(error.getMessage().contains("SQL privado")); e.sinCambios();
        assertEquals(1,e.rollbacks); assertTrue(e.cerrada);
    }
    @Test void d14DosEquiposSeConfirmanJuntosConCondicionesDistintas() {
        Escenario e = new Escenario(2);
        e.servicio.registrar(41,Map.of(102,CondicionDevolucion.DANADO,101,CondicionDevolucion.BUENO),"",42);
        assertEquals(2,e.devoluciones.size()); assertEquals(EstadoPrestamo.FINALIZADO,e.estado);
        assertEquals(EstadoEquipo.DISPONIBLE,e.equipos.get(1)); assertEquals(EstadoEquipo.DANADO,e.equipos.get(2));
        assertEquals(List.of(1,2),e.ordenEscritura); assertEquals(1,e.commits);
    }
    @Test void d14FalloEnSegundoEquipoRevierteTambienElPrimero() {
        Escenario e = new Escenario(2); e.fallo = "segundo";
        assertThrows(DataAccessException.class, () -> e.servicio.registrar(41,
                Map.of(101,CondicionDevolucion.BUENO,102,CondicionDevolucion.DANADO),"",42));
        e.sinCambios(); assertEquals(1,e.rollbacks);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","ENCARGADO"})
    void d15RolesAutorizadosConIdsNoFijos(String rol) {
        Escenario e = new Escenario(1); e.rol = rol; e.devolver(101,CondicionDevolucion.BUENO); assertEquals(1,e.commits);
    }
    @ParameterizedTest @ValueSource(strings={"ESTUDIANTE","DESCONOCIDO","INACTIVO"})
    void d15SinPermisoOInactivoNoPuedeEscribirNiConsultar(String rol) {
        Escenario e = new Escenario(1); e.rol = rol;
        assertThrows(BusinessException.class, () -> e.devolver(101,CondicionDevolucion.BUENO));
        assertThrows(BusinessException.class, () -> e.servicio.buscarPendientes(41,42));
        assertThrows(BusinessException.class, () -> e.servicio.listarPrestamosPendientes(42)); e.sinCambios();
    }
    @Test void prestamoFinalizadoRechazaInclusoConDetalleInconsistente() {
        Escenario e = new Escenario(1); e.estado = EstadoPrestamo.FINALIZADO;
        assertThrows(BusinessException.class, () -> e.devolver(101,CondicionDevolucion.BUENO));
        assertTrue(e.devoluciones.isEmpty()); assertEquals(0,e.commits);
    }
    @Test void equipoConEstadoIncompatibleNoSeSobrescribe() {
        Escenario e = new Escenario(1); e.equipos.put(1,EstadoEquipo.MANTENIMIENTO);
        assertThrows(BusinessException.class, () -> e.devolver(101,CondicionDevolucion.BUENO));
        assertEquals(EstadoEquipo.MANTENIMIENTO,e.equipos.get(1)); assertTrue(e.devoluciones.isEmpty());
    }
    @Test void loteConDetalleAjenoNoHaceEscriturasPrevias() {
        Escenario e = new Escenario(1); e.detalles.put(103,detalle(103,99,3));
        assertThrows(BusinessException.class, () -> e.servicio.registrar(41,
                Map.of(101,CondicionDevolucion.BUENO,103,CondicionDevolucion.BUENO),"",42));
        assertTrue(e.ordenEscritura.isEmpty()); e.sinCambios();
    }
    @Test void validaVacioIdsYLongitudSinConexion() {
        Escenario e = new Escenario(1);
        assertThrows(BusinessException.class, () -> e.servicio.registrar(0,Map.of(101,CondicionDevolucion.BUENO),"",42));
        assertThrows(BusinessException.class, () -> e.servicio.registrar(41,Map.of(),"",42));
        assertThrows(BusinessException.class, () -> e.servicio.registrar(41,Map.of(0,CondicionDevolucion.BUENO),"",42));
        assertThrows(BusinessException.class, () -> e.servicio.registrar(41,Map.of(101,CondicionDevolucion.BUENO),"x".repeat(251),42));
        assertThrows(BusinessException.class, () -> e.servicio.registrar(41,Map.of(101,CondicionDevolucion.BUENO),"",0));
        assertEquals(0,e.aperturas);
    }

    private static DetallePrestamo detalle(int id, int prestamo, int equipo) {
        DetallePrestamo d = new DetallePrestamo(prestamo,equipo); d.setIdDetallePrestamo(id); return d;
    }
    private static class Escenario {
        Map<Integer,EstadoEquipo> equipos = new HashMap<>(), equiposTx = new HashMap<>();
        Map<Integer,Devolucion> devoluciones = new HashMap<>(), devolucionesTx = new HashMap<>();
        Map<Integer,DetallePrestamo> detalles = new HashMap<>();
        List<String> eventos = new ArrayList<>(), eventosTx = new ArrayList<>();
        List<Integer> ordenEscritura = new ArrayList<>();
        EstadoPrestamo estado = EstadoPrestamo.ACTIVO, estadoTx;
        String fallo = "", rol = "ENCARGADO";
        int commits, rollbacks, aperturas, aislamiento; boolean autoCommit = true, cerrada;
        final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Connection.class}, (p,m,a) -> {
                    switch (m.getName()) {
                        case "setTransactionIsolation" -> aislamiento = (int)a[0];
                        case "setAutoCommit" -> {
                            autoCommit = (boolean)a[0]; equiposTx = new HashMap<>(equipos);
                            devolucionesTx = new HashMap<>(devoluciones); eventosTx = new ArrayList<>(eventos); estadoTx = estado;
                        }
                        case "commit" -> { fallar("commit"); commits++; equipos = new HashMap<>(equiposTx);
                            devoluciones = new HashMap<>(devolucionesTx); eventos = new ArrayList<>(eventosTx); estado = estadoTx; }
                        case "rollback" -> { rollbacks++; equiposTx.clear(); devolucionesTx.clear(); eventosTx.clear(); }
                        case "close" -> cerrada = true;
                        default -> throw new AssertionError(m.getName());
                    }
                    return null;
                });
        final DevolucionService servicio;
        Escenario(int cantidad) {
            for (int i=1;i<=cantidad;i++) { equipos.put(i,EstadoEquipo.PRESTADO); detalles.put(100+i,detalle(100+i,41,i)); }
            PermisoService permisos = new PermisoService(new RolRepository() {
                public Optional<Rol> buscarDeUsuarioActivo(Connection c,int id) {
                    comprobar(c); assertEquals(42,id);
                    if (rol.equals("INACTIVO")) return Optional.empty();
                    Rol r = new Rol(rol); r.setIdRol(987); return Optional.of(r);
                }
            });
            servicio = new DevolucionService(new DevolucionRepository() {
                public boolean existeParaDetalle(Connection c,int id) { comprobar(c); return devolucionesTx.containsKey(id); }
                public int guardar(Connection c,Devolucion d) throws SQLException {
                    comprobar(c); devolucionesTx.put(d.getIdDetallePrestamo(),d); fallar("insertar"); return 800 + d.getIdDetallePrestamo();
                }
            },new PrestamoRepository() {
                public Optional<Prestamo> buscarParaDevolucion(Connection c,int id) {
                    comprobar(c); if (id != 41) return Optional.empty();
                    Prestamo p = new Prestamo(); p.setIdPrestamo(id); p.setEstado(estadoTx); return Optional.of(p);
                }
                public void finalizarSinPendientes(Connection c,int id) throws SQLException {
                    comprobar(c); estadoTx = EstadoPrestamo.FINALIZADO; fallar("prestamo");
                }
            },new DetallePrestamoRepository() {
                public Optional<DetallePrestamo> buscarPorId(Connection c,int id) { comprobar(c); return Optional.ofNullable(detalles.get(id)); }
                public List<DetallePrestamo> buscarPendientes(Connection c,int id) {
                    comprobar(c); return detalles.values().stream().filter(d -> d.getIdPrestamo()==id
                            && !devolucionesTx.containsKey(d.getIdDetallePrestamo()))
                            .sorted(Comparator.comparingInt(DetallePrestamo::getIdEquipo)).toList();
                }
            },new EquipoRepository() {
                public Optional<Equipo> buscarPorId(int id,Connection c) {
                    comprobar(c); if (!equiposTx.containsKey(id)) return Optional.empty();
                    Equipo e = new Equipo(); e.setIdEquipo(id); e.setEstado(equiposTx.get(id)); return Optional.of(e);
                }
                public void actualizarEstado(int id,EstadoEquipo nuevo,Connection c) throws SQLException {
                    comprobar(c); ordenEscritura.add(id); equiposTx.put(id,nuevo); fallar("equipo");
                    if (id==2) fallar("segundo");
                }
            },permisos,new AuditService() {
                public void registrar(Integer actor,String accion,String entidad,Integer id,String descripcion,Connection c) {
                    comprobar(c); assertEquals(42,actor); assertEquals("Devolucion",entidad); assertNotNull(id);
                    eventosTx.add(accion);
                    if (fallo.equals("auditoria")) throw new DataAccessException("SQL privado");
                }
            }, () -> { aperturas++; return conn; },RELOJ);
        }
        List<Devolucion> devolver(int detalle,CondicionDevolucion condicion) {
            return servicio.registrar(41,Map.of(detalle,condicion),"Observación",42);
        }
        void comprobar(Connection c) { assertSame(conn,c); assertFalse(autoCommit); assertEquals(Connection.TRANSACTION_SERIALIZABLE,aislamiento); }
        void fallar(String paso) throws SQLException { if (fallo.equals(paso)) throw new SQLException("SQL privado"); }
        void sinCambios() {
            assertTrue(devoluciones.isEmpty()); assertTrue(eventos.isEmpty()); assertEquals(0,commits);
            assertEquals(EstadoPrestamo.ACTIVO,estado);
            assertTrue(equipos.values().stream().allMatch(s -> s==EstadoEquipo.PRESTADO));
        }
    }
}
