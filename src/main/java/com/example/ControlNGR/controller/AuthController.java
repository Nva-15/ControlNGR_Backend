package com.example.ControlNGR.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.ControlNGR.dto.AuthRequest;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.service.EmpleadoService;
import com.example.ControlNGR.service.JWTUtil;
import com.example.ControlNGR.service.EmailService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private JWTUtil jwtUtil;
    
    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            System.out.println("🔐 Login intento para: " + authRequest.getUsername());
            
            Optional<Empleado> empleadoOpt = empleadoService.validarCredenciales(
                authRequest.getUsername(), 
                authRequest.getPassword()
            );

            if (empleadoOpt.isPresent()) {
                Empleado empleado = empleadoOpt.get();
                
                String token = jwtUtil.generateToken(empleado.getUsername(), empleado.getRol());
                
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                
                // Crear mapa del empleado
                Map<String, Object> empleadoData = new HashMap<>();
                empleadoData.put("id", empleado.getId());
                empleadoData.put("dni", empleado.getDni());
                empleadoData.put("nombre", empleado.getNombre());
                empleadoData.put("cargo", empleado.getCargo());
                empleadoData.put("nivel", empleado.getNivel());
                empleadoData.put("rol", empleado.getRol());
                empleadoData.put("username", empleado.getUsername());
                empleadoData.put("email", empleado.getEmail());
                empleadoData.put("foto", empleado.getFoto());
                empleadoData.put("descripcion", empleado.getDescripcion());
                empleadoData.put("hobby", empleado.getHobby());
                empleadoData.put("cumpleanos", empleado.getCumpleanos());
                empleadoData.put("ingreso", empleado.getIngreso());
                
                response.put("empleado", empleadoData);
                
                System.out.println("✅ Login exitoso: " + empleado.getNombre());
                return ResponseEntity.ok(response);
            }
            
            System.out.println("❌ Credenciales inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas o usuario inactivo"));

        } catch (Exception e) {
            System.out.println("❌ Error en login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    // Cambiar contraseña desde perfil (con autenticación)
    @PostMapping("/cambiar-password-perfil")
    public ResponseEntity<?> cambiarPasswordPerfil(@RequestBody Map<String, String> request,
                                                  @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autorizado", "success", false));
            }
            
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token inválido", "success", false));
            }
            
            String username = jwtUtil.extractUsername(token);
            
            String passwordActual = request.get("passwordActual");
            String passwordNueva = request.get("passwordNueva");
            String confirmarPassword = request.get("confirmarPassword");
            
            // Si no viene confirmarPassword, usar passwordNueva como confirmación
            if (confirmarPassword == null) {
                confirmarPassword = passwordNueva;
            }
            
            if (passwordActual == null || passwordNueva == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Contraseña actual y nueva son requeridas", "success", false));
            }
            
            if (!passwordNueva.equals(confirmarPassword)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Las contraseñas no coinciden", "success", false));
            }
            
            if (passwordNueva.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La contraseña debe tener al menos 6 caracteres", "success", false));
            }
            
            Optional<Empleado> empleadoOpt = empleadoService.findByUsername(username);
            if (!empleadoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado", "success", false));
            }
            
            Empleado empleado = empleadoOpt.get();
            boolean cambiado = empleadoService.cambiarPassword(empleado.getId(), passwordActual, passwordNueva);
            
            if (cambiado) {
                // Notificar por email
                if (empleado.getEmail() != null) {
                    try {
                        emailService.enviarNotificacionCambioPassword(empleado.getEmail(), empleado.getNombre());
                    } catch (Exception e) {
                        System.err.println("⚠️ Error enviando email de notificación: " + e.getMessage());
                        // No fallar la operación si el email falla
                    }
                }
                
                return ResponseEntity.ok(Map.of(
                    "message", "Contraseña cambiada exitosamente",
                    "success", true
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Contraseña actual incorrecta", "success", false));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error cambiando contraseña: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar contraseña: " + e.getMessage(), "success", false));
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
            System.err.println("❌ Error verificando token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar token: " + e.getMessage()));
        }
    }
}