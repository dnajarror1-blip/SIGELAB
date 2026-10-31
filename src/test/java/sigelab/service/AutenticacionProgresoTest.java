package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.model.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacionProgresoTest {
    @Test void credencialesRechazadasNoAnuncianNiEjecutanFaceId() {
        UsuarioService usuarios = new UsuarioService() {
            public Usuario autenticar(String nombre,String clave) { throw new sigelab.exception.BusinessException("Credenciales inválidas"); }
        };
        var servicio = new AutenticacionService(usuarios,new PermisoService(),id -> { fail("No debe ejecutar FaceID"); return false; },null);
        assertThrows(sigelab.exception.BusinessException.class,
                () -> servicio.autenticar("ana","fixture",() -> fail("No debe anunciar la etapa facial")));
    }

    @Test void notificaRostroDespuesDeCredencialesYAntesDelVerifySinCambiarLaAuditoria() {
        List<String> pasos = new ArrayList<>();
        Usuario usuario = new Usuario(); usuario.setIdUsuario(42); usuario.setIdRol(9); usuario.setEstado(EstadoUsuario.ACTIVO);
        Rol rol = new Rol("ENCARGADO"); rol.setIdRol(9);
        UsuarioService usuarios = new UsuarioService() {
            public Usuario autenticar(String nombre,String clave) { pasos.add("credenciales"); return usuario; }
            public Optional<Rol> buscarRolPorId(int id) { return Optional.of(rol); }
            public Usuario revalidarUsuarioActivo(int id) { pasos.add("revalidar"); return usuario; }
        };
        AuditService auditoria = new AuditService() {
            public void registrarObligatorio(Integer actor,String accion,String entidad,Integer id,String detalle) { pasos.add(accion); }
        };
        var servicio = new AutenticacionService(usuarios,new PermisoService(),id -> { pasos.add("verify"); return true; },auditoria);
        var sesion = servicio.autenticar("ana","fixture",() -> pasos.add("progreso"));
        assertEquals(List.of("credenciales","progreso","verify","revalidar","LOGIN_EXITOSO"),pasos);
        assertEquals("ENCARGADO",sesion.nombreRol());
    }
}
