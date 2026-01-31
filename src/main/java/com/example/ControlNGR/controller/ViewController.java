package com.example.ControlNGR.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    /**
     * Redirige todas las rutas que NO sean /api/** a index.html
     * Esto permite que Angular maneje el enrutamiento del frontend
     */
    @GetMapping(value = {
        "/",
        "/login",
        "/dashboard",
        "/empleados",
        "/asistencias",
        "/solicitudes",
        "/horarios",
        "/organigrama",
        "/reportes"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
