package com.example.ControlNGR.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para manejar las rutas del SPA (Single Page Application).
 * Redirige todas las rutas que no son API ni recursos estáticos a index.html
 * para que Angular Router pueda manejarlas.
 */
@Controller
public class SpaController {

    /**
     * Redirige las rutas del frontend a index.html para que Angular las maneje.
     * Excluye: /api/**, /img/**, archivos con extensión (js, css, ico, etc.)
     */
    @GetMapping(value = {
        "/dashboard",
        "/login",
        "/asistencia",
        "/horarios",
        "/solicitudes",
        "/organigrama",
        "/empleados",
        "/reportes",
        "/eventos",
        "/eventos/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
