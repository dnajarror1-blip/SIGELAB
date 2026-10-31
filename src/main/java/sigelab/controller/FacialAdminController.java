package sigelab.controller;

import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.exception.DatabaseConfigurationException;
import sigelab.model.Usuario;
import sigelab.service.FacialAdminService;
import sigelab.service.FacialAuthClient;

import java.util.Objects;
import java.util.function.Supplier;

/** Frontera de Swing: actor fijado desde la sesión, respuestas tipadas y errores seguros. */
public class FacialAdminController {
    private final FacialAdminService servicio;
    private final Usuario actor;

    public FacialAdminController(Usuario usuarioAutenticado) {
        this(usuarioAutenticado, new FacialAdminService());
    }

    public FacialAdminController(Usuario usuarioAutenticado, FacialAdminService servicio) {
        this.servicio = Objects.requireNonNull(servicio);
        // No conservar roles/estado de una sesión antigua ni un ID mutable desde la View.
        actor = usuarioAutenticado == null ? null : new Usuario();
        if (actor != null) actor.setIdUsuario(usuarioAutenticado.getIdUsuario());
    }

    public record Resultado<T>(boolean exito, String mensaje, T dato) { }

    public Resultado<FacialAuthClient.EstadoPerfil> consultarEstado(int targetUserId) {
        return ejecutar(() -> servicio.consultarEstado(actor, targetUserId), "Estado facial consultado.",
                "No fue posible consultar el perfil facial. Comprueba que el servicio esté disponible.");
    }

    public Resultado<FacialAuthClient.Registro> registrarRostro(int targetUserId) {
        return ejecutar(() -> servicio.registrarRostro(actor, targetUserId), "Rostro registrado correctamente.",
                "No fue posible registrar el rostro. Comprueba el servicio; si ya existe perfil, utiliza Reemplazar rostro.");
    }

    public Resultado<FacialAuthClient.Registro> reemplazarRostro(int targetUserId) {
        return ejecutar(() -> servicio.reemplazarRostro(actor, targetUserId), "Rostro reemplazado correctamente.",
                "No fue posible reemplazar el rostro. Comprueba el servicio y el estado del usuario.");
    }

    public Resultado<FacialAuthClient.Eliminacion> eliminarRostro(int targetUserId) {
        return ejecutar(() -> servicio.eliminarRostro(actor, targetUserId), "Perfil facial eliminado correctamente.",
                "No fue posible eliminar el perfil facial. Comprueba el servicio y si el perfil todavía existe.");
    }

    private <T> Resultado<T> ejecutar(Supplier<T> operacion, String exito, String falloFacial) {
        try {
            return new Resultado<>(true, exito, operacion.get());
        } catch (BusinessException error) {
            return new Resultado<>(false, error.getMessage(), null);
        } catch (FacialAuthClient.ErrorComunicacion error) {
            return new Resultado<>(false, falloFacial, null);
        } catch (DatabaseConfigurationException error) {
            return new Resultado<>(false, "La conexión a la base de datos no está configurada. Contacta al administrador.", null);
        } catch (DataAccessException error) {
            return new Resultado<>(false, "No se pudo verificar el acceso a los datos. Operación facial denegada.", null);
        }
    }
}
