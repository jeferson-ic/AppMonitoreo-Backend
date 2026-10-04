package com.appmonitoreo.backend.nofuncional;

import com.appmonitoreo.backend.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RendimientoTest extends BaseIntegrationTest {

    private static final Duration LIMITE_VALIDACION = Duration.ofSeconds(5);
    private static final Duration LIMITE_CONSULTA = Duration.ofSeconds(2);

    @Test
    @DisplayName("RNF07: crear y auto-validar un incidente tarda menos de 5 s con 200 registros previos")
    void tiempoValidacionAutomatica() throws Exception {
        String token = tokenDe(crearUsuario("carga@correo.com", "USUARIO"));
        for (int i = 0; i < 200; i++) {
            crearIncidente(token, "Robo", -12.0 - i * 0.001, -77.0);
        }

        long inicio = System.nanoTime();
        crearIncidente(token, "Robo", -12.0, -77.0);
        Duration duracion = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(duracion).isLessThan(LIMITE_VALIDACION);
    }

    @Test
    @DisplayName("Consulta de incidentes cercanos responde en menos de 2 s")
    void tiempoConsultaCercanos() throws Exception {
        String token = tokenDe(crearUsuario("consulta@correo.com", "USUARIO"));
        for (int i = 0; i < 200; i++) {
            crearIncidente(token, "Acoso", -12.0 + i * 0.0005, -77.0);
        }

        long inicio = System.nanoTime();
        mockMvc.perform(get("/incidentes/cercanos")
                        .param("lat", "-12.0").param("lng", "-77.0").param("radioKm", "2")
                        .header("Authorization", token))
                .andExpect(status().isOk());
        Duration duracion = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(duracion).isLessThan(LIMITE_CONSULTA);
    }

    private void crearIncidente(String token, String tipo, double lat, double lng) throws Exception {
        mockMvc.perform(post("/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoIncidente":"%s","descripcion":"carga","latitud":%s,"longitud":%s}
                                """.formatted(tipo, lat, lng)))
                .andExpect(status().isCreated());
    }
}
