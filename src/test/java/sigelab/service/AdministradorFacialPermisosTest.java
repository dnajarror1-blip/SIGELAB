package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.repository.RolRepository;
import sigelab.repository.UsuarioRepository;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AdministradorFacialPermisosTest {
    @Test void autorizacionUsaNombreDeRolYEstadoActual() {
        for (String nombre : new String[]{"ADMIN", "ENCARGADO"}) {
            for (EstadoUsuario estado : EstadoUsuario.values()) {
                Usuario actor = new Usuario(); actor.setIdUsuario(42); actor.setIdRol(999); actor.setEstado(estado);
                var service = new UsuarioService(new UsuarioRepository() {
                    @Override public Optional<Usuario> buscarPorId(int id) {
                        assertEquals(42,id); return Optional.of(actor);
                    }
                }, new RolRepository() {
                    @Override public Optional<Rol> buscarPorId(int id) {
                        assertEquals(999,id); return Optional.of(new Rol(nombre));
                    }
                });
                if ("ADMIN".equals(nombre) && estado == EstadoUsuario.ACTIVO) {
                    assertDoesNotThrow(() -> service.exigirAdministradorActivo(42));
                } else {
                    assertThrows(BusinessException.class, () -> service.exigirAdministradorActivo(42));
                }
            }
        }
    }
}
