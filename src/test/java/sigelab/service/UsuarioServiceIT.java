package sigelab.service;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import sigelab.exception.BusinessException;
import sigelab.model.Usuario;

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
}