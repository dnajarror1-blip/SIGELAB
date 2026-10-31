package sigelab.controller;

import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.model.Notificacion;
import sigelab.model.Usuario;
import sigelab.service.NotificacionService;

import java.util.List;

public class NotificacionController {

    // Lo que le devolvemos a la pantalla: si salió bien, un mensaje y el dato (si hay)
    public record Respuesta<T>(boolean exito, String mensaje, T dato) {
    }

    private final Usuario usuarioActual;
    private final NotificacionService service;

    public NotificacionController(Usuario usuarioActual) {
        this(usuarioActual, new NotificacionService());
    }

    // Este constructor nos sirve para pruebas
    public NotificacionController(Usuario usuarioActual, NotificacionService service) {
        this.usuarioActual = usuarioActual;
        this.service = service;
    }

    // Las notificaciones del usuario que tiene la sesión abierta
    public Respuesta<List<Notificacion>> listar() {
        if (!haySesion()) {
            return new Respuesta<>(false, "No hay una sesión iniciada", List.of());
        }
        try {
            return new Respuesta<>(true, "", service.listar(usuarioActual.getIdUsuario()));
        } catch (DataAccessException e) {
            return new Respuesta<>(false, "No se pudieron cargar las notificaciones.", List.of());
        }
    }

    // Cuántas tiene sin leer, para el numerito del menú
    public Respuesta<Integer> contarNoLeidas() {
        if (!haySesion()) {
            return new Respuesta<>(false, "No hay una sesión iniciada", 0);
        }
        try {
            return new Respuesta<>(true, "", service.contarNoLeidas(usuarioActual.getIdUsuario()));
        } catch (DataAccessException e) {
            return new Respuesta<>(false, "No se pudo consultar las notificaciones.", 0);
        }
    }

    public Respuesta<Void> marcarComoLeida(int idNotificacion) {
        if (!haySesion()) {
            return new Respuesta<>(false, "No hay una sesión iniciada", null);
        }
        try {
            service.marcarComoLeida(idNotificacion, usuarioActual.getIdUsuario());
            return new Respuesta<>(true, "Notificación marcada como leída.", null);
        } catch (BusinessException e) {
            return new Respuesta<>(false, e.getMessage(), null);
        } catch (DataAccessException e) {
            return new Respuesta<>(false, "No se pudo actualizar la notificación.", null);
        }
    }

    public Respuesta<Integer> marcarTodasComoLeidas() {
        if (!haySesion()) {
            return new Respuesta<>(false, "No hay una sesión iniciada", 0);
        }
        try {
            int cuantas = service.marcarTodasComoLeidas(usuarioActual.getIdUsuario());
            return new Respuesta<>(true, "Marcadas como leídas: " + cuantas, cuantas);
        } catch (DataAccessException e) {
            return new Respuesta<>(false, "No se pudieron actualizar las notificaciones.", 0);
        }
    }

    // Confirma que de verdad hay un usuario con sesión abierta
    private boolean haySesion() {
        return usuarioActual != null && usuarioActual.getIdUsuario() != null;
    }
}