package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.exception.DatabaseConfigurationException;
import sigelab.model.Usuario;
import sigelab.service.FacialAdminService;
import sigelab.service.FacialAuthClient;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FacialAdminControllerTest {
    private final Usuario actor = new Usuario();
    private final List<String> llamadas = new ArrayList<>();
    private RuntimeException fallo;
    private final FacialAdminService servicio = new FacialAdminService() {
        private void registrar(Usuario actor, int target, String operacion) {
            assertEquals(42, actor.getIdUsuario().intValue());
            assertEquals(91, target);
            llamadas.add(operacion);
            if (fallo != null) throw fallo;
        }
        @Override public FacialAuthClient.EstadoPerfil consultarEstado(Usuario actor, int target) {
            registrar(actor, target, "status");
            return new FacialAuthClient.EstadoPerfil(target, true, true, true, true);
        }
        @Override public FacialAuthClient.Registro registrarRostro(Usuario actor, int target) {
            registrar(actor, target, "registrar"); return new FacialAuthClient.Registro(target, true);
        }
        @Override public FacialAuthClient.Registro reemplazarRostro(Usuario actor, int target) {
            registrar(actor, target, "reemplazar"); return new FacialAuthClient.Registro(target, true);
        }
        @Override public FacialAuthClient.Eliminacion eliminarRostro(Usuario actor, int target) {
            registrar(actor, target, "eliminar"); return new FacialAuthClient.Eliminacion(target, true);
        }
    };
    FacialAdminControllerTest() { actor.setIdUsuario(42); }
    private FacialAdminController controller() { return new FacialAdminController(actor, servicio); }

    @Test void operacionesExitosasDevuelvenDatosTipados() {
        var controller = controller();
        assertTrue(controller.consultarEstado(91).dato().activo());
        assertTrue(controller.registrarRostro(91).dato().registered());
        assertTrue(controller.reemplazarRostro(91).dato().registered());
        assertTrue(controller.eliminarRostro(91).dato().deleted());
        assertEquals(List.of("status", "registrar", "reemplazar", "eliminar"), llamadas);
    }
    @Test void actorSeFijaDesdeLaSesionYNoDeUnParametroDeAccion() {
        var controller = controller();
        actor.setIdUsuario(777); // Mutar el modelo original no sustituye la identidad de sesión capturada.
        assertTrue(controller.consultarEstado(91).exito());
    }
    @Test void permisoDenegadoSeComunicaSinDatos() {
        fallo = new BusinessException("Se requiere un administrador activo.");
        var resultado = controller().consultarEstado(91);
        assertFalse(resultado.exito()); assertNull(resultado.dato());
        assertEquals("Se requiere un administrador activo.", resultado.mensaje());
    }
    @Test void comunicacionFallidaORespuestaInvalidaTieneMensajeControlado() {
        fallo = new FacialAuthClient.ErrorComunicacion("detalle privado", new IllegalArgumentException("secreto"));
        var controller = controller();
        for (var resultado : List.of(controller.consultarEstado(91), controller.registrarRostro(91),
                controller.reemplazarRostro(91), controller.eliminarRostro(91))) {
            assertFalse(resultado.exito()); assertNull(resultado.dato());
            assertFalse(resultado.mensaje().contains("privado"));
            assertFalse(resultado.mensaje().contains("secreto"));
            assertFalse(resultado.mensaje().contains("todavía no está conectado"));
        }
    }
    @Test void falloDeDatosNoRevelaExcepcionTecnica() {
        fallo = new DataAccessException("Detalle SQL privado");
        var resultado = controller().eliminarRostro(91);
        assertFalse(resultado.exito()); assertFalse(resultado.mensaje().contains("privado"));
        assertNull(resultado.dato());
    }
    @Test void configuracionDeDatosIncompletaSeControla() {
        fallo = new DatabaseConfigurationException("Credencial privada");
        var resultado = controller().consultarEstado(91);
        assertFalse(resultado.exito()); assertFalse(resultado.mensaje().contains("privada"));
    }
}
