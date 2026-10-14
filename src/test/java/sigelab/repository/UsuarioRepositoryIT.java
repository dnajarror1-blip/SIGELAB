package sigelab.repository;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Usuario;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRepositoryIT {

    private final UsuarioRepository repo = new UsuarioRepository();

    @Test
    void encuentraUsuarioExistente() throws Exception {
        Optional<Usuario> r = repo.buscarPorUsername("prueba_repo");
        assertTrue(r.isPresent());
        assertEquals(EstadoUsuario.ACTIVO, r.get().getEstado());
    }

    @Test
    void usuarioInexistenteDevuelveVacio() throws Exception {
        assertTrue(repo.buscarPorUsername("no_existe_xyz").isEmpty());
    }

    @Test
    void guardaUsuarioYAsignaId() throws Exception {
        UsuarioRepository repo = new UsuarioRepository();

        Usuario nuevo = new Usuario();
        nuevo.setNombre("Nuevo");
        nuevo.setApellido("Guardado");
        nuevo.setUsername("test_guardar_" + System.currentTimeMillis());
        nuevo.setPasswordHash("hash_de_prueba");
        nuevo.setCorreo("guardar@correo.test");
        nuevo.setIdRol(1);
        nuevo.setEstado(EstadoUsuario.ACTIVO);

        Usuario guardado = repo.guardar(nuevo);

        assertNotNull(guardado.getIdUsuario());
        assertTrue(repo.buscarPorUsername(nuevo.getUsername()).isPresent());
    }
}