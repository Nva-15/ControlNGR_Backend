package com.example.ControlNGR.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.repository.EmpleadoRepository;

@Service
public class EmpleadoService {
    
    @Autowired
    private EmpleadoRepository empleadoRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Método para login
    public Optional<Empleado> validarCredenciales(String username, String password) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findByUsername(username);
        
        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            
            // Verificar si el usuario está activo
            if (Boolean.TRUE.equals(empleado.getActivo()) && 
                Boolean.TRUE.equals(empleado.getUsuarioActivo())) {
                
                // Verificar contraseña
                if (passwordEncoder.matches(password, empleado.getPassword())) {
                    return Optional.of(empleado);
                }
            }
        }
        
        return Optional.empty();
    }
    
    // Métodos existentes
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
    
    public Empleado save(Empleado empleado) {
        // Si es un nuevo empleado o se está actualizando la contraseña
        if (empleado.getPassword() != null && !empleado.getPassword().trim().isEmpty()) {
            // Si no empieza con $2a$ o $2y$, encriptarla
            if (!empleado.getPassword().startsWith("$2")) {
                empleado.setPassword(passwordEncoder.encode(empleado.getPassword()));
            }
        }
        
        // Si no tiene username, usar el DNI
        if (empleado.getUsername() == null || empleado.getUsername().trim().isEmpty()) {
            empleado.setUsername(empleado.getDni());
        }
        
        // Asignar rol por defecto si no tiene
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
}