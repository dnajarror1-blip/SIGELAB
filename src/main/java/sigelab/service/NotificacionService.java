package sigelab.service;

import sigelab.enums.TipoNotificacion;
import sigelab.exception.BusinessException;
import sigelab.model.Notificacion;
import sigelab.repository.NotificacionRepository;

import java.time.LocalDateTime;
import java.util.List;

public class NotificacionService {

    // Los largos máximos son los mismos que tiene la tabla en la base de datos
    private static final int MAX_TITULO = 100;
    private static final int MAX_MENSAJE = 300;
    private static final int MAX_ENTIDAD = 30;

    private final NotificacionRepository repository;

    // Constructor normal, el que usa la app
    public NotificacionService() {
        this(new NotificacionRepository());
    }

    // Este otro nos sirve para pruebas
    public NotificacionService(NotificacionRepository repository) {
        this.repository = repository;
    }

    // Crea una notificación para un usuario. Las demás partes del sistema
    // (reservas, préstamos, etc.) llaman a este método cuando algo pasa.
    public Notificacion crear(int idUsuario, TipoNotificacion tipo, String titulo,
                              String mensaje, String entidad, Integer idReferencia) {
        if (tipo == null) {
            throw new BusinessException("El tipo de notificación es obligatorio");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new BusinessException("El título es obligatorio");
        }
        if (titulo.trim().length() > MAX_TITULO) {
            throw new BusinessException("El título no puede pasar de " + MAX_TITULO + " caracteres");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new BusinessException("El mensaje es obligatorio");
        }
        if (mensaje.trim().length() > MAX_MENSAJE) {
            throw new BusinessException("El mensaje no puede pasar de " + MAX_MENSAJE + " caracteres");
        }

        // La entidad es opcional: si viene vacía la guardamos como nula
        String entidadLimpia = (entidad == null || entidad.isBlank()) ? null : entidad.trim();
        if (entidadLimpia != null && entidadLimpia.length() > MAX_ENTIDAD) {
            throw new BusinessException("La entidad no puede pasar de " + MAX_ENTIDAD + " caracteres");
        }

        // Toda notificación nace sin leer
        Notificacion nueva = new Notificacion(idUsuario, tipo, titulo.trim(), mensaje.trim(),
                LocalDateTime.now(), false, entidadLimpia, idReferencia);
        repository.guardar(nueva);
        return nueva;
    }

    // Las notificaciones de un usuario, las más nuevas primero
    public List<Notificacion> listar(int idUsuario) {
        return repository.listarPorUsuario(idUsuario);
    }

    // Cuántas tiene sin leer, para el numerito del menú
    public int contarNoLeidas(int idUsuario) {
        return repository.contarNoLeidas(idUsuario);
    }

    // Marca una como leída. Si no existe o es de otro usuario, avisamos.
    public void marcarComoLeida(int idNotificacion, int idUsuario) {
        boolean cambio = repository.marcarComoLeida(idNotificacion, idUsuario);
        if (!cambio) {
            throw new BusinessException("La notificación no existe o no es tuya");
        }
    }

    // Marca todas las pendientes de ese usuario como leídas. Devuelve cuántas fueron.
    public int marcarTodasComoLeidas(int idUsuario) {
        return repository.marcarTodasComoLeidas(idUsuario);
    }
}