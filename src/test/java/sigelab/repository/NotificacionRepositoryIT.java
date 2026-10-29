package sigelab.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sigelab.config.DatabaseConfig;
import sigelab.enums.TipoNotificacion;
import sigelab.model.Notificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionRepositoryIT {

    private final NotificacionRepository repository = new NotificacionRepository();

    // Un usuario que ya exista en la base, para poder cumplir la llave foránea
    private int idUsuario;

    @BeforeEach
    void buscarUsuarioExistente() throws Exception {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT TOP 1 id_usuario FROM dbo.Usuario ORDER BY id_usuario");
             ResultSet rs = stmt.executeQuery()) {
            assertTrue(rs.next(), "No hay ningún usuario en la base, crea uno primero");
            idUsuario = rs.getInt(1);
        }
    }

    // Después de cada prueba borramos lo que ella misma creó
    @AfterEach
    void limpiar() throws Exception {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM dbo.Notificacion WHERE titulo LIKE 'test[_]notif[_]%'")) {
            stmt.executeUpdate();
        }
    }

    // Arma una notificación de prueba con título único para encontrarla después
    private Notificacion nueva() {
        String titulo = "test_notif_" + UUID.randomUUID().toString().substring(0, 8);
        return new Notificacion(idUsuario, TipoNotificacion.SISTEMA, titulo,
                "Mensaje de prueba", LocalDateTime.now(), false, "Reserva", 7);
    }

    @Test
    void guardaYLaEncuentraEnElListado() {
        Notificacion n = nueva();
        repository.guardar(n);

        // La base debió darle un id
        assertNotNull(n.getIdNotificacion());

        List<Notificacion> lista = repository.listarPorUsuario(idUsuario);
        Notificacion guardada = lista.stream()
                .filter(x -> x.getIdNotificacion().equals(n.getIdNotificacion()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No apareció la notificación guardada"));

        assertEquals(n.getTitulo(), guardada.getTitulo());
        assertEquals(TipoNotificacion.SISTEMA, guardada.getTipo());
        assertEquals("Reserva", guardada.getEntidad());
        assertEquals(7, guardada.getIdReferencia());
        assertFalse(guardada.isLeida());
    }

    @Test
    void marcarComoLeidaBajaElContadorDeNoLeidas() {
        int antes = repository.contarNoLeidas(idUsuario);

        Notificacion n = nueva();
        repository.guardar(n);
        assertEquals(antes + 1, repository.contarNoLeidas(idUsuario));

        boolean cambio = repository.marcarComoLeida(n.getIdNotificacion(), idUsuario);
        assertTrue(cambio);
        assertEquals(antes, repository.contarNoLeidas(idUsuario));
    }

    @Test
    void noPuedeMarcarLaNotificacionDeOtroUsuario() {
        Notificacion n = nueva();
        repository.guardar(n);

        // Un id de usuario distinto: no debe encontrar nada que actualizar
        boolean cambio = repository.marcarComoLeida(n.getIdNotificacion(), idUsuario + 100000);
        assertFalse(cambio);
    }

    @Test
    void marcarTodasComoLeidasDejaElContadorEnCero() {
        repository.guardar(nueva());
        repository.guardar(nueva());

        int cambiadas = repository.marcarTodasComoLeidas(idUsuario);

        assertTrue(cambiadas >= 2);
        assertEquals(0, repository.contarNoLeidas(idUsuario));
    }
}