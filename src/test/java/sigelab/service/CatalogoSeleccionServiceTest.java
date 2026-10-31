package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.controller.CatalogoSeleccionController;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoSeleccionServiceTest {
    @Test void solicitantesRespetaActivosConRolRealSinRestringirAEstudiantes() {
        Rol estudiante = new Rol("ESTUDIANTE"); estudiante.setIdRol(10);
        Rol encargado = new Rol("ENCARGADO"); encargado.setIdRol(20);
        Usuario ana = usuario(71,"Ana",10,EstadoUsuario.ACTIVO);
        Usuario juan = usuario(72,"Juan",20,EstadoUsuario.ACTIVO);
        Usuario inactivo = usuario(73,"Inactivo",10,EstadoUsuario.INACTIVO);
        Usuario sinRol = usuario(74,"Sin rol",30,EstadoUsuario.ACTIVO);
        UsuarioService usuarios = new UsuarioService() {
            public List<Rol> listarRoles() { return List.of(estudiante,encargado); }
            public List<Usuario> listarUsuarios() { return List.of(ana,juan,inactivo,sinRol); }
        };
        CatalogoSeleccionService servicio = new CatalogoSeleccionService(null,usuarios,null,null);
        var lista = servicio.listarSolicitantesActivos();
        assertEquals(List.of(71,72),lista.stream().map(CatalogoSeleccionService.Solicitante::idUsuario).toList());
        assertTrue(lista.getFirst().nombre().contains("estudiante")); assertTrue(lista.get(1).nombre().contains("encargado"));
        assertFalse(lista.toString().contains("hash privado"));
    }
    @Test void consultaExigePermisoAntesDeCargarElCatalogo() {
        AuditService permisos = new AuditService() {
            public void validarConsulta(Integer actor,Modulo modulo) { assertEquals(42,actor); assertEquals(Modulo.MANTENIMIENTO,modulo); throw new BusinessException("Denegado"); }
        };
        CatalogoSeleccionController controller = new CatalogoSeleccionController(42,Modulo.MANTENIMIENTO,null,permisos);
        assertThrows(BusinessException.class, () -> controller.listarEquipos(false));
    }
    @Test void equipoDisponibleReutilizaConsultaExistenteYResuelveTipoYLaboratorio() {
        Equipo equipo = new Equipo(); equipo.setIdEquipo(71); equipo.setCodigoInventario("CAM-001");
        equipo.setNombre("ThinkPad"); equipo.setIdTipoEquipo(5); equipo.setIdLaboratorio(8);
        EquipoService equipos = new EquipoService() {
            public List<Equipo> listarDisponibles() { return List.of(equipo); }
            public List<Equipo> listar() { fail("No debe sustituir la consulta de disponibles"); return List.of(); }
        };
        TipoEquipo tipo = new TipoEquipo(); tipo.setIdTipoEquipo(5); tipo.setNombre("Portátil");
        Laboratorio laboratorio = new Laboratorio(); laboratorio.setIdLaboratorio(8); laboratorio.setNombre("Laboratorio A");
        var servicio = new CatalogoSeleccionService(equipos,null,
                new TipoEquipoService() { public List<TipoEquipo> listar() { return List.of(tipo); } },
                new LaboratorioService() { public List<Laboratorio> listar() { return List.of(laboratorio); } });
        var opcion = servicio.listarEquipos(true).getFirst();
        assertEquals("Portátil",opcion.tipo()); assertEquals("Laboratorio A",opcion.laboratorio());
        assertEquals(71,opcion.equipo().getIdEquipo()); assertEquals("CAM-001 · ThinkPad",opcion.toString());
    }
    private Usuario usuario(int id,String nombre,int rol,EstadoUsuario estado) {
        Usuario u = new Usuario(); u.setIdUsuario(id); u.setNombre(nombre); u.setApellido("Prueba"); u.setUsername(nombre); u.setIdRol(rol); u.setEstado(estado); u.setPasswordHash("hash privado"); return u;
    }
}
