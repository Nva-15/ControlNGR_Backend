package com.example.ControlNGR.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID; // Generador de identificador único
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.repository.EmpleadoRepository;

@Service
public class EmpleadoService {
    
    // Repositorio de empleados
    @Autowired
    private EmpleadoRepository empleadoRepository;
    
    // Encriptador de contraseñas
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Validar credenciales de inicio de sesión
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
    
    // Obtener todos los empleados
    public List<Empleado> findAll() {
        return empleadoRepository.findAll();
    }

    // Buscar por ID
    public Optional<Empleado> findById(Integer id) {
        return empleadoRepository.findById(id);
    }

    // Buscar por DNI
    public Optional<Empleado> findByDni(String dni) {
        return empleadoRepository.findByDni(dni);
    }

    // Buscar por username
    public Optional<Empleado> findByUsername(String username) {
        return empleadoRepository.findByUsername(username);
    }
    
    // Guardar o actualizar empleado
    public Empleado save(Empleado empleado) {
        
        // Generar identificador si no existe
        if (empleado.getIdentificador() == null || empleado.getIdentificador().trim().isEmpty()) {
            empleado.setIdentificador(UUID.randomUUID().toString());
        }

        // Encriptar contraseña si es nueva
        if (empleado.getPassword() != null && !empleado.getPassword().trim().isEmpty()) {
            if (!empleado.getPassword().startsWith("$2")) {
                empleado.setPassword(passwordEncoder.encode(empleado.getPassword()));
            }
        }
        
        // Asignar username por defecto
        if (empleado.getUsername() == null || empleado.getUsername().trim().isEmpty()) {
            empleado.setUsername(empleado.getDni());
        }
        
        // Asignar rol por nivel
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
    
    // Eliminar por ID
    public void deleteById(Integer id) {
        empleadoRepository.deleteById(id);
    }

    // Verificar DNI existente
    public boolean existsByDni(String dni) {
        return empleadoRepository.existsByDni(dni);
    }

    // Verificar username existente
    public boolean existsByUsername(String username) {
        return empleadoRepository.existsByUsername(username);
    }

    // Buscar por rol
    public List<Empleado> findByRol(String rol) {
        return empleadoRepository.findByRol(rol);
    }

    // Buscar por estado de usuario
    public List<Empleado> findByUsuarioActivo(Boolean activo) {
        return empleadoRepository.findByUsuarioActivo(activo);
    }
}
