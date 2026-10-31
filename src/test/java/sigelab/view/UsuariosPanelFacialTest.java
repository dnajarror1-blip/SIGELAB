package sigelab.view;

import org.junit.jupiter.api.Test;
import sigelab.controller.FacialAdminController;
import sigelab.controller.UsuarioController;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.DataAccessException;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.service.FacialAdminService;
import sigelab.service.FacialAuthClient;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Swing headless con controladores/servicios en memoria: no abre ventanas, SQL ni HTTP. */
class UsuariosPanelFacialTest {
    private static class ServicioFalso extends FacialAdminService {
        final List<String> llamadas = new ArrayList<>();
        boolean registrado;
        boolean falloConsulta, falloMutacion;
        CountDownLatch iniciada, liberar;
        private void comprobar(Usuario actor, int objetivo, String accion) {
            assertFalse(SwingUtilities.isEventDispatchThread(), "La operación facial no debe ejecutarse en el EDT");
            assertEquals(42, actor.getIdUsuario().intValue());
            assertEquals(91, objetivo);
            llamadas.add(accion);
        }
        @Override public FacialAuthClient.EstadoPerfil consultarEstado(Usuario actor, int objetivo) {
            comprobar(actor, objetivo, "status");
            if (falloConsulta) throw new DataAccessException("Fallo simulado");
            return new FacialAuthClient.EstadoPerfil(objetivo, true, true, registrado, registrado);
        }
        @Override public FacialAuthClient.Registro registrarRostro(Usuario actor, int objetivo) {
            comprobar(actor, objetivo, "registrar");
            if (iniciada != null) iniciada.countDown();
            if (liberar != null) {
                try { assertTrue(liberar.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
            }
            if (falloMutacion) throw new FacialAuthClient.ErrorComunicacion("Error privado", null);
            registrado = true;
            return new FacialAuthClient.Registro(objetivo, true);
        }
        @Override public FacialAuthClient.Registro reemplazarRostro(Usuario actor, int objetivo) {
            comprobar(actor, objetivo, "reemplazar"); registrado = true;
            return new FacialAuthClient.Registro(objetivo, true);
        }
        @Override public FacialAuthClient.Eliminacion eliminarRostro(Usuario actor, int objetivo) {
            comprobar(actor, objetivo, "eliminar"); registrado = false;
            return new FacialAuthClient.Eliminacion(objetivo, true);
        }
    }

    private record Escenario(UsuariosPanel panel, JTable tabla, ServicioFalso servicio, List<String> confirmaciones) { }

    private Usuario usuario(int id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id); usuario.setNombre(nombre); usuario.setApellido("Prueba");
        usuario.setUsername(nombre); usuario.setCorreo("prueba@ejemplo.test");
        usuario.setIdRol(805); usuario.setEstado(EstadoUsuario.ACTIVO);
        return usuario;
    }

    // Debe llamarse en EDT; los controladores de catálogo no acceden a infraestructura.
    private Escenario crear(boolean confirmar, boolean seleccionar) {
        assertTrue(SwingUtilities.isEventDispatchThread());
        Usuario actor = usuario(42, "Admin");
        Rol rol = new Rol("ADMIN"); rol.setIdRol(805);
        UsuarioController usuarios = new UsuarioController(actor) {
            @Override public Respuesta<List<Rol>> listarRoles() { return new Respuesta<>(true, "OK", List.of(rol)); }
            @Override public Respuesta<List<Usuario>> listar() {
                return new Respuesta<>(true, "OK", List.of(usuario(17, "Zeta"), usuario(91, "Ana")));
            }
        };
        var servicio = new ServicioFalso();
        var facial = new FacialAdminController(actor, servicio);
        List<String> confirmaciones = new ArrayList<>();
        UsuariosPanel panel = new UsuariosPanel(usuarios, facial) {
            @Override boolean confirmarOperacionFacial(String mensaje) {
                assertTrue(SwingUtilities.isEventDispatchThread());
                confirmaciones.add(mensaje);
                return confirmar;
            }
        };
        // Campos editables de creación nunca se usan como actor administrativo.
        for (JTextField campo : componentes(panel, JTextField.class)) campo.setText("999");
        actor.setIdUsuario(777); // El controlador ya capturó el ID estable de sesión (42).
        JTable tabla = componentes(panel, JTable.class).getFirst();
        tabla.setAutoCreateRowSorter(true);
        tabla.getRowSorter().toggleSortOrder(1);
        if (seleccionar) tabla.setRowSelectionInterval(0, 0); // Fila ordenada Ana => ID 91, no ID 17.
        return new Escenario(panel, tabla, servicio, confirmaciones);
    }

    private JButton boton(Escenario escenario, String texto) {
        return componentes(escenario.panel(), JButton.class).stream()
                .filter(b -> texto.equals(b.getText())).findFirst().orElseThrow();
    }
    private <T extends Component> List<T> componentes(Container raiz, Class<T> tipo) {
        List<T> encontrados = new ArrayList<>();
        for (Component componente : raiz.getComponents()) {
            if (tipo.isInstance(componente)) encontrados.add(tipo.cast(componente));
            if (componente instanceof Container contenedor) encontrados.addAll(componentes(contenedor, tipo));
        }
        return encontrados;
    }
    private boolean contieneTexto(Escenario escenario, String texto) {
        return componentes(escenario.panel(), JLabel.class).stream().anyMatch(label -> label.getText().contains(texto));
    }
    private CountDownLatch observarFin(Escenario escenario) {
        CountDownLatch fin = new CountDownLatch(1);
        boton(escenario, "Estado facial").addPropertyChangeListener("enabled", evento -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            if (Boolean.TRUE.equals(evento.getNewValue())) fin.countDown();
        });
        return fin;
    }

    @Test void accionesUsanFilaSeleccionadaYActorDeSesionFueraDelEdt() throws Exception {
        for (String accion : List.of("Estado facial", "Registrar rostro", "Reemplazar rostro", "Eliminar rostro")) {
            Escenario[] escenario = new Escenario[1];
            CountDownLatch[] fin = new CountDownLatch[1];
            SwingUtilities.invokeAndWait(() -> {
                escenario[0] = crear(true, true);
                fin[0] = observarFin(escenario[0]);
                boton(escenario[0], accion).doClick();
            });
            assertTrue(fin[0].await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                var caso = escenario[0];
                List<String> esperado = switch (accion) {
                    case "Estado facial" -> List.of("status");
                    case "Registrar rostro" -> List.of("registrar", "status");
                    case "Reemplazar rostro" -> List.of("reemplazar", "status");
                    case "Eliminar rostro" -> List.of("eliminar", "status");
                    default -> throw new AssertionError();
                };
                assertEquals(esperado, caso.servicio().llamadas);
                assertTrue(contieneTexto(caso, "Usuario #91"));
                assertTrue(contieneTexto(caso, caso.servicio().registrado ? "Perfil facial: registrado; activo" : "Perfil facial: no registrado; inactivo"));
                assertEquals(accion.equals("Reemplazar rostro") || accion.equals("Eliminar rostro") ? 1 : 0,
                        caso.confirmaciones().size());
                assertTrue(boton(caso, "Estado facial").isEnabled());
                assertTrue(caso.tabla().isEnabled());
            });
        }
    }
    @Test void sinSeleccionNoPermiteOperacionesFaciales() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var escenario = crear(true, false);
            for (String accion : List.of("Estado facial", "Registrar rostro", "Reemplazar rostro", "Eliminar rostro")) {
                assertFalse(boton(escenario, accion).isEnabled());
                boton(escenario, accion).doClick();
            }
            assertTrue(escenario.servicio().llamadas.isEmpty());
        });
    }
    @Test void cancelarReemplazoOEliminacionNoInvocaServicio() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var escenario = crear(false, true);
            boton(escenario, "Reemplazar rostro").doClick();
            boton(escenario, "Eliminar rostro").doClick();
            assertEquals(2, escenario.confirmaciones().size());
            assertTrue(escenario.servicio().llamadas.isEmpty());
            assertTrue(boton(escenario, "Estado facial").isEnabled());
        });
    }
    @Test void operacionEnCursoBloqueaDobleClicYRecuperaBotones() throws Exception {
        Escenario[] escenario = new Escenario[1];
        CountDownLatch[] fin = new CountDownLatch[1];
        var iniciada = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                escenario[0] = crear(true, true);
                escenario[0].servicio().iniciada = iniciada;
                escenario[0].servicio().liberar = liberar;
                fin[0] = observarFin(escenario[0]);
                boton(escenario[0], "Registrar rostro").doClick();
                boton(escenario[0], "Registrar rostro").doClick();
                for (String accion : List.of("Estado facial", "Registrar rostro", "Reemplazar rostro", "Eliminar rostro")) {
                    assertFalse(boton(escenario[0], accion).isEnabled());
                }
                assertFalse(escenario[0].tabla().isEnabled());
            });
            assertTrue(iniciada.await(5, TimeUnit.SECONDS));
        } finally { liberar.countDown(); }
        assertTrue(fin[0].await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(List.of("registrar", "status"), escenario[0].servicio().llamadas);
            assertTrue(boton(escenario[0], "Registrar rostro").isEnabled());
        });
    }
    @Test void errorDeComunicacionSeMuestraYRehabilitaAcciones() throws Exception {
        Escenario[] escenario = new Escenario[1]; CountDownLatch[] fin = new CountDownLatch[1];
        SwingUtilities.invokeAndWait(() -> {
            escenario[0] = crear(true, true); escenario[0].servicio().falloMutacion = true;
            fin[0] = observarFin(escenario[0]); boton(escenario[0], "Registrar rostro").doClick();
        });
        assertTrue(fin[0].await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(contieneTexto(escenario[0], "No fue posible registrar"));
            assertFalse(contieneTexto(escenario[0], "Error privado"));
            assertTrue(boton(escenario[0], "Registrar rostro").isEnabled());
        });
    }
    @Test void falloAlRefrescarNoOcultaMutacionConfirmada() throws Exception {
        Escenario[] escenario = new Escenario[1]; CountDownLatch[] fin = new CountDownLatch[1];
        SwingUtilities.invokeAndWait(() -> {
            escenario[0] = crear(true, true); escenario[0].servicio().falloConsulta = true;
            fin[0] = observarFin(escenario[0]); boton(escenario[0], "Registrar rostro").doClick();
        });
        assertTrue(fin[0].await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(contieneTexto(escenario[0], "Rostro registrado correctamente"));
            assertTrue(contieneTexto(escenario[0], "estado no disponible"));
        });
    }
}
