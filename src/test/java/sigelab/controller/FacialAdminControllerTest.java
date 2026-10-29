package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Usuario;
import sigelab.service.FacialAdminService;
import sigelab.service.PermisoService;
import sigelab.service.UsuarioService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacialAdminControllerTest {

    private Usuario crearUsuario(int id, String username, int idRol, EstadoUsuario estado) {
        Usuario u = new Usuario("Prueba", "Uno", username, "hash", "prueba@correo.com", idRol, estado);
        u.setIdUsuario(id);
        return u;
    }

    // Controller armado con un UsuarioService de mentira (sin base de datos)
    private FacialAdminController controllerCon(Usuario enBaseDeDatos) {
        UsuarioService falso = new UsuarioService() {
            @Override
            public Optional<Usuario> buscarPorUsername(String username) {
                return Optional.ofNullable(enBaseDeDatos);
            }
        };
        return new FacialAdminController(new FacialAdminService(falso, new PermisoService()));
    }

    @Test
    void adminActivoPuedeAdministrarRostros() {
        Usuario admin = crearUsuario(1, "admin", 1, EstadoUsuario.ACTIVO);
        assertTrue(controllerCon(admin).puedeAdministrarRostros(admin));
    }

    @Test
    void encargadoNoPuedeAdministrarRostros() {
        Usuario encargado = crearUsuario(2, "encargado", 2, EstadoUsuario.ACTIVO);
        assertFalse(controllerCon(encargado).puedeAdministrarRostros(encargado));
    }

    @Test
    void encargadoRecibeMensajeDePermisoAlRegistrar() {
        Usuario encargado = crearUsuario(2, "encargado", 2, EstadoUsuario.ACTIVO);
        var resultado = controllerCon(encargado).registrarRostro(encargado, 10);
        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("permiso"));
    }

    @Test
    void adminDesactivadoNoPuedeEliminarRostros() {
        Usuario enSesion = crearUsuario(1, "admin", 1, EstadoUsuario.ACTIVO);
        Usuario enBase = crearUsuario(1, "admin", 1, EstadoUsuario.INACTIVO);
        var resultado = controllerCon(enBase).eliminarRostro(enSesion, 10);
        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("activo"));
    }
}