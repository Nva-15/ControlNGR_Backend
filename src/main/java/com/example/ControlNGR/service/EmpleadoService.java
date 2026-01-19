package com.example.ControlNGR.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.repository.EmpleadoRepository;

@Service
@Transactional
public class EmpleadoService {
    
    @Autowired
    private EmpleadoRepository empleadoRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    public Optional<Empleado> validarCredenciales(String username, String password) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findByUsername(username);
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            
            // Verificar que el usuario esté activo en el sistema
            if (Boolean.TRUE.equals(empleado.getActivo()) &&
                Boolean.TRUE.equals(empleado.getUsuarioActivo())) {
                
                // Verificar la contraseña
                if (passwordEncoder.matches(password, empleado.getPassword())) {
                    // PERMITIR TODOS LOS ROLES: admin, supervisor, tecnico, hd, noc
                    // No se filtra por rol específico, todos los roles válidos pueden acceder
                    return Optional.of(empleado);
                }
            }
        }
        return Optional.empty();
    }
    
    // Obtener perfil completo por ID (sin datos sensibles)
    public Map<String, Object> obtenerPerfilCompleto(Integer empleadoId) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            Map<String, Object> perfil = new HashMap<>();
            
            perfil.put("id", empleado.getId());
            perfil.put("dni", empleado.getDni());
            perfil.put("nombre", empleado.getNombre());
            perfil.put("cargo", empleado.getCargo());
            perfil.put("nivel", empleado.getNivel());
            perfil.put("username", empleado.getUsername());
            perfil.put("email", empleado.getEmail());
            perfil.put("rol", empleado.getRol());
            perfil.put("descripcion", empleado.getDescripcion());
            perfil.put("hobby", empleado.getHobby());
            perfil.put("cumpleanos", empleado.getCumpleanos());
            perfil.put("ingreso", empleado.getIngreso());
            perfil.put("foto", empleado.getFoto());
            perfil.put("activo", empleado.getActivo());
            perfil.put("usuarioActivo", empleado.getUsuarioActivo());
            
            return perfil;
        }
        return null;
    }
    
    // Actualizar información personal (solo campos no sensibles)
    public Map<String, Object> actualizarInformacionPersonal(Integer empleadoId, Map<String, Object> datos) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (!empleadoOpt.isPresent()) {
            throw new RuntimeException("Empleado no encontrado");
        }
        
        Empleado empleado = empleadoOpt.get();
        Map<String, Object> cambios = new HashMap<>();
        boolean cambiosRealizados = false;
        
        // Campos permitidos para edición por el usuario
        if (datos.containsKey("nombre") && datos.get("nombre") != null) {
            String nuevoNombre = ((String) datos.get("nombre")).trim();
            if (!nuevoNombre.isEmpty() && !nuevoNombre.equals(empleado.getNombre())) {
                empleado.setNombre(nuevoNombre);
                cambios.put("nombre", nuevoNombre);
                cambiosRealizados = true;
            }
        }
        
        if (datos.containsKey("descripcion")) {
            String nuevaDescripcion = (String) datos.get("descripcion");
            if (nuevaDescripcion != null && !nuevaDescripcion.equals(empleado.getDescripcion())) {
                empleado.setDescripcion(nuevaDescripcion);
                cambios.put("descripcion", nuevaDescripcion);
                cambiosRealizados = true;
            }
        }
        
        if (datos.containsKey("hobby")) {
            String nuevoHobby = (String) datos.get("hobby");
            if (nuevoHobby != null && !nuevoHobby.equals(empleado.getHobby())) {
                empleado.setHobby(nuevoHobby);
                cambios.put("hobby", nuevoHobby);
                cambiosRealizados = true;
            }
        }
        
        if (datos.containsKey("cumpleanos")) {
            try {
                String fechaStr = (String) datos.get("cumpleanos");
                if (fechaStr != null && !fechaStr.isEmpty()) {
                    java.time.LocalDate nuevaFecha = java.time.LocalDate.parse(fechaStr);
                    if (!nuevaFecha.equals(empleado.getCumpleanos())) {
                        empleado.setCumpleanos(nuevaFecha);
                        cambios.put("cumpleanos", fechaStr);
                        cambiosRealizados = true;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Formato de fecha inválido. Use formato YYYY-MM-DD");
            }
        }
        
        if (datos.containsKey("email")) {
            String nuevoEmail = (String) datos.get("email");
            if (nuevoEmail != null && !nuevoEmail.trim().isEmpty()) {
                String emailLimpio = nuevoEmail.trim().toLowerCase();
                
                // Validar formato básico de email
                if (!emailLimpio.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    throw new RuntimeException("Formato de email inválido");
                }
                
                // Verificar si el email ya está en uso por otro usuario
                Optional<Empleado> empleadoExistente = empleadoRepository.findByEmail(emailLimpio);
                if (empleadoExistente.isPresent() && !empleadoExistente.get().getId().equals(empleadoId)) {
                    throw new RuntimeException("El email ya está registrado por otro usuario");
                }
                
                if (!emailLimpio.equals(empleado.getEmail())) {
                    empleado.setEmail(emailLimpio);
                    cambios.put("email", emailLimpio);
                    cambiosRealizados = true;
                }
            }
        }
        
        if (cambiosRealizados) {
            empleadoRepository.save(empleado);
            
            // Enviar notificación por email si hay cambios
            if (empleado.getEmail() != null && !empleado.getEmail().trim().isEmpty()) {
                try {
                    emailService.enviarNotificacionActualizacionPerfil(empleado.getEmail(), empleado.getNombre());
                } catch (Exception e) {
                    System.err.println("⚠️ Error enviando email de notificación: " + e.getMessage());
                }
            }
        }
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("success", cambiosRealizados);
        resultado.put("message", cambiosRealizados ? "Información actualizada correctamente" : "No se realizaron cambios");
        resultado.put("cambios", cambios);
        resultado.put("empleadoId", empleadoId);
        
        return resultado;
    }
    
    // Cambiar contraseña (con validaciones mejoradas)
    public Map<String, Object> cambiarPasswordUsuario(Integer empleadoId, String passwordActual, String passwordNueva) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (!empleadoOpt.isPresent()) {
            throw new RuntimeException("Empleado no encontrado");
        }
        
        Empleado empleado = empleadoOpt.get();
        Map<String, Object> resultado = new HashMap<>();
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(passwordActual, empleado.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        
        // Validar nueva contraseña
        if (passwordNueva == null || passwordNueva.trim().isEmpty()) {
            throw new RuntimeException("La nueva contraseña es requerida");
        }
        
        if (passwordNueva.length() < 6) {
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
        }
        
        // La nueva contraseña no puede ser igual a la actual
        if (passwordEncoder.matches(passwordNueva, empleado.getPassword())) {
            throw new RuntimeException("La nueva contraseña no puede ser igual a la actual");
        }
        
        // Cambiar contraseña
        empleado.setPassword(passwordEncoder.encode(passwordNueva));
        empleadoRepository.save(empleado);
        
        // Enviar notificación por email
        if (empleado.getEmail() != null && !empleado.getEmail().trim().isEmpty()) {
            try {
                emailService.enviarNotificacionCambioPassword(empleado.getEmail(), empleado.getNombre());
            } catch (Exception e) {
                System.err.println("⚠️ Error enviando email de notificación: " + e.getMessage());
            }
        }
        
        resultado.put("success", true);
        resultado.put("message", "Contraseña cambiada exitosamente");
        resultado.put("empleadoId", empleadoId);
        resultado.put("fechaCambio", java.time.LocalDateTime.now());
        
        return resultado;
    }
    
    // Cambiar contraseña
    public boolean cambiarPassword(Integer empleadoId, String passwordActual, String passwordNueva) {
        try {
            Map<String, Object> resultado = cambiarPasswordUsuario(empleadoId, passwordActual, passwordNueva);
            return (Boolean) resultado.get("success");
        } catch (RuntimeException e) {
            return false;
        }
    }
    
    // Cambiar contraseña por admin
    public boolean cambiarPasswordAdmin(Integer empleadoId, String passwordNueva) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            
            if (passwordNueva == null || passwordNueva.trim().isEmpty()) {
                throw new RuntimeException("La nueva contraseña es requerida");
            }
            
            if (passwordNueva.length() < 6) {
                throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
            }
            
            empleado.setPassword(passwordEncoder.encode(passwordNueva));
            empleadoRepository.save(empleado);
            
            if (empleado.getEmail() != null && !empleado.getEmail().trim().isEmpty()) {
                try {
                    emailService.enviarNotificacionCambioPassword(empleado.getEmail(), empleado.getNombre());
                } catch (Exception e) {
                    System.err.println("⚠️ Error enviando email de notificación: " + e.getMessage());
                }
            }
            
            return true;
        }
        return false;
    }
    
    // Actualizar perfil completo
    public boolean actualizarPerfil(Integer empleadoId, Map<String, Object> datos) {
        try {
            Map<String, Object> resultado = actualizarInformacionPersonal(empleadoId, datos);
            return (Boolean) resultado.get("success");
        } catch (RuntimeException e) {
            throw e; // Propagar la excepción
        }
    }
    
    // 
    public List<Empleado> findAll() {
        return empleadoRepository.findAll();
    }

    public Optional<Empleado> findById(Integer id) {
        return empleadoRepository.findById(id);
    }

    public Optional<Empleado> findByDni(String dni) {
        return empleadoRepository.findByDni(dni);
    }

    public Optional<Empleado> findByUsername(String username) {
        return empleadoRepository.findByUsername(username);
    }
    
    public Optional<Empleado> findByEmail(String email) {
        return empleadoRepository.findByEmail(email);
    }
    
    public Empleado save(Empleado empleado) {
        if (empleado.getIdentificador() == null || empleado.getIdentificador().trim().isEmpty()) {
            empleado.setIdentificador(UUID.randomUUID().toString());
        }

        if (empleado.getPassword() != null && !empleado.getPassword().trim().isEmpty()) {
            if (!empleado.getPassword().startsWith("$2")) {
                empleado.setPassword(passwordEncoder.encode(empleado.getPassword()));
            }
        }
        
        if (empleado.getUsername() == null || empleado.getUsername().trim().isEmpty()) {
            empleado.setUsername(empleado.getDni());
        }
        
        if (empleado.getEmail() == null || empleado.getEmail().trim().isEmpty()) {
            String emailGenerado = empleado.getUsername() + "@ngr.com.pe";
            empleado.setEmail(emailGenerado);
        }
        
        if (empleado.getRol() == null || empleado.getRol().trim().isEmpty()) {
            if (empleado.getNivel() != null) {
                switch (empleado.getNivel().toLowerCase()) {
                    case "gerente":
                    case "jefe":
                        empleado.setRol("admin");
                        break;
                    case "supervisor":
                        empleado.setRol("supervisor");
                        break;
                    default:
                        empleado.setRol("tecnico");
                }
            } else {
                empleado.setRol("tecnico");
            }
        }
        
        return empleadoRepository.save(empleado);
    }
    
    public void deleteById(Integer id) {
        empleadoRepository.deleteById(id);
    }

    public boolean existsByDni(String dni) {
        return empleadoRepository.existsByDni(dni);
    }

    public boolean existsByUsername(String username) {
        return empleadoRepository.existsByUsername(username);
    }

    public List<Empleado> findByRol(String rol) {
        return empleadoRepository.findByRol(rol);
    }

    public List<Empleado> findByUsuarioActivo(Boolean activo) {
        return empleadoRepository.findByUsuarioActivo(activo);
    }
    
    public List<Map<String, Object>> exportarEmpleados() {
        return empleadoRepository.findAll().stream()
                .map(e -> {
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("ID", e.getId());
                    datos.put("DNI", e.getDni());
                    datos.put("Nombre", e.getNombre());
                    datos.put("Cargo", e.getCargo());
                    datos.put("Nivel", e.getNivel());
                    datos.put("Email", e.getEmail());
                    datos.put("Usuario", e.getUsername());
                    datos.put("Rol", e.getRol());
                    datos.put("Activo", e.getActivo() != null && e.getActivo() ? "Sí" : "No");
                    datos.put("Usuario_Activo", e.getUsuarioActivo() != null && e.getUsuarioActivo() ? "Sí" : "No");
                    datos.put("Fecha_Ingreso", e.getIngreso());
                    datos.put("Cumpleaños", e.getCumpleanos());
                    datos.put("Descripción", e.getDescripcion());
                    datos.put("Hobby", e.getHobby());
                    return datos;
                })
                .collect(Collectors.toList());
    }
    
    public boolean actualizarEmail(Integer empleadoId, String email) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            
            Optional<Empleado> empleadoExistente = empleadoRepository.findByEmail(email);
            if (empleadoExistente.isPresent() && !empleadoExistente.get().getId().equals(empleadoId)) {
                throw new RuntimeException("El email ya está registrado por otro usuario");
            }
            
            empleado.setEmail(email);
            empleadoRepository.save(empleado);
            return true;
        }
        return false;
    }
}