package com.appmonitoreo.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidentes")
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidente")
    private Integer idIncidente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "tipo_incidente", length = 50)
    private String tipoIncidente;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "nivel_riesgo", length = 20)
    private String nivelRiesgo = "MEDIO";

    @Column(name = "fecha_incidente")
    private LocalDateTime fechaIncidente = LocalDateTime.now();

    @Column(length = 20)
    private String estado = "PENDIENTE";

    public Integer getIdIncidente() { return idIncidente; }
    public void setIdIncidente(Integer idIncidente) { this.idIncidente = idIncidente; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getTipoIncidente() { return tipoIncidente; }
    public void setTipoIncidente(String tipoIncidente) { this.tipoIncidente = tipoIncidente; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }

    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }

    public String getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(String nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }

    public LocalDateTime getFechaIncidente() { return fechaIncidente; }
    public void setFechaIncidente(LocalDateTime fechaIncidente) { this.fechaIncidente = fechaIncidente; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
