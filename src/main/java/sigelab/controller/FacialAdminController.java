package sigelab.controller;

import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.model.Usuario;
import sigelab.service.FacialAdminService;

public class FacialAdminController {

    // Lo que le devolvemos a la pantalla: si salió bien y qué mensaje mostrar
    public record Resultado(boolean exito, String mensaje) {
    }

    private final FacialAdminService facialAdminService;

    public FacialAdminController() {
        this(new FacialAdminService());
    }

    public FacialAdminController(FacialAdminService facialAdminService) {
        this.facialAdminService = facialAdminService;
    }

    // La pantalla lo usa para decidir si muestra o activa los botones faciales.
    // Ojo: esto solo es comodidad visual, la seguridad real se revisa en cada acción.
    public boolean puedeAdministrarRostros(Usuario quienPide) {
        try {
            facialAdminService.validarAdministrador(quienPide);
            return true;
        } catch (BusinessException | DataAccessException e) {
            return false;
        }
    }

    public Resultado registrarRostro(Usuario quienPide, int idUsuarioObjetivo) {
        try {
            // Primero siempre confirmamos que quien pide es un admin activo
            facialAdminService.validarAdministrador(quienPide);

            // Pendiente: aquí va la llamada a Python cuando #1 y #8 confirmen el contrato
            return new Resultado(false, "El servicio facial todavía no está conectado.");
        } catch (BusinessException e) {
            return new Resultado(false, e.getMessage());
        } catch (DataAccessException e) {
            return new Resultado(false, "No se pudo consultar la base de datos.");
        }
    }

    public Resultado eliminarRostro(Usuario quienPide, int idUsuarioObjetivo) {
        try {
            facialAdminService.validarAdministrador(quienPide);

            // Pendiente: aquí va el DELETE a Python
            return new Resultado(false, "El servicio facial todavía no está conectado.");
        } catch (BusinessException e) {
            return new Resultado(false, e.getMessage());
        } catch (DataAccessException e) {
            return new Resultado(false, "No se pudo consultar la base de datos.");
        }
    }
}