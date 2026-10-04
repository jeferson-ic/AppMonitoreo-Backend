package com.appmonitoreo.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_sistema")
public class EventoSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer idEvento;

    @Column(length = 10, nullable = false)
    private String tipo;

    @Column(length = 50)
    private String componente;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "fecha_evento")
    private LocalDateTime fechaEvento = LocalDateTime.now();

    public Integer getIdEvento() { return idEvento; }
    public void setIdEvento(Integer idEvento) { this.idEvento = idEvento; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getComponente() { return componente; }
    public void setComponente(String componente) { this.componente = componente; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }
}
