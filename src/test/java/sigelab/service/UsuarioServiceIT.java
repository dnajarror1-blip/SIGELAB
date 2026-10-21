package sigelab.service;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import sigelab.model.Usuario;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioServiceIT {

    private final UsuarioService service = new UsuarioService();

    @Test
    void guardaLaContrasenaCifrada() throws Exception {
        // Username con la hora para que nunca se repita entre corridas
        String username = "test_service_" + System.currentTimeMillis();

        Usuario u = service.crearUsuario("Ana", "Prueba", username,
                "clave123", "ana@correo.test", 1);

        assertNotNull(u.getIdUsuario());                 // la base le dio un id
        assertNotEquals("clave123", u.getPasswordHash()); // no se guardó en texto plano
        assertTrue(BCrypt.checkpw("clave123", u.getPasswordHash())); // el hash sí corresponde
    }

    @Test
    void noPermiteUsernameRepetido() throws Exception {
        String username = "test_repetido_" + System.currentTimeMillis();
        service.crearUsuario("Ana", "Prueba", username, "clave123", "a@correo.test", 1);

        // El segundo intento con el mismo username tiene que fallar
        assertThrows(IllegalArgumentException.class, () ->
                service.crearUsuario("Otro", "Prueba", username, "clave123", "b@correo.test", 1));
    }

    @Test
    void rechazaContrasenaCorta() {
        // Con solo 3 caracteres no debe dejar crear el usuario
        assertThrows(IllegalArgumentException.class, () ->
                service.crearUsuario("Ana", "Prueba", "x_corta", "123", "c@correo.test", 1));
    }
}