package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.controller.LoginController;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Rol;
import sigelab.model.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacionServiceTest {
    private final List<String> pasos = new ArrayList<>();
    private final Usuario usuario = new Usuario();
    private boolean passwordValido = true, disponible = true;
    private final UsuarioService usuarios = new UsuarioService() {
        @Override public Usuario autenticar(String username, String password) {
            pasos.add("password");
            if (!passwordValido) throw new BusinessException("Credenciales rechazadas");
            return usuario;
        }
        @Override public Optional<Rol> buscarRolPorId(int id) { return Optional.of(new Rol("ADMIN")); }
        @Override public Usuario revalidarUsuarioActivo(int id) {
            pasos.add("revalidar"); assertEquals(7, id);
            if (!disponible) throw new BusinessException("Usuario desactivado");
            return usuario;
        }
    };
    private final AuditService auditoria = new AuditService() {
        @Override public void registrar(Integer actor, String accion, String entidad, Integer id, String detalle) {
            assertFalse(detalle.contains("secreto")); pasos.add(accion);
        }
    };
    AutenticacionServiceTest() {
        usuario.setIdUsuario(7); usuario.setIdRol(42); usuario.setEstado(EstadoUsuario.ACTIVO);
    }
    private LoginController controller(AutenticacionService.VerificadorFacial facial) {
        return new LoginController(new AutenticacionService(usuarios, new PermisoService(), facial, auditoria));
    }
    @Test void passwordInvalidoNoInvocaFacial() {
        passwordValido = false;
        assertFalse(controller(id -> fail("No abrir cámara")).iniciarSesion("ana", "secreto").exito());
        assertEquals(List.of("password"), pasos);
    }
    @Test void inactivoNoInvocaFacial() {
        usuario.setEstado(EstadoUsuario.INACTIVO);
        assertFalse(controller(id -> fail("No abrir cámara")).iniciarSesion("ana", "secreto").exito());
        assertEquals(List.of("password", "LOGIN_FALLIDO"), pasos);
    }
    @Test void exitoSoloDespuesDeFacialYRevalidacion() {
        var resultado = controller(id -> { assertEquals(7,id); pasos.add("facial"); return true; })
                .iniciarSesion("ana", "secreto");
        assertTrue(resultado.exito());
        assertEquals(List.of("password", "facial", "revalidar", "LOGIN_EXITOSO"), pasos);
        assertEquals("ADMIN", resultado.nombreRol());
    }
    @Test void rostroDistintoDeniegaYAUDITA() {
        var resultado = controller(id -> false).iniciarSesion("ana", "secreto");
        assertFalse(resultado.exito()); assertNull(resultado.usuario()); assertTrue(resultado.menu().isEmpty());
        assertEquals(List.of("password", "LOGIN_FALLIDO"), pasos);
    }
    @Test void falloServicioDeniegaControladamente() {
        var resultado = controller(id -> { throw new FacialAuthClient.ErrorComunicacion("privado", null); })
                .iniciarSesion("ana", "secreto");
        assertFalse(resultado.exito()); assertFalse(resultado.mensaje().contains("privado"));
        assertEquals(List.of("password", "LOGIN_FALLIDO"), pasos);
    }
    @Test void desactivadoDuranteCapturaNoEntra() {
        disponible = false;
        assertFalse(controller(id -> true).iniciarSesion("ana", "secreto").exito());
        assertEquals(List.of("password", "revalidar", "LOGIN_FALLIDO"), pasos);
    }
}
