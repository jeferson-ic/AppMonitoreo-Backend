package com.appmonitoreo.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "validacion_reportes")
public class ValidacionReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_validacion")
    private Integer idValidacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_incidente")
    private Incidente incidente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(length = 20)
    private String accion;

    @Column(name = "fecha_accion")
    private LocalDateTime fechaAccion = LocalDateTime.now();

    @Column(length = 255)
    private String observacion;

    public Integer getIdValidacion() { return idValidacion; }
    public void setIdValidacion(Integer idValidacion) { this.idValidacion = idValidacion; }

    public Incidente getIncidente() { return incidente; }
    public void setIncidente(Incidente incidente) { this.incidente = incidente; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public LocalDateTime getFechaAccion() { return fechaAccion; }
    public void setFechaAccion(LocalDateTime fechaAccion) { this.fechaAccion = fechaAccion; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
