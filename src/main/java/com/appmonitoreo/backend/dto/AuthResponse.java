package com.appmonitoreo.backend.dto;

public class AuthResponse {
    private String token;
    private String correo;
    private String nombre;
    private String rol;

    public AuthResponse(String token, String correo, String nombre, String rol) {
        this.token = token;
        this.correo = correo;
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getToken() { return token; }
    public String getCorreo() { return correo; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
}
