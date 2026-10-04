package com.appmonitoreo.backend.service;

import com.appmonitoreo.backend.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementación de RF04 (alerta al ingresar a una zona de riesgo).
 *
 * La detección de proximidad y el registro del evento ya son reales (ver
 * AlertaController /alertas/verificar). El envío de push se hace aquí
 * llamando directo a la API HTTP v1 de Firebase Cloud Messaging (sin el SDK
 * "firebase-admin": ese arrastra un árbol de dependencias enorme —
 * Firestore, gRPC, OpenTelemetry, etc.— innecesario solo para enviar
 * notificaciones). Se autentica como cuenta de servicio de Google firmando
 * un JWT (con la librería jjwt que ya usa el proyecto) y canjeándolo por un
 * access token OAuth2.
 *
 * Si no se configura app.firebase.credentials-path, el envío se omite (se
 * deja log) y la app cliente sigue cubierta por el polling a
 * /alertas/verificar.
 */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile boolean cargado = false;
    private volatile boolean disponible = false;
    private String clientEmail;
    private PrivateKey privateKey;
    private String projectId;

    private volatile String accessTokenCache;
    private volatile Instant expiracionToken = Instant.EPOCH;

    @Override
    public void enviarAlertaZonaRiesgo(Usuario usuario, int incidentesCercanos) {
        if (usuario.getTokenFcm() == null || usuario.getTokenFcm().isBlank()) {
            log.warn("No se puede enviar push a {}: no tiene tokenFcm registrado", usuario.getCorreo());
            return;
        }

        cargarCredencialesSiHaceFalta();

        if (!disponible) {
            log.info("[PUSH OMITIDO: Firebase no configurado] Alerta de zona de riesgo para {} ({} incidentes cercanos)",
                    usuario.getCorreo(), incidentesCercanos);
            return;
        }

        try {
            String accessToken = obtenerAccessToken();
            enviarMensajeFcm(accessToken, usuario, incidentesCercanos);
            log.info("Push enviado a {} ({} incidentes cercanos)", usuario.getCorreo(), incidentesCercanos);
        } catch (Exception ex) {
            log.warn("Fallo al enviar push a {}: {}", usuario.getCorreo(), ex.getMessage());
        }
    }

    private void enviarMensajeFcm(String accessToken, Usuario usuario, int incidentesCercanos) throws IOException, InterruptedException {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", "Zona de riesgo cercana");
        notification.put("body", "Hay " + incidentesCercanos + " incidente(s) de riesgo alto cerca de tu ubicación");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tipo", "ALERTA_ZONA_RIESGO");
        data.put("incidentesCercanos", String.valueOf(incidentesCercanos));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", usuario.getTokenFcm());
        message.put("notification", notification);
        message.put("data", data);

        Map<String, Object> body = Map.of("message", message);
        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("FCM respondió " + response.statusCode() + ": " + response.body());
        }
    }

    private synchronized String obtenerAccessToken() throws Exception {
        if (accessTokenCache != null && Instant.now().isBefore(expiracionToken.minusSeconds(60))) {
            return accessTokenCache;
        }

        Instant ahora = Instant.now();
        String jwt = Jwts.builder()
                .issuer(clientEmail)
                .claim("scope", SCOPE)
                .claim("aud", TOKEN_URL)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(3600)))
                .signWith(privateKey)
                .compact();

        String formBody = "grant_type=" + URLEncoder(GRANT_TYPE) + "&assertion=" + URLEncoder(jwt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("No se pudo obtener el access token de Google: " + response.statusCode() + " " + response.body());
        }

        JsonNode nodo = objectMapper.readTree(response.body());
        accessTokenCache = nodo.get("access_token").asText();
        long expiresIn = nodo.has("expires_in") ? nodo.get("expires_in").asLong() : 3600;
        expiracionToken = Instant.now().plusSeconds(expiresIn);
        return accessTokenCache;
    }

    private static String URLEncoder(String valor) {
        return java.net.URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private synchronized void cargarCredencialesSiHaceFalta() {
        if (cargado) {
            return;
        }
        cargado = true;

        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Firebase no configurado (app.firebase.credentials-path vacío). " +
                    "Las alertas de proximidad seguirán funcionando por polling (/alertas/verificar), " +
                    "pero no se enviarán notificaciones push reales.");
            return;
        }

        try (InputStream in = abrir(credentialsPath)) {
            JsonNode credenciales = objectMapper.readTree(in);
            clientEmail = credenciales.get("client_email").asText();
            projectId = credenciales.get("project_id").asText();
            privateKey = parsearClavePrivada(credenciales.get("private_key").asText());
            disponible = true;
            log.info("Firebase (FCM) configurado correctamente para el proyecto {}", projectId);
        } catch (Exception ex) {
            log.warn("No se pudo cargar las credenciales de Firebase desde '{}': {}. " +
                    "Las notificaciones push quedarán deshabilitadas.", credentialsPath, ex.getMessage());
            disponible = false;
        }
    }

    private PrivateKey parsearClavePrivada(String pem) throws Exception {
        String limpio = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(limpio);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private InputStream abrir(String ruta) throws IOException {
        Path path = Path.of(ruta);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }
        InputStream classpathStream = getClass().getClassLoader().getResourceAsStream(ruta);
        if (classpathStream != null) {
            return classpathStream;
        }
        throw new IOException("No se encontró el archivo de credenciales en '" + ruta +
                "' (ni como ruta de archivo ni en el classpath)");
    }
}
