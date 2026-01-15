package com.example.ControlNGR.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.service.EmpleadoService;
import com.example.ControlNGR.service.JWTUtil;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {
    
    @Autowired
    private EmpleadoService empleadoService;
    
    @Autowired
    private JWTUtil jwtUtil;
    
    @GetMapping
    public ResponseEntity<List<Empleado>> getAllEmpleados() {
        List<Empleado> empleados = empleadoService.findAll();
        return ResponseEntity.ok(empleados);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmpleadoById(@PathVariable("id") Integer id) {
        Optional<Empleado> empleado = empleadoService.findById(id);
        if (empleado.isPresent()) {
            return ResponseEntity.ok(empleado.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Empleado no encontrado"));
    }
    
    @PostMapping
    public ResponseEntity<?> createEmpleado(@RequestBody Empleado empleado) {
        try {
            if (empleadoService.existsByDni(empleado.getDni())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ya existe un empleado con ese DNI"));
            }
            if (empleado.getPassword() == null || empleado.getPassword().trim().isEmpty()) {
                empleado.setPassword("password");
            }
            Empleado empleadoGuardado = empleadoService.save(empleado);
            return ResponseEntity.status(HttpStatus.CREATED).body(empleadoGuardado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear empleado: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmpleado(@PathVariable("id") Integer id, @RequestBody Empleado empleado) {
        try {
            if (!empleadoService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Empleado no encontrado"));
            }
            empleado.setId(id);
            Empleado empleadoActualizado = empleadoService.save(empleado);
            return ResponseEntity.ok(empleadoActualizado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar empleado"));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmpleado(@PathVariable("id") Integer id) {
        try {
            Optional<Empleado> empleadoOpt = empleadoService.findById(id);
            if (!empleadoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Empleado no encontrado"));
            }
            Empleado empleado = empleadoOpt.get();
            empleado.setActivo(false);
            empleado.setUsuarioActivo(false);
            empleadoService.save(empleado);
            return ResponseEntity.ok(Map.of("message", "Empleado desactivado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar empleado"));
        }
    }
    
    @GetMapping("/buscar")
    public ResponseEntity<List<Empleado>> buscarEmpleados(@RequestParam("nombre") String nombre) {
        List<Empleado> empleados = empleadoService.findAll().stream()
                .filter(e -> e.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .toList();
        return ResponseEntity.ok(empleados);
    }
    
    @GetMapping("/rol/{rol}")
    public ResponseEntity<List<Empleado>> getEmpleadosByRol(@PathVariable("rol") String rol) {
        List<Empleado> empleados = empleadoService.findByRol(rol);
        return ResponseEntity.ok(empleados);
    }
    
    @GetMapping("/mi-perfil")
    public ResponseEntity<?> getMiPerfil(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);
                Optional<Empleado> empleadoOpt = empleadoService.findByUsername(username);
                if (empleadoOpt.isPresent()) {
                    return ResponseEntity.ok(empleadoOpt.get());
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener perfil"));
        }
    }
    
    // NUEVOS ENDPOINTS PARA ACTUALIZAR PERFIL
    
    @PutMapping("/actualizar-perfil/{id}")
    public ResponseEntity<?> actualizarPerfil(@PathVariable("id") Integer id, @RequestBody Map<String, Object> datos) {
        try {
            boolean actualizado = empleadoService.actualizarPerfil(id, datos);
            if (actualizado) {
                return ResponseEntity.ok(Map.of(
                    "message", "Perfil actualizado correctamente",
                    "success", true
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Empleado no encontrado", "success", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar perfil: " + e.getMessage(), "success", false));
        }
    }
    
    @PostMapping("/cambiar-password-admin/{id}")
    public ResponseEntity<?> cambiarPasswordAdmin(@PathVariable("id") Integer id, @RequestBody Map<String, String> request) {
        try {
            String passwordNueva = request.get("passwordNueva");
            if (passwordNueva == null || passwordNueva.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La nueva contraseña es requerida", "success", false));
            }
            
            if (passwordNueva.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La contraseña debe tener al menos 6 caracteres", "success", false));
            }
            
            boolean cambiado = empleadoService.cambiarPasswordAdmin(id, passwordNueva);
            if (cambiado) {
                return ResponseEntity.ok(Map.of(
                    "message", "Contraseña cambiada exitosamente",
                    "success", true
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Empleado no encontrado", "success", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar contraseña: " + e.getMessage(), "success", false));
        }
    }
    
    @PutMapping("/actualizar-email/{id}")
    public ResponseEntity<?> actualizarEmail(@PathVariable("id") Integer id, @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El email es requerido", "success", false));
            }
            
            // Validar formato de email simple
            if (!email.contains("@") || !email.contains(".")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Formato de email inválido", "success", false));
            }
            
            // Verificar si el email ya existe
            Optional<Empleado> empleadoExistente = empleadoService.findByEmail(email);
            if (empleadoExistente.isPresent() && !empleadoExistente.get().getId().equals(id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El email ya está registrado por otro usuario", "success", false));
            }
            
            Map<String, Object> datos = new java.util.HashMap<>();
            datos.put("email", email);
            
            boolean actualizado = empleadoService.actualizarPerfil(id, datos);
            if (actualizado) {
                return ResponseEntity.ok(Map.of(
                    "message", "Email actualizado correctamente",
                    "success", true
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Empleado no encontrado", "success", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar email: " + e.getMessage(), "success", false));
        }
    }
    
    // Exportar empleados
    @GetMapping("/exportar")
    public ResponseEntity<?> exportarEmpleados() {
        try {
            List<Map<String, Object>> datos = empleadoService.exportarEmpleados();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                        "titulo", "Reporte de Empleados",
                        "fecha_generacion", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        "total_registros", datos.size(),
                        "datos", datos
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al exportar empleados: " + e.getMessage()));
        }
    }
}