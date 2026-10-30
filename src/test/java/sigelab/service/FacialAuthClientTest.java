package sigelab.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FacialAuthClientTest {

    // Clave ficticia exclusiva para las pruebas.
    private static final String CLAVE_PRUEBA =
            "A".repeat(32);

    private HttpServer servidor;
    private FacialAuthClient cliente;

    private AtomicReference<String> ultimoMetodo;
    private AtomicReference<String> ultimaRuta;
    private AtomicReference<String> ultimaClave;
    private AtomicReference<String> ultimoCuerpo;

    @BeforeEach
    void iniciarServidorSimulado() throws IOException {

        ultimoMetodo = new AtomicReference<>();
        ultimaRuta = new AtomicReference<>();
        ultimaClave = new AtomicReference<>();
        ultimoCuerpo = new AtomicReference<>();

        // Puerto aleatorio disponible en la computadora.
        servidor = HttpServer.create(
                new InetSocketAddress(
                        "127.0.0.1",
                        0
                ),
                0
        );

        servidor.createContext(
                "/",
                this::procesarSolicitud
        );

        servidor.start();

        int puerto = servidor.getAddress().getPort();

        String urlServicio =
                "http://127.0.0.1:" + puerto;

        cliente = new FacialAuthClient(
                urlServicio,
                CLAVE_PRUEBA
        );
    }

    @AfterEach
    void detenerServidorSimulado() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    /**
     * Simula las respuestas del servicio Python.
     *
     * No accede a SQL Server.
     * No utiliza la cámara.
     * No ejecuta reconocimiento facial real.
     */
    private void procesarSolicitud(
            HttpExchange intercambio
    ) throws IOException {

        String metodo =
                intercambio.getRequestMethod();

        String ruta =
                intercambio.getRequestURI().getPath();

        String clave =
                intercambio.getRequestHeaders()
                        .getFirst("X-Facial-Api-Key");

        String cuerpo = new String(
                intercambio.getRequestBody()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        ultimoMetodo.set(metodo);
        ultimaRuta.set(ruta);
        ultimaClave.set(clave);
        ultimoCuerpo.set(cuerpo);

        int codigoHttp;
        String respuestaJson;

        if ("/health".equals(ruta)) {

            codigoHttp = 200;

            respuestaJson = """
                    {
                        "status": "ok",
                        "service": "SIGELAB FaceID TEST"
                    }
                    """;

        } else if (
                "/face/status/7".equals(ruta)
        ) {

            codigoHttp = 200;

            respuestaJson = """
                    {
                        "userId": 7,
                        "usuarioActivo": true,
                        "perfilRegistrado": true
                    }
                    """;

        } else if (
                "/face/verify".equals(ruta)
        ) {

            codigoHttp = 200;

            respuestaJson = """
                    {
                        "result": "DIAGNOSTIC_ONLY",
                        "verified": false,
                        "authenticationAllowed": false
                    }
                    """;

        } else {

            codigoHttp = 404;

            respuestaJson = """
                    {
                        "detail": "Recurso no encontrado"
                    }
                    """;
        }

        byte[] datosRespuesta =
                respuestaJson.getBytes(
                        StandardCharsets.UTF_8
                );

        intercambio.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8"
        );

        intercambio.sendResponseHeaders(
                codigoHttp,
                datosRespuesta.length
        );

        try (
                OutputStream salida =
                        intercambio.getResponseBody()
        ) {
            salida.write(datosRespuesta);
        }
    }

    @Test
    void testConsultarSalud() {

        FacialAuthClient.RespuestaFacial respuesta =
                cliente.consultarSalud();

        assertEquals(
                200,
                respuesta.codigoHttp()
        );

        assertEquals(
                "GET",
                ultimoMetodo.get()
        );

        assertEquals(
                "/health",
                ultimaRuta.get()
        );

        assertNull(
                ultimaClave.get()
        );

        assertTrue(
                respuesta.cuerpo().contains(
                        "SIGELAB FaceID TEST"
                )
        );

        System.out.println(
                "OK: Consulta de salud funcionando"
        );
    }

    @Test
    void testConsultarEstadoFacial() {

        FacialAuthClient.RespuestaFacial respuesta =
                cliente.consultarEstado(7);

        assertEquals(
                200,
                respuesta.codigoHttp()
        );

        assertEquals(
                "GET",
                ultimoMetodo.get()
        );

        assertEquals(
                "/face/status/7",
                ultimaRuta.get()
        );

        assertEquals(
                CLAVE_PRUEBA,
                ultimaClave.get()
        );

        System.out.println(
                "OK: Consulta de estado protegida"
        );
    }

    @Test
    void testVerificacionDiagnostica() {

        FacialAuthClient.RespuestaFacial respuesta =
                cliente.verificarDiagnostico(7);

        assertEquals(
                200,
                respuesta.codigoHttp()
        );

        assertEquals(
                "POST",
                ultimoMetodo.get()
        );

        assertEquals(
                "/face/verify",
                ultimaRuta.get()
        );

        assertEquals(
                "{\"userId\":7}",
                ultimoCuerpo.get()
        );

        assertEquals(
                CLAVE_PRUEBA,
                ultimaClave.get()
        );

        assertTrue(
                respuesta.cuerpo().contains(
                        "\"authenticationAllowed\": false"
                )
        );

        assertTrue(
                respuesta.cuerpo().contains(
                        "\"DIAGNOSTIC_ONLY\""
                )
        );

        System.out.println(
                "OK: Verificacion diagnostica HTTP correcta"
        );
    }

    @Test
    void testUsuarioInexistente() {

        FacialAuthClient.RespuestaFacial respuesta =
                cliente.consultarEstado(999);

        assertEquals(
                404,
                respuesta.codigoHttp()
        );

        assertTrue(
                respuesta.cuerpo().contains(
                        "Recurso no encontrado"
                )
        );

        System.out.println(
                "OK: Error HTTP 404 identificado"
        );
    }

    @Test
    void testIdentificadoresInvalidos() {

        assertThrows(
                IllegalArgumentException.class,
                () -> cliente.consultarEstado(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cliente.consultarEstado(-1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cliente.verificarDiagnostico(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cliente.verificarDiagnostico(-10)
        );

        System.out.println(
                "OK: Identificadores invalidos rechazados"
        );
    }

    @Test
    void testConfiguracionInsegura() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new FacialAuthClient(
                        "http://ejemplo.com:8765",
                        CLAVE_PRUEBA
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FacialAuthClient(
                        "http://127.0.0.1:8765",
                        "clave-corta"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FacialAuthClient(
                        "http://127.0.0.1:8765",
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FacialAuthClient(
                        "http://127.0.0.1:8765",
                        CLAVE_PRUEBA + "\nInvalido"
                )
        );

        System.out.println(
                "OK: Configuraciones inseguras rechazadas"
        );
    }
}