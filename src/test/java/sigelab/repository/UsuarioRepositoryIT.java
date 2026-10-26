package sigelab.repository;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Usuario;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRepositoryIT {

    private final UsuarioRepository repo = new UsuarioRepository();

    // Arma un usuario de prueba con un username que nunca se repite
    private Usuario nuevoDePrueba(String prefijo) {
        Usuario u = new Usuario();
        u.setNombre("Prueba");
        u.setApellido("Repo");
        u.setUsername(prefijo + System.currentTimeMillis());
        u.setPasswordHash("hash_de_prueba");
        u.setCorreo("repo@correo.test");
        u.setIdRol(1);
        u.setEstado(EstadoUsuario.ACTIVO);
        return u;
    }

    @Test
    void encuentraUsuarioExistente() {
        // Primero creamos el usuario, así el test no depende de nada puesto a mano
        Usuario guardado = repo.guardar(nuevoDePrueba("test_repo_busca_"));

        Optional<Usuario> encontrado = repo.buscarPorUsername(guardado.getUsername());

        assertTrue(encontrado.isPresent());
        assertEquals(EstadoUsuario.ACTIVO, encontrado.get().getEstado());
    }

    @Test
    void usuarioInexistenteDevuelveVacio() {
        // Este username no existe en la base
        Optional<Usuario> encontrado = repo.buscarPorUsername("no_existe_xyz");

        assertTrue(encontrado.isEmpty());
    }

    @Test
    void guardaUsuarioYAsignaId() {
        Usuario guardado = repo.guardar(nuevoDePrueba("test_repo_guarda_"));

        // La base le puso un id
        assertNotNull(guardado.getIdUsuario());
        // Y ahora sí se puede encontrar
        assertTrue(repo.buscarPorUsername(guardado.getUsername()).isPresent());
    }
}