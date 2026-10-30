package sigelab.service;

import java.io.IOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader;
import java.util.function.IntConsumer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Frontera HTTP local. Java conserva credenciales, permisos y decisión final de acceso. */
public final class FacialAuthClient {

    private static final String CABECERA_API =
            "X-Facial-Api-Key";

    private static final Duration TIMEOUT_CONEXION =
            Duration.ofSeconds(5);

    private static final Duration TIMEOUT_CONSULTA =
            Duration.ofSeconds(10);

    private static final Duration TIMEOUT_VERIFICACION =
            Duration.ofSeconds(180);

    private final HttpClient cliente;
    private final String urlBase;
    private final String claveApi;
    private final IntConsumer autorizarAdmin;

    public static FacialAuthClient desdeEntorno() {
        String url = System.getenv("FACIAL_API_URL");
        return new FacialAuthClient(url == null || url.isBlank() ? "http://127.0.0.1:8765" : url,
                System.getenv("FACIAL_API_KEY"));
    }


    /**
     * Crea un cliente para el servicio facial local.
     *
     * Ejemplo de URL:
     * http://127.0.0.1:8765
     *
     * La clave debe obtenerse de una configuración
     * segura. Nunca debe escribirse directamente
     * en el código fuente.
     */
    public FacialAuthClient(
            String urlServicio,
            String claveApi
    ) {
        this(urlServicio, claveApi, id -> new UsuarioService().exigirAdministradorActivo(id));
    }

    FacialAuthClient(String urlServicio, String claveApi, IntConsumer autorizarAdmin) {
        this.autorizarAdmin = autorizarAdmin;
        if (
                urlServicio == null
                || urlServicio.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "La URL del servicio facial es obligatoria"
            );
        }

        URI uri;

        try {
            uri = URI.create(urlServicio.strip());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "La URL del servicio facial no es válida",
                    error
            );
        }

        // Esta versión solo permite conexiones locales.
        if (
                !"http".equalsIgnoreCase(
                        uri.getScheme()
                )
        ) {
            throw new IllegalArgumentException(
                    "FaceID debe utilizar HTTP local"
            );
        }

        String host = uri.getHost();

        if (
                host == null
                || !(
                        "127.0.0.1".equals(host)
                        || "localhost".equalsIgnoreCase(host)
                )
        ) {
            throw new IllegalArgumentException(
                    "FaceID solo permite conexiones locales"
            );
        }

        String ruta = uri.getRawPath();

        if (
                uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || (
                        ruta != null
                        && !ruta.isEmpty()
                        && !"/".equals(ruta)
                )
        ) {
            throw new IllegalArgumentException(
                    "La URL del servicio facial contiene elementos no permitidos"
            );
        }

        if (
                claveApi == null
                || claveApi.strip().length() < 32
        ) {
            throw new IllegalArgumentException(
                    "La clave de FaceID no está configurada correctamente"
            );
        }

        String claveValidada = claveApi.strip();

        if (
                claveValidada.contains("\r")
                || claveValidada.contains("\n")
        ) {
            throw new IllegalArgumentException(
                    "La clave de FaceID contiene caracteres no permitidos"
            );
        }

        String baseNormalizada = uri.toString();

        if (baseNormalizada.endsWith("/")) {
            baseNormalizada = baseNormalizada.substring(
                    0,
                    baseNormalizada.length() - 1
            );
        }

        this.urlBase = baseNormalizada;
        this.claveApi = claveValidada;

        this.cliente = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT_CONEXION)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Consulta el estado general del servicio.
     *
     * No utiliza la cámara.
     */
    public RespuestaFacial consultarSalud() {

        return enviarSolicitud(
                "GET",
                "/health",
                null,
                false
        );
    }

    /**
     * Consulta si un usuario tiene un perfil facial.
     *
     * No realiza reconocimiento facial.
     */
    public RespuestaFacial consultarEstado(
            int idUsuario
    ) {

        validarIdUsuario(idUsuario);

        return enviarSolicitud(
                "GET",
                "/face/status/" + idUsuario,
                null,
                true
        );
    }

    /** Adaptador para el panel diagnóstico. No decide el acceso de Java. */
    public RespuestaFacial verificarDiagnostico(
            int idUsuario
    ) {

        validarIdUsuario(idUsuario);

        String cuerpoJson =
                "{\"userId\":" + idUsuario + "}";

        return enviarSolicitud(
                "POST",
                "/face/verify",
                cuerpoJson,
                true
        );
    }

    public record Verificacion(int userId, boolean matched, Double score, double threshold,
                               boolean livenessPassed) { }
    public record EstadoPerfil(int userId, boolean usuarioExiste, boolean usuarioActivo,
                               boolean registrado, boolean activo) { }
    public record Registro(int userId, boolean registered) { }
    public record Eliminacion(int userId, boolean deleted) { }

    public Verificacion verificar(int idUsuario) {
        JsonObject json = leerRespuesta(verificarDiagnostico(idUsuario));
        comprobarUsuario(json, "userId", idUsuario);
        boolean matched = booleano(json, "matched");
        boolean live = booleano(json, "livenessPassed");
        double threshold = numero(json, "threshold");
        Double score = json.has("score") && !json.get("score").isJsonNull() ? numero(json, "score") : null;
        if (threshold <= 0 || threshold > 1 || (score != null && (score < -1 || score > 1))
                || (matched && (!live || score == null || score < threshold))) {
            throw respuestaInvalida();
        }
        return new Verificacion(idUsuario, matched, score, threshold, live);
    }

    public EstadoPerfil consultarPerfil(int idUsuario) {
        JsonObject json = leerRespuesta(consultarEstado(idUsuario));
        comprobarUsuario(json, "id_usuario", idUsuario);
        return new EstadoPerfil(idUsuario, booleano(json, "usuario_existe"), booleano(json, "usuario_activo"),
                booleano(json, "perfil_registrado"), booleano(json, "perfil_activo"));
    }

    /** El actor debe proceder del usuario autenticado, nunca de un campo libre de la View. */
    public Registro registrar(int idUsuario, int adminUserId, boolean reemplazar) {
        validarIdUsuario(idUsuario);
        validarIdUsuario(adminUserId);
        autorizarAdmin.accept(adminUserId);
        JsonObject json = leerRespuesta(enviarSolicitud("POST", "/face/enroll",
                "{\"userId\":" + idUsuario + ",\"adminUserId\":" + adminUserId
                        + ",\"replace\":" + reemplazar + "}", true));
        comprobarUsuario(json, "userId", idUsuario);
        return new Registro(idUsuario, booleano(json, "registered"));
    }

    public Eliminacion eliminar(int idUsuario, int adminUserId) {
        validarIdUsuario(idUsuario);
        validarIdUsuario(adminUserId);
        autorizarAdmin.accept(adminUserId);
        JsonObject json = leerRespuesta(enviarSolicitud("DELETE", "/face/profile/" + idUsuario
                + "?adminUserId=" + adminUserId, null, true));
        comprobarUsuario(json, "userId", idUsuario);
        return new Eliminacion(idUsuario, booleano(json, "deleted"));
    }

    private static JsonObject leerRespuesta(RespuestaFacial respuesta) {
        if (respuesta.codigoHttp() != 200 || respuesta.cuerpo() == null) throw respuestaInvalida();
        try (JsonReader reader = new JsonReader(new StringReader(respuesta.cuerpo()))) {
            reader.setStrictness(Strictness.STRICT);
            var elemento = JsonParser.parseReader(reader);
            if (!elemento.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) throw respuestaInvalida();
            return elemento.getAsJsonObject();
        } catch (IOException | RuntimeException error) {
            throw respuestaInvalida();
        }
    }

    private static boolean booleano(JsonObject json, String campo) {
        if (!json.has(campo) || !json.get(campo).isJsonPrimitive()
                || !json.getAsJsonPrimitive(campo).isBoolean()) throw respuestaInvalida();
        return json.get(campo).getAsBoolean();
    }

    private static double numero(JsonObject json, String campo) {
        if (!json.has(campo) || !json.get(campo).isJsonPrimitive()
                || !json.getAsJsonPrimitive(campo).isNumber()) throw respuestaInvalida();
        double valor = json.get(campo).getAsDouble();
        if (!Double.isFinite(valor)) throw respuestaInvalida();
        return valor;
    }

    private static void comprobarUsuario(JsonObject json, String campo, int idUsuario) {
        if (numero(json, campo) != idUsuario) throw respuestaInvalida();
    }

    private static ErrorComunicacion respuestaInvalida() {
        return new ErrorComunicacion("Respuesta facial inválida o servicio no disponible", null);
    }

    /**
     * Ejecuta solicitudes HTTP al servicio Python.
     */
    private RespuestaFacial enviarSolicitud(
            String metodo,
            String ruta,
            String cuerpoJson,
            boolean requiereClave
    ) {

        URI destino = URI.create(
                urlBase + ruta
        );

        Duration tiempoEspera =
                "POST".equals(metodo)
                        ? TIMEOUT_VERIFICACION
                        : TIMEOUT_CONSULTA;

        HttpRequest.Builder solicitud =
                HttpRequest.newBuilder(destino)
                        .timeout(tiempoEspera)
                        .header(
                                "Accept",
                                "application/json"
                        );

        if (requiereClave) {

            solicitud.header(
                    CABECERA_API,
                    claveApi
            );
        }

        if ("POST".equals(metodo)) {

            solicitud.header(
                    "Content-Type",
                    "application/json"
            );

            solicitud.POST(
                    HttpRequest.BodyPublishers.ofString(
                            cuerpoJson,
                            StandardCharsets.UTF_8
                    )
            );

        } else if ("DELETE".equals(metodo)) {
            solicitud.DELETE();
        } else {
            solicitud.GET();
        }

        try {

            HttpResponse<String> respuesta =
                    cliente.send(
                            solicitud.build(),
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            return new RespuestaFacial(
                    respuesta.statusCode(),
                    respuesta.body()
            );

        } catch (InterruptedException error) {

            Thread.currentThread().interrupt();

            throw new ErrorComunicacion(
                    "La comunicación con FaceID fue interrumpida",
                    error
            );

        } catch (IOException error) {

            throw new ErrorComunicacion(
                    "No fue posible comunicarse con el servicio FaceID",
                    error
            );
        }
    }

    /**
     * Valida el identificador recibido.
     */
    private static void validarIdUsuario(
            int idUsuario
    ) {

        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del usuario debe ser positivo"
            );
        }
    }

    /**
     * Resultado HTTP del servicio facial.
     *
     * Un código HTTP 200 NO significa
     * que SIGELAB deba permitir el inicio de sesión.
     */
    public record RespuestaFacial(
            int codigoHttp,
            String cuerpo
    ) {
    }

    /**
     * Error de comunicación con el servicio facial.
     */
    public static final class ErrorComunicacion
            extends RuntimeException {

        public ErrorComunicacion(
                String mensaje,
                Throwable causa
        ) {
            super(mensaje, causa);
        }
    }
}