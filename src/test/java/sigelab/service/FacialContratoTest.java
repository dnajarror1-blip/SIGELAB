package sigelab.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class FacialContratoTest {
    private HttpServer server;
    private FacialAuthClient client;
    private String response;
    private int status = 200, admins;
    private String path, method, body;
    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/", exchange -> {
            path = exchange.getRequestURI().toString(); method = exchange.getRequestMethod();
            body = new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status,bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        client = new FacialAuthClient("http://127.0.0.1:"+server.getAddress().getPort(), "x".repeat(32),
                id -> { assertEquals(42,id); admins++; });
    }
    @AfterEach void close() { server.stop(0); }
    @Test void verificarSoloEnviaIdYExigeResultadoCoherente() {
        response = "{\"userId\":7,\"matched\":true,\"score\":0.9,\"threshold\":0.6,\"livenessPassed\":true}";
        assertTrue(client.verificar(7).matched()); assertEquals("{\"userId\":7}",body);
        assertEquals("/face/verify",path);
    }
    @Test void respuestasInsegurasNoConcedenAcceso() {
        for (String json : new String[]{"{}", "invalid", "{\"matched\":\"true\"}",
                "{\"userId\":8,\"matched\":true,\"score\":0.9,\"threshold\":0.6,\"livenessPassed\":true}",
                "{\"userId\":7,\"matched\":true,\"score\":0.9,\"threshold\":0.6,\"livenessPassed\":false}",
                "{\"userId\":7,\"matched\":true,\"score\":0.2,\"threshold\":0.6,\"livenessPassed\":true}"}) {
            response = json;
            assertThrows(FacialAuthClient.ErrorComunicacion.class, () -> client.verificar(7));
        }
    }
    @Test void sinCoincidenciaSeDevuelveRechazo() {
        response = "{\"userId\":7,\"matched\":false,\"score\":0.2,\"threshold\":0.6,\"livenessPassed\":true}";
        assertFalse(client.verificar(7).matched());
    }
    @Test void perfilAusenteOServicioCaidoDeniegan() {
        response = "{}";
        for (int code : new int[]{404,409,503}) {
            status = code;
            assertThrows(FacialAuthClient.ErrorComunicacion.class, () -> client.verificar(7));
        }
    }
    @Test void statusTipado() {
        response = "{\"id_usuario\":7,\"usuario_existe\":true,\"usuario_activo\":true,\"perfil_registrado\":false,\"perfil_activo\":false}";
        assertFalse(client.consultarPerfil(7).registrado());
    }
    @Test void enrollmentIncluyeActorYReemplazoExplicito() {
        response = "{\"userId\":7,\"registered\":true}";
        assertTrue(client.registrar(7,42,true).registered());
        assertEquals("POST",method); assertEquals("/face/enroll",path);
        assertEquals("{\"userId\":7,\"adminUserId\":42,\"replace\":true}",body); assertEquals(1,admins);
    }
    @Test void eliminarIncluyeActor() {
        response = "{\"userId\":7,\"deleted\":true}";
        assertTrue(client.eliminar(7,42).deleted()); assertEquals("DELETE",method);
        assertEquals("/face/profile/7?adminUserId=42",path); assertEquals(1,admins);
    }
    @Test void actorRechazadoNoEnviaSolicitud() {
        var denied = new FacialAuthClient("http://127.0.0.1:"+server.getAddress().getPort(), "x".repeat(32),
                id -> { throw new sigelab.exception.BusinessException("No ADMIN"); });
        assertThrows(sigelab.exception.BusinessException.class, () -> denied.eliminar(7,42));
        assertNull(path);
    }
}
