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
            if (Boolean.TRUE.equals(empleado.getActivo()) &&
                Boolean.TRUE.equals(empleado.getUsuarioActivo())) {
                if (passwordEncoder.matches(password, empleado.getPassword())) {
                    return Optional.of(empleado);
                }
            }
        }
        return Optional.empty();
    }
    
    public boolean cambiarPassword(Integer empleadoId, String passwordActual, String passwordNueva) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            if (passwordEncoder.matches(passwordActual, empleado.getPassword())) {
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
        }
        return false;
    }
    
    public boolean cambiarPasswordAdmin(Integer empleadoId, String passwordNueva) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
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
    
    public boolean actualizarPerfil(Integer empleadoId, Map<String, Object> datos) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            boolean cambiosRealizados = false;
            
            if (datos.containsKey("nombre")) {
                String nuevoNombre = (String) datos.get("nombre");
                if (nuevoNombre != null && !nuevoNombre.trim().isEmpty() && !nuevoNombre.equals(empleado.getNombre())) {
                    empleado.setNombre(nuevoNombre.trim());
                    cambiosRealizados = true;
                }
            }
            
            if (datos.containsKey("cargo")) {
                String nuevoCargo = (String) datos.get("cargo");
                if (nuevoCargo != null && !nuevoCargo.equals(empleado.getCargo())) {
                    empleado.setCargo(nuevoCargo);
                    cambiosRealizados = true;
                }
            }
            
            if (datos.containsKey("descripcion")) {
                String nuevaDescripcion = (String) datos.get("descripcion");
                if (nuevaDescripcion != null && !nuevaDescripcion.equals(empleado.getDescripcion())) {
                    empleado.setDescripcion(nuevaDescripcion);
                    cambiosRealizados = true;
                }
            }
            
            if (datos.containsKey("hobby")) {
                String nuevoHobby = (String) datos.get("hobby");
                if (nuevoHobby != null && !nuevoHobby.equals(empleado.getHobby())) {
                    empleado.setHobby(nuevoHobby);
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
                            cambiosRealizados = true;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error parseando fecha de cumpleaños: " + e.getMessage());
                }
            }
            
            if (datos.containsKey("email")) {
                String nuevoEmail = (String) datos.get("email");
                if (nuevoEmail != null && !nuevoEmail.trim().isEmpty() && !nuevoEmail.equals(empleado.getEmail())) {
                    Optional<Empleado> empleadoExistente = empleadoRepository.findByEmail(nuevoEmail);
                    if (empleadoExistente.isPresent() && !empleadoExistente.get().getId().equals(empleadoId)) {
                        throw new RuntimeException("El email ya está registrado por otro usuario");
                    }
                    empleado.setEmail(nuevoEmail.trim());
                    cambiosRealizados = true;
                }
            }
            
            if (datos.containsKey("foto")) {
                String nuevaFoto = (String) datos.get("foto");
                if (nuevaFoto != null && !nuevaFoto.equals(empleado.getFoto())) {
                    empleado.setFoto(nuevaFoto);
                    cambiosRealizados = true;
                }
            }
            
            if (cambiosRealizados) {
                empleadoRepository.save(empleado);
                
                if (empleado.getEmail() != null && !empleado.getEmail().trim().isEmpty()) {
                    try {
                        emailService.enviarNotificacionActualizacionPerfil(empleado.getEmail(), empleado.getNombre());
                    } catch (Exception e) {
                        System.err.println("⚠️ Error enviando email de notificación: " + e.getMessage());
                    }
                }
            }
            
            return true;
        }
        return false;
    }
    
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