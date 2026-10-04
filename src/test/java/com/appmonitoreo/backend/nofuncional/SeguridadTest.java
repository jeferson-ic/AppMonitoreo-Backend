package com.appmonitoreo.backend.nofuncional;

import com.appmonitoreo.backend.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Endpoint protegido sin token devuelve 401")
    void sinToken() throws Exception {
        mockMvc.perform(get("/incidentes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token manipulado o inválido devuelve 401")
    void tokenInvalido() throws Exception {
        String token = tokenDe(crearUsuario("valido@correo.com", "USUARIO"));
        String manipulado = token.substring(0, token.length() - 4) + "abcd";

        mockMvc.perform(get("/incidentes").header("Authorization", manipulado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Usuario sin rol ADMIN no accede a rutas de administración (403)")
    void usuarioNoAccedeAAdmin() throws Exception {
        String token = tokenDe(crearUsuario("normal@correo.com", "USUARIO"));

        mockMvc.perform(get("/admin/metricas").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador accede a métricas")
    void adminAccedeAMetricas() throws Exception {
        String token = tokenDe(crearUsuario("jefe@correo.com", "ADMIN"));

        mockMvc.perform(get("/admin/metricas").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios").value(1));
    }

    @Test
    @DisplayName("Health check es público y responde UP")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Otros endpoints de actuator no están expuestos")
    void actuatorRestringido() throws Exception {
        String token = tokenDe(crearUsuario("curioso@correo.com", "ADMIN"));

        mockMvc.perform(get("/actuator/env").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Las respuestas incluyen cabeceras de seguridad")
    void cabecerasDeSeguridad() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    @DisplayName("CORS permite el origen configurado y rechaza otros")
    void cors() throws Exception {
        mockMvc.perform(options("/incidentes")
                        .header("Origin", "https://panel.ejemplo.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://panel.ejemplo.com"));

        mockMvc.perform(options("/incidentes")
                        .header("Origin", "https://sitio-malicioso.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Las respuestas no exponen contraseña, token FCM ni datos de contacto del reportero")
    void noExponeDatosSensibles() throws Exception {
        var u = crearUsuario("privado@correo.com", "USUARIO");
        u.setTokenFcm("token-fcm-secreto");
        usuarioRepository.save(u);
        String token = tokenDe(u);

        mockMvc.perform(post("/incidentes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipoIncidente":"Robo","descripcion":"x","latitud":-12.0,"longitud":-77.0}
                        """));

        mockMvc.perform(get("/incidentes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("contrasena"))))
                .andExpect(content().string(not(containsString("token-fcm-secreto"))))
                .andExpect(content().string(not(containsString("privado@correo.com"))))
                .andExpect(content().string(not(containsString("987654321"))));
    }

    @Test
    @DisplayName("Errores internos no revelan detalles de la implementación")
    void errorSinDetalles() throws Exception {
        String token = tokenDe(crearUsuario("x@correo.com", "USUARIO"));

        mockMvc.perform(get("/alertas/verificar").param("lat", "abc").param("lng", "1")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Exception"))));
    }
}
