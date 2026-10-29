package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.model.Notificacion;
import sigelab.model.Usuario;
import sigelab.service.NotificacionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionControllerTest {

    // Servicio de mentira: anota para quién le preguntaron y, si queremos, lanza un error
    private static class ServicioFalso extends NotificacionService {
        Integer idUsuarioConsultado;
        RuntimeException errorALanzar;

        @Override
        public List<Notificacion> listar(int idUsuario) {
            idUsuarioConsultado = idUsuario;
            if (errorALanzar != null) throw errorALanzar;
            return List.of(new Notificacion());
        }

        @Override
        public int contarNoLeidas(int idUsuario) {
            idUsuarioConsultado = idUsuario;
            return 3;
        }

        @Override
        public void marcarComoLeida(int idNotificacion, int idUsuario) {
            idUsuarioConsultado = idUsuario;
            if (errorALanzar != null) throw errorALanzar;
        }
    }

    private Usuario usuario(Integer id) {
        Usuario u = new Usuario("Prueba", "Uno", "prueba", "hash", "p@correo.com", 2, EstadoUsuario.ACTIVO);
        u.setIdUsuario(id);
        return u;
    }

    @Test
    void sinSesionNoListaNada() {
        var controller = new NotificacionController(null, new ServicioFalso());
        assertFalse(controller.listar().exito());
    }

    @Test
    void usuarioSinIdNoCuentaComoSesion() {
        var controller = new NotificacionController(usuario(null), new ServicioFalso());
        assertFalse(controller.contarNoLeidas().exito());
    }

    @Test
    void listaSoloLasDelUsuarioDeLaSesion() {
        var servicio = new ServicioFalso();
        var respuesta = new NotificacionController(usuario(5), servicio).listar();

        assertTrue(respuesta.exito());
        assertEquals(1, respuesta.dato().size());
        // El id que se consultó fue el de la sesión, no uno que pueda cambiar la pantalla
        assertEquals(5, servicio.idUsuarioConsultado);
    }

    @Test
    void cuentaLasNoLeidas() {
        var respuesta = new NotificacionController(usuario(5), new ServicioFalso()).contarNoLeidas();
        assertTrue(respuesta.exito());
        assertEquals(3, respuesta.dato());
    }

    @Test
    void marcarAjenaMuestraElMensajeDelServicio() {
        var servicio = new ServicioFalso();
        servicio.errorALanzar = new BusinessException("La notificación no existe o no es tuya");

        var respuesta = new NotificacionController(usuario(5), servicio).marcarComoLeida(99);

        assertFalse(respuesta.exito());
        assertEquals("La notificación no existe o no es tuya", respuesta.mensaje());
    }

    @Test
    void siFallaLaBaseNoSeMuestraElErrorTecnico() {
        var servicio = new ServicioFalso();
        servicio.errorALanzar = new DataAccessException("detalle técnico de SQL");

        var respuesta = new NotificacionController(usuario(5), servicio).listar();

        assertFalse(respuesta.exito());
        // El usuario ve un mensaje genérico, no el detalle de SQL
        assertFalse(respuesta.mensaje().contains("SQL"));
    }
}