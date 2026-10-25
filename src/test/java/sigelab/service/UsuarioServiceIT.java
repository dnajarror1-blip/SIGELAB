package sigelab.service;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Usuario;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioServiceIT {

    private final UsuarioService service = new UsuarioService();

    @Test
    void guardaLaContrasenaCifrada() {
        // Username con la hora para que nunca se repita entre corridas
        String username = "test_service_" + System.currentTimeMillis();

        Usuario u = service.crearUsuario("Ana", "Prueba", username,
                "clave123", "ana@correo.test", 1);

        assertNotNull(u.getIdUsuario());                 // la base le dio un id
        assertNotEquals("clave123", u.getPasswordHash()); // no se guardó en texto plano
        assertTrue(BCrypt.checkpw("clave123", u.getPasswordHash())); // el hash sí corresponde
    }

    @Test
    void noPermiteUsernameRepetido() {
        String username = "test_repetido_" + System.currentTimeMillis();
        service.crearUsuario("Ana", "Prueba", username, "clave123", "a@correo.test", 1);

        // El segundo intento con el mismo username tiene que fallar
        assertThrows(BusinessException.class, () ->
                service.crearUsuario("Otro", "Prueba", username, "clave123", "b@correo.test", 1));
    }

    @Test
    void rechazaContrasenaCorta() {
        // Con solo 3 caracteres no debe dejar crear el usuario
        assertThrows(BusinessException.class, () ->
                service.crearUsuario("Ana", "Prueba", "x_corta", "123", "c@correo.test", 1));
    }

    @Test
    void loginCorrectoDevuelveElUsuario() {
        // Creamos un usuario para poder entrar con él
        String username = "test_login_" + System.currentTimeMillis();
        service.crearUsuario("Luis", "Prueba", username, "clave123", "luis@correo.test", 1);

        // Entramos con los datos correctos
        Usuario u = service.autenticar(username, "clave123");

        assertEquals(username, u.getUsername());
    }

    @Test
    void loginConContrasenaIncorrectaFalla() {
        String username = "test_mala_" + System.currentTimeMillis();
        service.crearUsuario("Luis", "Prueba", username, "clave123", "luis2@correo.test", 1);

        // Contraseña equivocada: tiene que lanzar error
        assertThrows(BusinessException.class, () ->
                service.autenticar(username, "otra_clave"));
    }

    @Test
    void loginConUsuarioInexistenteFalla() {
        // Este username no existe en la base
        assertThrows(BusinessException.class, () ->
                service.autenticar("no_existe_xyz_123", "clave123"));
    }

    // ---------- Tests nuevos ----------

    @Test
    void listarIncluyeAlUsuarioRecienCreado() {
        String username = "test_lista_" + System.currentTimeMillis();
        service.crearUsuario("Eva", "Prueba", username, "clave123", "eva@correo.test", 2);

        List<Usuario> todos = service.listarUsuarios();

        // Debe haber alguien en la lista con ese username
        assertTrue(todos.stream().anyMatch(u -> u.getUsername().equals(username)));
    }

    @Test
    void desactivarYReactivarUsuario() {
        String username = "test_estado_" + System.currentTimeMillis();
        Usuario u = service.crearUsuario("Eva", "Prueba", username, "clave123", "eva2@correo.test", 2);
        int id = u.getIdUsuario();

        // Lo desactiva "otro" usuario (id 0 solo para la prueba): ya no puede entrar
        service.cambiarEstado(id, EstadoUsuario.INACTIVO, 0);
        assertThrows(BusinessException.class, () -> service.autenticar(username, "clave123"));

        // Lo reactivamos: vuelve a poder entrar
        service.cambiarEstado(id, EstadoUsuario.ACTIVO, 0);
        assertEquals(username, service.autenticar(username, "clave123").getUsername());
    }

    @Test
    void noPermiteDesactivarseASiMismo() {
        String username = "test_propio_" + System.currentTimeMillis();
        Usuario u = service.crearUsuario("Eva", "Prueba", username, "clave123", "eva3@correo.test", 1);
        int id = u.getIdUsuario();

        // Quien lo hace y a quien se lo hacen es el mismo id: debe fallar
        assertThrows(BusinessException.class, () ->
                service.cambiarEstado(id, EstadoUsuario.INACTIVO, id));
    }

    @Test
    void cambiarEstadoDeUsuarioInexistenteFalla() {
        // El id -1 no existe en la base
        assertThrows(BusinessException.class, () ->
                service.cambiarEstado(-1, EstadoUsuario.ACTIVO, 0));
    }
}