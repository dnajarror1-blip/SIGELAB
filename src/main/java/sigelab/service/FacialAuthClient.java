package sigelab.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente de comunicación entre SIGELAB Java
 * y el servicio local de reconocimiento facial.
 *
 * IMPORTANTE:
 * - No autentica usuarios.
 * - No valida contraseñas.
 * - No permite iniciar sesión.
 * - No registra ni elimina perfiles.
 * - Solo ejecuta consultas y verificaciones diagnósticas.
 *
 * La autenticación y autorización corresponden
 * al módulo principal de seguridad de SIGELAB.
 */
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

    /**
     * Solicita una verificación facial de diagnóstico.
     *
     * PRECONDICIÓN:
     * El backend de SIGELAB debe validar previamente
     * las credenciales, el estado y la autorización
     * correspondiente al usuario.
     *
     * IMPORTANTE:
     * La respuesta nunca debe utilizarse para
     * autorizar directamente un inicio de sesión.
     */
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