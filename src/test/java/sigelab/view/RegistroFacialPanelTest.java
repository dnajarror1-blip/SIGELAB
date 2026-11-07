package sigelab.view;

import org.junit.jupiter.api.Test;
import sigelab.service.FacialAuthClient;

import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.*;

class RegistroFacialPanelTest {

    private FacialAuthClient crearClientePrueba() {

        return new FacialAuthClient(
                "http://127.0.0.1:8765",
                "x".repeat(32)
        );
    }

    @Test
    void constructorRecibeUsuarioSeleccionado() {

        RegistroFacialPanel panel =
                new RegistroFacialPanel(
                        crearClientePrueba(),
                        8
                );

        JTextField campo = buscarCampo(panel);

        assertNotNull(campo);

        assertEquals(
                "8",
                campo.getText()
        );
    }

    @Test
    void rechazaIdentificadorInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistroFacialPanel(
                        crearClientePrueba(),
                        0
                )
        );
    }

    @Test
    void permiteCambiarUsuarioSeleccionado() {

        RegistroFacialPanel panel =
                new RegistroFacialPanel(
                        crearClientePrueba(),
                        8
                );

        panel.seleccionarUsuario(12);

        JTextField campo = buscarCampo(panel);

        assertNotNull(campo);

        assertEquals(
                "12",
                campo.getText()
        );
    }

    private JTextField buscarCampo(
            Component componente
    ) {

        if (componente instanceof JTextField campo) {
            return campo;
        }

        if (componente instanceof Container contenedor) {

            for (Component hijo : contenedor.getComponents()) {

                JTextField encontrado = buscarCampo(hijo);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }
}