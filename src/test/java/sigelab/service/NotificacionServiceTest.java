package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.enums.TipoNotificacion;
import sigelab.exception.BusinessException;
import sigelab.model.Notificacion;
import sigelab.repository.NotificacionRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificacionServiceTest {

    // Repositorio de mentira: guarda en una lista y no toca la base de datos
    private static class RepositorioFalso extends NotificacionRepository {
        final List<Notificacion> guardadas = new ArrayList<>();
        boolean resultadoMarcarComoLeida = true;

        @Override
        public void guardar(Notificacion n) {
            n.setIdNotificacion(guardadas.size() + 1);
            guardadas.add(n);
        }

        @Override
        public boolean marcarComoLeida(int idNotificacion, int idUsuario) {
            return resultadoMarcarComoLeida;
        }
    }

    private final RepositorioFalso repo = new RepositorioFalso();
    private final NotificacionService service = new NotificacionService(repo);

    @Test
    void creaNotificacionValidaYLaDejaSinLeer() {
        // El título y el mensaje traen espacios de más y la entidad viene vacía
        Notificacion n = service.crear(1, TipoNotificacion.SISTEMA, "  Hola  ", "  Mensaje  ", "  ", null);

        assertEquals(1, repo.guardadas.size());
        assertEquals("Hola", n.getTitulo());
        assertEquals("Mensaje", n.getMensaje());
        assertFalse(n.isLeida());
        assertNull(n.getEntidad()); // la entidad vacía se guarda como nula
    }

    @Test
    void tituloVacioNoSeGuarda() {
        assertThrows(BusinessException.class,
                () -> service.crear(1, TipoNotificacion.SISTEMA, "   ", "Mensaje", null, null));
        assertEquals(0, repo.guardadas.size());
    }

    @Test
    void tituloMuyLargoNoSeGuarda() {
        String largo = "a".repeat(101); // la base solo permite 100
        assertThrows(BusinessException.class,
                () -> service.crear(1, TipoNotificacion.SISTEMA, largo, "Mensaje", null, null));
        assertEquals(0, repo.guardadas.size());
    }

    @Test
    void mensajeMuyLargoNoSeGuarda() {
        String largo = "a".repeat(301); // la base solo permite 300
        assertThrows(BusinessException.class,
                () -> service.crear(1, TipoNotificacion.SISTEMA, "Título", largo, null, null));
        assertEquals(0, repo.guardadas.size());
    }

    @Test
    void sinTipoNoSeGuarda() {
        assertThrows(BusinessException.class,
                () -> service.crear(1, null, "Título", "Mensaje", null, null));
        assertEquals(0, repo.guardadas.size());
    }

    @Test
    void marcarComoLeidaPropiaFunciona() {
        repo.resultadoMarcarComoLeida = true;
        assertDoesNotThrow(() -> service.marcarComoLeida(1, 1));
    }

    @Test
    void marcarComoLeidaAjenaOInexistenteAvisa() {
        // El repositorio dice que no encontró nada que actualizar
        repo.resultadoMarcarComoLeida = false;
        assertThrows(BusinessException.class, () -> service.marcarComoLeida(99, 1));
    }
}