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
}