package com.example.ControlNGR.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.ControlNGR.dto.AuthRequest;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.service.EmpleadoService;
import com.example.ControlNGR.service.JWTUtil;

@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = "*")  <-- ELIMINADO PARA EVITAR CONFLICTO CORS
public class AuthController {

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private JWTUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            System.out.println("Login intento para: " + authRequest.getUsername());
            
            // Validar credenciales usando empleados
            Optional<Empleado> empleadoOpt = empleadoService.validarCredenciales(
                authRequest.getUsername(), 
                authRequest.getPassword()
            );

            if (empleadoOpt.isPresent()) {
                Empleado empleado = empleadoOpt.get();
                
                // Generar token JWT
                String token = jwtUtil.generateToken(empleado.getUsername(), empleado.getRol());
                
                // Preparar respuesta con datos del empleado
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("empleado", Map.of(
                    "id", empleado.getId(),
                    "dni", empleado.getDni(),
                    "nombre", empleado.getNombre(),
                    "cargo", empleado.getCargo(),
                    "nivel", empleado.getNivel(),
                    "rol", empleado.getRol(),
                    "username", empleado.getUsername(),
                    "foto", empleado.getFoto()
                ));
                
                System.out.println("Login exitoso: " + empleado.getNombre());
                return ResponseEntity.ok(response);
            }
            
            System.out.println("Credenciales inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas o usuario inactivo"));

        } catch (Exception e) {
            System.out.println("Error en login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String passwordActual = request.get("passwordActual");
            String passwordNueva = request.get("passwordNueva");
            
            if (username == null || passwordActual == null || passwordNueva == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Datos incompletos"));
            }
            
            Optional<Empleado> empleadoOpt = empleadoService.findByUsername(username);
            if (!empleadoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }
            
            Empleado empleado = empleadoOpt.get();
            
            if (!empleadoService.validarCredenciales(username, passwordActual).isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Contraseña actual incorrecta"));
            }
            
            empleado.setPassword(passwordNueva);
            empleadoService.save(empleado);
            
            return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar contraseña"));
        }
    }
    
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String rol = jwtUtil.extractRol(token);
                    
                    return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "username", username,
                        "rol", rol
                    ));
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token inválido"));
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar token"));
        }
    }
}