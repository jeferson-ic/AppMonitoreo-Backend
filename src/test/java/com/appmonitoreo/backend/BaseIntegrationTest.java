package com.appmonitoreo.backend;

import com.appmonitoreo.backend.model.Usuario;
import com.appmonitoreo.backend.repository.UsuarioRepository;
import com.appmonitoreo.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class BaseIntegrationTest {

    protected static final String CONTRASENA = "Clave1234";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtUtil jwtUtil;

    protected Usuario crearUsuario(String correo, String rol) {
        Usuario u = new Usuario();
        u.setNombre("Usuario " + rol);
        u.setCorreo(correo);
        u.setTelefono("987654321");
        u.setContrasena(passwordEncoder.encode(CONTRASENA));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    protected String tokenDe(Usuario u) {
        return "Bearer " + jwtUtil.generateToken(u.getCorreo(), u.getRol());
    }

    protected static RequestPostProcessor desdeIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
