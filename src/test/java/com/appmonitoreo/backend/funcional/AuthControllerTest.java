package com.appmonitoreo.backend.funcional;

import com.appmonitoreo.backend.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends BaseIntegrationTest {

    private static String registro(String correo, String contrasena) {
        return """
                {"nombre":"Ana Pérez","correo":"%s","contrasena":"%s","telefono":"987654321"}
                """.formatted(correo, contrasena);
    }

    private static String login(String correo, String contrasena) {
        return """
                {"correo":"%s","contrasena":"%s"}
                """.formatted(correo, contrasena);
    }

    @Test
    @DisplayName("Registro válido crea el usuario con la contraseña cifrada")
    void registroValido() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("ana@correo.com", CONTRASENA)))
                .andExpect(status().isCreated());

        var guardado = usuarioRepository.findByCorreo("ana@correo.com").orElseThrow();
        assertThat(guardado.getContrasena()).isNotEqualTo(CONTRASENA).startsWith("$2");
        assertThat(guardado.getRol()).isEqualTo("USUARIO");
    }

    @Test
    @DisplayName("Registro con correo existente devuelve 409")
    void registroDuplicado() throws Exception {
        crearUsuario("repetido@correo.com", "USUARIO");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("repetido@correo.com", CONTRASENA)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Registro con contraseña débil devuelve 400")
    void registroContrasenaDebil() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("debil@correo.com", "123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.contrasena").exists());
    }

    @Test
    @DisplayName("Login correcto devuelve un JWT y el rol")
    void loginCorrecto() throws Exception {
        crearUsuario("login@correo.com", "USUARIO");

        mockMvc.perform(post("/auth/login")
                        .with(desdeIp("10.0.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("login@correo.com", CONTRASENA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("USUARIO"));
    }

    @Test
    @DisplayName("Login con contraseña incorrecta devuelve 401")
    void loginIncorrecto() throws Exception {
        crearUsuario("fallo@correo.com", "USUARIO");

        mockMvc.perform(post("/auth/login")
                        .with(desdeIp("10.0.0.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("fallo@correo.com", "OtraClave99")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Usuario inactivo no puede iniciar sesión")
    void loginUsuarioInactivo() throws Exception {
        var u = crearUsuario("inactivo@correo.com", "USUARIO");
        u.setEstado("INACTIVO");
        usuarioRepository.save(u);

        mockMvc.perform(post("/auth/login")
                        .with(desdeIp("10.0.0.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("inactivo@correo.com", CONTRASENA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tras 5 intentos fallidos la IP queda bloqueada (429)")
    void bloqueoPorFuerzaBruta() throws Exception {
        crearUsuario("victima@correo.com", "USUARIO");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .with(desdeIp("10.0.0.99"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(login("victima@correo.com", "Incorrecta" + i)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                        .with(desdeIp("10.0.0.99"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("victima@correo.com", CONTRASENA)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("JSON mal formado devuelve 400 y no 500")
    void jsonMalFormado() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{correo:"))
                .andExpect(status().isBadRequest());
    }
}
