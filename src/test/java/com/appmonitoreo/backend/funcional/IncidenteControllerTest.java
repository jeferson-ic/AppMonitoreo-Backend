package com.appmonitoreo.backend.funcional;

import com.appmonitoreo.backend.BaseIntegrationTest;
import com.appmonitoreo.backend.model.Usuario;
import com.appmonitoreo.backend.repository.IncidenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidenteControllerTest extends BaseIntegrationTest {

    @Autowired
    private IncidenteRepository incidenteRepository;

    private Usuario usuario;
    private String token;

    @BeforeEach
    void setUp() {
        usuario = crearUsuario("reportero@correo.com", "USUARIO");
        token = tokenDe(usuario);
    }

    private static String incidente(String tipo, double lat, double lng) {
        return """
                {"tipoIncidente":"%s","descripcion":"Reporte de prueba","latitud":%s,"longitud":%s}
                """.formatted(tipo, lat, lng);
    }

    @Test
    @DisplayName("Crear incidente asigna nivel de riesgo según el tipo")
    void crearIncidente() throws Exception {
        mockMvc.perform(post("/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidente("Asalto", -12.0464, -77.0428)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivelRiesgo").value("ALTO"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Coordenadas fuera de rango devuelven 400")
    void coordenadasInvalidas() throws Exception {
        mockMvc.perform(post("/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidente("Robo", 120.0, -77.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.latitud").exists());
    }

    @Test
    @DisplayName("Tres reportes similares cercanos se validan automáticamente")
    void validacionAutomatica() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/incidentes")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(incidente("Robo", -12.0464 + i * 0.0001, -77.0428)))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        }

        mockMvc.perform(post("/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidente("Robo", -12.0465, -77.0429)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("VALIDADO"));
    }

    @Test
    @DisplayName("Mis reportes solo devuelve los incidentes del usuario autenticado")
    void misReportes() throws Exception {
        Usuario otro = crearUsuario("otro@correo.com", "USUARIO");
        mockMvc.perform(post("/incidentes").header("Authorization", tokenDe(otro))
                .contentType(MediaType.APPLICATION_JSON).content(incidente("Robo", -12.1, -77.1)));
        mockMvc.perform(post("/incidentes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(incidente("Acoso", -12.2, -77.2)));

        mockMvc.perform(get("/incidentes/mis-reportes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoIncidente").value("Acoso"));
    }

    @Test
    @DisplayName("Filtrar por nivel de riesgo devuelve solo coincidencias")
    void filtrarPorRiesgo() throws Exception {
        mockMvc.perform(post("/incidentes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(incidente("Secuestro", -12.3, -77.3)));
        mockMvc.perform(post("/incidentes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(incidente("Accidente", -12.4, -77.4)));

        mockMvc.perform(get("/incidentes").param("nivelRiesgo", "ALTO").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoIncidente").value("Secuestro"));
    }

    @Test
    @DisplayName("Alerta de proximidad detecta zona de riesgo ALTO")
    void alertaProximidad() throws Exception {
        mockMvc.perform(post("/incidentes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(incidente("Asalto", -12.0500, -77.0500)));

        mockMvc.perform(get("/alertas/verificar")
                        .param("lat", "-12.0501").param("lng", "-77.0501")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enZonaDeRiesgo").value(true));

        mockMvc.perform(get("/alertas/verificar")
                        .param("lat", "-12.2000").param("lng", "-77.2000")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.enZonaDeRiesgo").value(false));
    }

    @Test
    @DisplayName("Administrador puede validar un incidente pendiente")
    void adminValidaIncidente() throws Exception {
        String admin = tokenDe(crearUsuario("admin@correo.com", "ADMIN"));
        mockMvc.perform(post("/incidentes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(incidente("Vandalismo", -12.5, -77.5)));
        Integer id = incidenteRepository.findAll().getFirst().getIdIncidente();

        mockMvc.perform(put("/admin/incidentes/{id}/validar", id).header("Authorization", admin))
                .andExpect(status().isOk());

        assertThat(incidenteRepository.findById(id).orElseThrow().getEstado()).isEqualTo("VALIDADO");
    }
}
