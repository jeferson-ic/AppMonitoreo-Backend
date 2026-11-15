package com.appmonitoreo.backend.funcional;

import com.appmonitoreo.backend.BaseIntegrationTest;
import com.appmonitoreo.backend.model.Usuario;
import com.appmonitoreo.backend.repository.AlertaRepository;
import com.appmonitoreo.backend.repository.IncidenteRepository;
import com.appmonitoreo.backend.repository.ValidacionReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private ValidacionReporteRepository validacionReporteRepository;

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

    private String crearReporte(String tokenUsuario, String tipo, double lat, double lng) throws Exception {
        return mockMvc.perform(post("/incidentes")
                        .header("Authorization", tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidente(tipo, lat, lng)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Tres usuarios distintos con reportes similares cercanos validan todo el grupo")
    void validacionAutomatica() throws Exception {
        String token2 = tokenDe(crearUsuario("vecino2@correo.com", "USUARIO"));
        String token3 = tokenDe(crearUsuario("vecino3@correo.com", "USUARIO"));

        mockMvc.perform(post("/incidentes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(incidente("Robo", -12.0464, -77.0428)))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mockMvc.perform(post("/incidentes").header("Authorization", token2)
                        .contentType(MediaType.APPLICATION_JSON).content(incidente("Robo", -12.0465, -77.0428)))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        mockMvc.perform(post("/incidentes").header("Authorization", token3)
                        .contentType(MediaType.APPLICATION_JSON).content(incidente("Robo", -12.0465, -77.0429)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("VALIDADO"));

        assertThat(incidenteRepository.findAll())
                .hasSize(3)
                .allMatch(i -> "VALIDADO".equals(i.getEstado()));
        assertThat(validacionReporteRepository.countByAccion("VALIDADO_AUTO")).isEqualTo(3);
    }

    @Test
    @DisplayName("Un mismo usuario no puede validar su reporte enviándolo varias veces")
    void consensoExigeUsuariosDistintos() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/incidentes").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(incidente("Robo", -12.0464 + i * 0.0001, -77.0428)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        }
        assertThat(incidenteRepository.countByEstado("VALIDADO")).isZero();
    }

    @Test
    @DisplayName("Un tipo de incidente que no está en la lista devuelve 400")
    void tipoNoValido() throws Exception {
        mockMvc.perform(post("/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidente("Incendio", -12.0464, -77.0428)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.tipoIncidente").exists());
        assertThat(incidenteRepository.count()).isZero();
    }

    @Test
    @DisplayName("La fecha del incidente se registra en hora de Perú")
    void fechaEnHoraDePeru() throws Exception {
        crearReporte(token, "Robo", -12.0464, -77.0428);
        LocalDateTime fecha = incidenteRepository.findAll().getFirst().getFechaIncidente();
        LocalDateTime ahoraLima = LocalDateTime.now(ZoneId.of("America/Lima"));
        assertThat(Duration.between(fecha, ahoraLima).abs()).isLessThan(Duration.ofMinutes(1));
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
    @DisplayName("Alerta de proximidad solo considera incidentes de riesgo ALTO validados")
    void alertaProximidad() throws Exception {
        String admin = tokenDe(crearUsuario("admin@correo.com", "ADMIN"));
        crearReporte(token, "Asalto", -12.0500, -77.0500);
        Integer id = incidenteRepository.findAll().getFirst().getIdIncidente();

        // Pendiente: se ve en el mapa, pero todavía no genera alerta.
        mockMvc.perform(get("/alertas/verificar")
                        .param("lat", "-12.0501").param("lng", "-77.0501")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enZonaDeRiesgo").value(false));
        assertThat(alertaRepository.count()).isZero();

        mockMvc.perform(put("/admin/incidentes/{id}/validar", id).header("Authorization", admin))
                .andExpect(status().isOk());
        assertThat(alertaRepository.count()).isEqualTo(1);

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
    @DisplayName("Las zonas de riesgo solo cuentan incidentes validados")
    void zonasRiesgoSoloValidados() throws Exception {
        String admin = tokenDe(crearUsuario("admin@correo.com", "ADMIN"));
        crearReporte(token, "Robo", -12.0600, -77.0600);
        crearReporte(token, "Acoso", -12.3000, -77.3000);
        Integer idValidado = incidenteRepository.findAll().stream()
                .filter(i -> "Robo".equals(i.getTipoIncidente()))
                .findFirst().orElseThrow().getIdIncidente();
        mockMvc.perform(put("/admin/incidentes/{id}/validar", idValidado).header("Authorization", admin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/incidentes/zonas-riesgo").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cantidad").value(1));

        // El mapa sigue mostrando los dos reportes (validado y pendiente).
        mockMvc.perform(get("/incidentes").header("Authorization", token))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("El administrador no puede cambiar el estado de un incidente eliminado")
    void transicionNoPermitida() throws Exception {
        String admin = tokenDe(crearUsuario("admin@correo.com", "ADMIN"));
        crearReporte(token, "Vandalismo", -12.5, -77.5);
        Integer id = incidenteRepository.findAll().getFirst().getIdIncidente();

        mockMvc.perform(put("/admin/incidentes/{id}/eliminar", id).header("Authorization", admin))
                .andExpect(status().isOk());
        mockMvc.perform(put("/admin/incidentes/{id}/validar", id).header("Authorization", admin))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/admin/incidentes/{id}/eliminar", id).header("Authorization", admin))
                .andExpect(status().isConflict());

        assertThat(incidenteRepository.findById(id).orElseThrow().getEstado()).isEqualTo("ELIMINADO");
    }

    @Test
    @DisplayName("Validar dos veces un incidente ALTO no duplica su alerta")
    void alertaSinDuplicados() throws Exception {
        String admin = tokenDe(crearUsuario("admin@correo.com", "ADMIN"));
        crearReporte(token, "Secuestro", -12.7, -77.7);
        Integer id = incidenteRepository.findAll().getFirst().getIdIncidente();

        mockMvc.perform(put("/admin/incidentes/{id}/validar", id).header("Authorization", admin))
                .andExpect(status().isOk());
        mockMvc.perform(put("/admin/incidentes/{id}/rechazar", id).header("Authorization", admin))
                .andExpect(status().isOk());
        mockMvc.perform(put("/admin/incidentes/{id}/validar", id).header("Authorization", admin))
                .andExpect(status().isOk());

        assertThat(alertaRepository.count()).isEqualTo(1);
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
