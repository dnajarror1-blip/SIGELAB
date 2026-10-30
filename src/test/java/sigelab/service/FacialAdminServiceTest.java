package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.repository.RolRepository;
import sigelab.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FacialAdminServiceTest {
    private final Usuario actor = new Usuario();
    private final Usuario actual = new Usuario();
    private String nombreRol = "ADMIN";
    private int revalidaciones;
    private final FacialFalso facial = new FacialFalso();
    private final UsuarioService usuarios = new UsuarioService(new UsuarioRepository() {
        @Override public Optional<Usuario> buscarPorId(int id) {
            assertEquals(42, id);
            revalidaciones++;
            return Optional.of(actual);
        }
    }, new RolRepository() {
        @Override public Optional<Rol> buscarPorId(int id) {
            assertEquals(805, id); // El ID arbitrario del rol no determina permisos.
            return Optional.of(new Rol(nombreRol));
        }
    });
    private final FacialAdminService servicio = new FacialAdminService(usuarios, facial);

    FacialAdminServiceTest() {
        actor.setIdUsuario(42);
        actual.setIdUsuario(42);
        actual.setIdRol(805);
        actual.setEstado(EstadoUsuario.ACTIVO);
    }

    private static class FacialFalso implements FacialAdminService.OperacionesFaciales {
        final List<String> llamadas = new ArrayList<>();
        boolean registrado, activo = true, existe = true, confirmado = true;
        public FacialAuthClient.EstadoPerfil consultar(int target) {
            llamadas.add("status:" + target);
            return new FacialAuthClient.EstadoPerfil(target, existe, activo, registrado, registrado);
        }
        public FacialAuthClient.Registro registrar(int target, int admin, boolean replace) {
            llamadas.add("enroll:" + target + ":" + admin + ":" + replace);
            return new FacialAuthClient.Registro(target, confirmado);
        }
        public FacialAuthClient.Eliminacion eliminar(int target, int admin) {
            llamadas.add("delete:" + target + ":" + admin);
            return new FacialAuthClient.Eliminacion(target, confirmado);
        }
    }

    @Test void adminActivoConsultaPorIdObjetivo() {
        assertEquals(91, servicio.consultarEstado(actor, 91).userId());
        assertEquals(List.of("status:91"), facial.llamadas);
        assertEquals(1, revalidaciones);
    }
    @Test void adminActivoRegistraSinReemplazar() {
        assertTrue(servicio.registrarRostro(actor, 91).registered());
        assertEquals(List.of("status:91", "enroll:91:42:false"), facial.llamadas);
    }
    @Test void adminActivoReemplazaExplicitamente() {
        assertTrue(servicio.reemplazarRostro(actor, 91).registered());
        assertEquals(List.of("enroll:91:42:true"), facial.llamadas);
    }
    @Test void adminActivoElimina() {
        assertTrue(servicio.eliminarRostro(actor, 91).deleted());
        assertEquals(List.of("delete:91:42"), facial.llamadas);
    }
    private void ejecutar(int operacion) {
        switch (operacion) {
            case 0 -> servicio.consultarEstado(actor, 91);
            case 1 -> servicio.registrarRostro(actor, 91);
            case 2 -> servicio.reemplazarRostro(actor, 91);
            case 3 -> servicio.eliminarRostro(actor, 91);
            default -> throw new AssertionError();
        }
    }
    @Test void encargadoYEstudianteNoPuedenEjecutarNingunaOperacion() {
        for (String rol : List.of("ENCARGADO", "ESTUDIANTE")) {
            nombreRol = rol;
            for (int operacion = 0; operacion < 4; operacion++) {
                int seleccion = operacion;
                assertThrows(BusinessException.class, () -> ejecutar(seleccion));
            }
        }
        assertTrue(facial.llamadas.isEmpty());
    }
    @Test void adminInactivoNoPuedeEjecutarNingunaOperacion() {
        actual.setEstado(EstadoUsuario.INACTIVO);
        for (int operacion = 0; operacion < 4; operacion++) {
            int seleccion = operacion;
            assertThrows(BusinessException.class, () -> ejecutar(seleccion));
        }
        assertTrue(facial.llamadas.isEmpty());
    }
    @Test void revalidaPermisosEnCadaOperacionAunqueLaSesionNoCambie() {
        servicio.consultarEstado(actor, 91);
        actual.setEstado(EstadoUsuario.INACTIVO);
        assertThrows(BusinessException.class, () -> servicio.eliminarRostro(actor, 91));
        assertEquals(2, revalidaciones);
        assertEquals(List.of("status:91"), facial.llamadas);
    }
    @Test void actorNuloOConIdInvalidoSeRechazaSinConsultar() {
        assertThrows(BusinessException.class, () -> servicio.consultarEstado(null, 91));
        for (Integer id : new Integer[]{null, 0, -1}) {
            actor.setIdUsuario(id);
            assertThrows(BusinessException.class, () -> servicio.registrarRostro(actor, 91));
        }
        assertEquals(0, revalidaciones);
        assertTrue(facial.llamadas.isEmpty());
    }
    @Test void objetivoInvalidoSeRechazaSinInvocarFacial() {
        for (int id : new int[]{0, -1}) {
            assertThrows(BusinessException.class, () -> servicio.consultarEstado(actor, id));
            assertThrows(BusinessException.class, () -> servicio.registrarRostro(actor, id));
            assertThrows(BusinessException.class, () -> servicio.reemplazarRostro(actor, id));
            assertThrows(BusinessException.class, () -> servicio.eliminarRostro(actor, id));
        }
        assertTrue(facial.llamadas.isEmpty());
    }
    @Test void perfilExistenteSugiereReemplazarSinRegistrar() {
        facial.registrado = true;
        var error = assertThrows(BusinessException.class, () -> servicio.registrarRostro(actor, 91));
        assertTrue(error.getMessage().contains("Reemplazar rostro"));
        assertEquals(List.of("status:91"), facial.llamadas);
    }
    @Test void objetivoInactivoNoIniciaRegistro() {
        facial.activo = false;
        assertThrows(BusinessException.class, () -> servicio.registrarRostro(actor, 91));
        assertEquals(List.of("status:91"), facial.llamadas);
    }
    @Test void objetivoInexistenteNoIniciaRegistro() {
        facial.existe = false;
        assertThrows(BusinessException.class, () -> servicio.registrarRostro(actor, 91));
        assertEquals(List.of("status:91"), facial.llamadas);
    }
    @Test void mutacionNoConfirmadaNoSePresentaComoExito() {
        facial.confirmado = false;
        assertThrows(BusinessException.class, () -> servicio.registrarRostro(actor, 91));
        assertThrows(BusinessException.class, () -> servicio.reemplazarRostro(actor, 91));
        assertThrows(BusinessException.class, () -> servicio.eliminarRostro(actor, 91));
    }
}
