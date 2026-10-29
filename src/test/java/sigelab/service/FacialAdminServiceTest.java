package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Usuario;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FacialAdminServiceTest {

    // Arma un usuario de prueba rápido (idRol 1 = ADMIN, 2 = ENCARGADO)
    private Usuario crearUsuario(int id, String username, int idRol, EstadoUsuario estado) {
        Usuario u = new Usuario("Prueba", "Uno", username, "hash", "prueba@correo.com", idRol, estado);
        u.setIdUsuario(id);
        return u;
    }

    // UsuarioService de mentira: siempre devuelve lo que le pasemos, sin tocar la base
    private FacialAdminService servicioCon(Usuario enBaseDeDatos) {
        UsuarioService falso = new UsuarioService() {
            @Override
            public Optional<Usuario> buscarPorUsername(String username) {
                return Optional.ofNullable(enBaseDeDatos);
            }
        };
        return new FacialAdminService(falso, new PermisoService());
    }

    @Test
    void adminActivoPasa() {
        Usuario admin = crearUsuario(1, "admin", 1, EstadoUsuario.ACTIVO);
        Usuario resultado = servicioCon(admin).validarAdministrador(admin);
        assertEquals(1, resultado.getIdUsuario());
    }

    @Test
    void encargadoNoPuedeAdministrarRostros() {
        Usuario encargado = crearUsuario(2, "encargado", 2, EstadoUsuario.ACTIVO);
        assertThrows(BusinessException.class,
                () -> servicioCon(encargado).validarAdministrador(encargado));
    }

    @Test
    void adminDesactivadoDespuesDeEntrarYaNoPasa() {
        // En la sesión sigue como ACTIVO, pero en la base ya está INACTIVO
        Usuario enSesion = crearUsuario(1, "admin", 1, EstadoUsuario.ACTIVO);
        Usuario enBase = crearUsuario(1, "admin", 1, EstadoUsuario.INACTIVO);
        assertThrows(BusinessException.class,
                () -> servicioCon(enBase).validarAdministrador(enSesion));
    }

    @Test
    void usuarioQueYaNoExisteNoPasa() {
        Usuario enSesion = crearUsuario(1, "admin", 1, EstadoUsuario.ACTIVO);
        assertThrows(BusinessException.class,
                () -> servicioCon(null).validarAdministrador(enSesion));
    }

    @Test
    void sesionConOtroIdNoPasa() {
        // Mismo username pero distinto id: no es la misma persona
        Usuario enSesion = crearUsuario(5, "admin", 1, EstadoUsuario.ACTIVO);
        Usuario enBase = crearUsuario(1, "admin", 1, EstadoUsuario.ACTIVO);
        assertThrows(BusinessException.class,
                () -> servicioCon(enBase).validarAdministrador(enSesion));
    }

    @Test
    void sinSesionNoPasa() {
        Usuario admin = crearUsuario(1, "admin", 1, EstadoUsuario.ACTIVO);
        assertThrows(BusinessException.class,
                () -> servicioCon(admin).validarAdministrador(null));
    }
}