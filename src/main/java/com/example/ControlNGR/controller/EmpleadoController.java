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
    
    // CORREGIDO: ("id")
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
    
    // CORREGIDO: ("id")
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
    
    // CORREGIDO: ("id")
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
    
    // CORREGIDO: ("nombre")
    @GetMapping("/buscar")
    public ResponseEntity<List<Empleado>> buscarEmpleados(@RequestParam("nombre") String nombre) {
        List<Empleado> empleados = empleadoService.findAll().stream()
                .filter(e -> e.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .toList();
        return ResponseEntity.ok(empleados);
    }
    
    // CORREGIDO: ("rol")
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
}