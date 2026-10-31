package sigelab.service;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import sigelab.enums.*;
import sigelab.exception.*;
import sigelab.model.*;
import sigelab.repository.*;
import static org.junit.jupiter.api.Assertions.*;

class ReservaTransaccionTest {
    private static final LocalDate INICIO = LocalDate.of(2026,11,1), FIN = INICIO.plusDays(2);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"),ZoneOffset.UTC);
    @Test void creaReservaActivaConFechaDelSistemaYAuditoria() {
        Escenario e = new Escenario(); Reserva r = e.crear(INICIO,FIN);
        assertEquals(71,r.getIdReserva()); assertEquals(EstadoReserva.ACTIVA,r.getEstado());
        assertEquals(LocalDateTime.of(2026,10,30,12,0),r.getFechaCreacion());
        assertEquals(1,e.reservas.size()); assertEquals(List.of("RESERVA_CREADA"),e.eventos);
        assertEquals(1,e.commits); assertTrue(e.cerrada);
    }
    @ParameterizedTest @EnumSource(EstadoEquipo.class)
    void reservaFuturaNoExigeDisponibilidadFisicaNiCambiaEquipo(EstadoEquipo estado) {
        Escenario e = new Escenario(); e.equipo = estado; e.crear(INICIO,FIN);
        assertEquals(estado,e.equipo); assertEquals(1,e.reservas.size());
    }
    @ParameterizedTest @ValueSource(strings={"INEXISTENTE","INACTIVO"})
    void solicitanteInexistenteOInactivoSeRechaza(String estado) {
        Escenario e = new Escenario(); e.solicitante = estado;
        assertThrows(BusinessException.class, () -> e.crear(INICIO,FIN)); e.sinReservas(); assertEquals(1,e.rollbacks);
    }
    @Test void equipoInexistenteSeRechaza() {
        Escenario e = new Escenario(); e.equipo = null;
        assertThrows(BusinessException.class, () -> e.crear(INICIO,FIN)); e.sinReservas();
    }
    @Test void fechasNulasDesordenadasYFueraDeRangoNoAbrenConexion() {
        Escenario e = new Escenario();
        for (LocalDate[] fechas : List.of(new LocalDate[]{null,FIN},new LocalDate[]{INICIO,null},
                new LocalDate[]{FIN,INICIO},new LocalDate[]{LocalDate.of(0,1,1),FIN},
                new LocalDate[]{INICIO,LocalDate.of(10000,1,1)}))
            assertThrows(BusinessException.class, () -> e.crear(fechas[0],fechas[1]));
        assertEquals(0,e.aperturas);
    }
    @Test void idsInvalidosYObservacionesLargasNoAbrenConexion() {
        Escenario e = new Escenario();
        assertThrows(BusinessException.class, () -> e.servicio.crear(0,7,INICIO,FIN,"",42));
        assertThrows(BusinessException.class, () -> e.servicio.crear(10,0,INICIO,FIN,"",42));
        assertThrows(BusinessException.class, () -> e.servicio.crear(10,7,INICIO,FIN,"x".repeat(251),42));
        assertThrows(BusinessException.class, () -> e.servicio.cancelar(0,42)); assertEquals(0,e.aperturas);
    }
    @Test void permiteReservaDeUnSoloDia() { assertEquals(INICIO,new Escenario().crear(INICIO,INICIO).getFechaFin()); }
    @Test void noInventaProhibicionDeFechasPasadas() {
        assertNotNull(new Escenario().crear(INICIO.minusYears(1),FIN.minusYears(1)).getIdReserva());
    }
    @ParameterizedTest @ValueSource(ints={-2,-1,0,1,2})
    void rechazaSolapamientosIncluidosExtremosCoincidentes(int desplazamiento) {
        Escenario e = new Escenario(); e.crear(INICIO,FIN);
        assertThrows(BusinessException.class, () -> e.crear(INICIO.plusDays(desplazamiento),FIN.plusDays(desplazamiento)));
        assertEquals(1,e.reservas.size()); assertEquals(1,e.eventos.size()); assertEquals(1,e.commits);
    }
    @Test void permiteIntervalosSeparados() {
        Escenario e = new Escenario(); e.crear(INICIO,FIN); e.crear(FIN.plusDays(1),FIN.plusDays(2));
        assertEquals(2,e.reservas.size());
    }
    @Test void reservasDeOtroEquipoNoSeSolapan() {
        Escenario e = new Escenario(); e.crear(INICIO,FIN); e.reservas.get(71).setIdEquipo(8);
        e.crear(INICIO,FIN); assertEquals(2,e.reservas.size());
    }
    @Test void cancelarConservaEquipoYPermiteVolverAReservarIntervalo() {
        Escenario e = new Escenario(); e.crear(INICIO,FIN); e.servicio.cancelar(71,42);
        assertEquals(EstadoReserva.CANCELADA,e.reservas.get(71).getEstado()); assertEquals(EstadoEquipo.PRESTADO,e.equipo);
        e.crear(INICIO,FIN); assertEquals(2,e.reservas.size());
        assertEquals(List.of("RESERVA_CREADA","RESERVA_CANCELADA","RESERVA_CREADA"),e.eventos);
    }
    @Test void cancelarDosVecesEsRechazadoSinNuevoEvento() {
        Escenario e = new Escenario(); e.crear(INICIO,FIN); e.servicio.cancelar(71,42);
        assertThrows(BusinessException.class, () -> e.servicio.cancelar(71,42));
        assertEquals(2,e.commits); assertEquals(2,e.eventos.size());
    }
    @Test void cancelarInexistenteEsRechazado() {
        Escenario e = new Escenario(); assertThrows(BusinessException.class, () -> e.servicio.cancelar(99,42)); e.sinReservas();
    }
    @Test void completadaSeConsultaSinInventarTransicionYNoBloqueaReserva() {
        Escenario e = new Escenario(); e.crear(INICIO,FIN); e.reservas.get(71).setEstado(EstadoReserva.COMPLETADA);
        assertEquals(EstadoReserva.COMPLETADA,e.servicio.buscarPorId(71,42).orElseThrow().getEstado());
        assertThrows(BusinessException.class, () -> e.servicio.cancelar(71,42));
        e.crear(INICIO,FIN); assertEquals(2,e.reservas.size());
    }
    @ParameterizedTest @ValueSource(strings={"guardar","auditoria","commit"})
    void falloEnCreacionRevierteReservaYAuditoria(String paso) {
        Escenario e = new Escenario(); e.fallo = paso;
        DataAccessException ex = assertThrows(DataAccessException.class, () -> e.crear(INICIO,FIN));
        assertFalse(ex.getMessage().contains("SQL privado")); e.sinReservas(); assertEquals(1,e.rollbacks); assertTrue(e.cerrada);
    }
    @ParameterizedTest @ValueSource(strings={"cancelar","auditoria","commit"})
    void falloEnCancelacionConservaReservaActivaYAuditoriaPrevia(String paso) {
        Escenario e = new Escenario(); e.crear(INICIO,FIN); e.fallo = paso;
        assertThrows(DataAccessException.class, () -> e.servicio.cancelar(71,42));
        assertEquals(EstadoReserva.ACTIVA,e.reservas.get(71).getEstado());
        assertEquals(List.of("RESERVA_CREADA"),e.eventos); assertEquals(1,e.commits); assertEquals(1,e.rollbacks);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","ENCARGADO"})
    void rolesOperativosPuedenCrearYConsultarConIdsDeRolVariables(String rol) {
        Escenario e = new Escenario(); e.rol = rol; e.crear(INICIO,FIN);
        assertEquals(1,e.servicio.listar(42).size());
    }
    @ParameterizedTest @ValueSource(strings={"ESTUDIANTE","DESCONOCIDO","INACTIVO","INEXISTENTE"})
    void sinPermisoNoCreaConsultaNiManipulaReservasCambiandoIds(String rol) {
        Escenario e = new Escenario(); e.crear(INICIO,FIN); e.rol = rol;
        assertThrows(BusinessException.class, () -> e.crear(FIN.plusDays(1),FIN.plusDays(1)));
        assertThrows(BusinessException.class, () -> e.servicio.listar(42));
        assertThrows(BusinessException.class, () -> e.servicio.buscarPorId(71,42));
        assertThrows(BusinessException.class, () -> e.servicio.buscarPorId(999,42));
        assertThrows(BusinessException.class, () -> e.servicio.cancelar(71,42));
        assertThrows(BusinessException.class, () -> e.servicio.listarSolicitantes(42));
        assertThrows(BusinessException.class, () -> e.servicio.listarEquipos(42));
        assertEquals(1,e.commits); assertEquals(EstadoReserva.ACTIVA,e.reservas.get(71).getEstado());
    }
    @Test void sinSesionNoSeAbreConexion() {
        Escenario e = new Escenario();
        assertThrows(BusinessException.class, () -> e.servicio.crear(10,7,INICIO,FIN,"",0));
        assertThrows(BusinessException.class, () -> e.servicio.listar(0)); assertEquals(0,e.aperturas);
    }
    @ParameterizedTest @EnumSource(value=EstadoEquipo.class,names={"PRESTADO","DANADO","MANTENIMIENTO","BAJA"})
    void reservaInclusoCanceladaNoPermiteSaltarDisponibilidadDelPrestamo(EstadoEquipo estado) {
        Escenario e = new Escenario(); e.equipo = estado; e.crear(INICIO,FIN); e.servicio.cancelar(71,42);
        PrestamoService prestamos = new PrestamoService(e.equipos,new PrestamoRepository() {
            public Optional<Rol> buscarRolDeUsuarioActivo(Connection c,int id) { e.comprobar(c); return Optional.of(new Rol("ENCARGADO")); }
            public int guardar(Connection c,Prestamo p) { throw new AssertionError("No debe crearse un préstamo."); }
        },new DetallePrestamoRepository(),new AuditService(),() -> e.conn,null);
        Prestamo p = new Prestamo(10,42,INICIO,FIN,null,"");
        assertThrows(BusinessException.class, () -> prestamos.crearPrestamo(p,List.of(7)));
        assertNull(p.getIdPrestamo()); assertEquals(estado,e.equipo);
        assertEquals(EstadoReserva.CANCELADA,e.reservas.get(71).getEstado());
    }

    private static Reserva copiar(Reserva r) {
        Reserva n = new Reserva(r.getIdUsuario(),r.getIdEquipo(),r.getFechaInicio(),r.getFechaFin(),r.getEstado(),r.getFechaCreacion(),r.getObservaciones());
        n.setIdReserva(r.getIdReserva()); return n;
    }
    private static class Escenario {
        Map<Integer,Reserva> reservas = new HashMap<>(), tx = new HashMap<>();
        List<String> eventos = new ArrayList<>(), eventosTx = new ArrayList<>();
        String rol = "ENCARGADO", solicitante = "ACTIVO", fallo = "";
        EstadoEquipo equipo = EstadoEquipo.PRESTADO;
        int aperturas,commits,rollbacks,aislamiento; boolean autoCommit = true,cerrada;
        final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a) -> {
            switch(m.getName()) {
                case "setTransactionIsolation" -> aislamiento = (int)a[0];
                case "setAutoCommit" -> {
                    autoCommit = (boolean)a[0]; tx = new HashMap<>(); reservas.forEach((id,r) -> tx.put(id,copiar(r)));
                    eventosTx = new ArrayList<>(eventos);
                }
                case "commit" -> { fallar("commit"); commits++; reservas = new HashMap<>();
                    tx.forEach((id,r) -> reservas.put(id,copiar(r))); eventos = new ArrayList<>(eventosTx); }
                case "rollback" -> { rollbacks++; tx.clear(); eventosTx.clear(); }
                case "close" -> cerrada = true;
                default -> throw new AssertionError(m.getName());
            }
            return null;
        });
        final EquipoRepository equipos = new EquipoRepository() {
            public Optional<Equipo> buscarPorId(int id,Connection c) {
                comprobar(c); if (equipo==null) return Optional.empty();
                Equipo e = new Equipo(); e.setIdEquipo(id); e.setEstado(equipo); return Optional.of(e);
            }
            public void actualizarEstado(int id,EstadoEquipo nuevo,Connection c) { throw new AssertionError("Reservas no cambia estados físicos."); }
        };
        final ReservaService servicio = new ReservaService(new ReservaRepository() {
            public boolean existeSolicitanteActivo(Connection c,int id) { comprobar(c); assertEquals(10,id); return solicitante.equals("ACTIVO"); }
            public boolean existeSolapamiento(Connection c,int equipo,LocalDate inicio,LocalDate fin) {
                comprobar(c); return tx.values().stream().anyMatch(r -> r.getIdEquipo()==equipo && r.getEstado()==EstadoReserva.ACTIVA
                        && !r.getFechaInicio().isAfter(fin) && !r.getFechaFin().isBefore(inicio));
            }
            public int guardar(Connection c,Reserva r) throws SQLException {
                comprobar(c); int id = 71+tx.size(); Reserva n = copiar(r); n.setIdReserva(id); tx.put(id,n); fallar("guardar"); return id;
            }
            public Optional<Reserva> buscarPorId(Connection c,int id) { comprobar(c); return Optional.ofNullable(tx.get(id)).map(ReservaTransaccionTest::copiar); }
            public Optional<Reserva> buscarParaCancelar(Connection c,int id) { return buscarPorId(c,id); }
            public void cancelar(Connection c,int id) throws SQLException { comprobar(c); tx.get(id).setEstado(EstadoReserva.CANCELADA); fallar("cancelar"); }
            public List<Resumen> listar(Connection c) {
                comprobar(c); return tx.values().stream().map(r -> new Resumen(copiar(r),"Solicitante","EQ-7","Equipo")).toList();
            }
        },equipos,new PermisoService(new RolRepository() {
            public Optional<Rol> buscarDeUsuarioActivo(Connection c,int id) {
                comprobar(c); assertEquals(42,id);
                if (rol.equals("INACTIVO") || rol.equals("INEXISTENTE")) return Optional.empty();
                Rol r = new Rol(rol); r.setIdRol(654); return Optional.of(r);
            }
        }),new AuditService() {
            public void registrar(Integer actor,String accion,String entidad,Integer id,String descripcion,Connection c) {
                comprobar(c); assertEquals(42,actor); assertEquals("Reserva",entidad); assertNotNull(id); eventosTx.add(accion);
                if (fallo.equals("auditoria")) throw new DataAccessException("SQL privado");
            }
        },() -> { aperturas++; return conn; },RELOJ);
        Reserva crear(LocalDate inicio,LocalDate fin) { return servicio.crear(10,7,inicio,fin,"",42); }
        void comprobar(Connection c) { assertSame(conn,c); assertFalse(autoCommit); assertEquals(Connection.TRANSACTION_SERIALIZABLE,aislamiento); }
        void fallar(String paso) throws SQLException { if (fallo.equals(paso)) throw new SQLException("SQL privado"); }
        void sinReservas() { assertTrue(reservas.isEmpty()); assertTrue(eventos.isEmpty()); assertEquals(0,commits); }
    }
}
