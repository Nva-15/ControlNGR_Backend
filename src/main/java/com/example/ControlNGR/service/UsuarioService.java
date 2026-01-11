package com.example.ControlNGR.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.ControlNGR.entity.Usuario;
import com.example.ControlNGR.repository.UsuarioRepository;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }
    
    public Optional<Usuario> findById(Integer id) {
        return usuarioRepository.findById(id);
    }
    
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }
    
    public Usuario save(Usuario usuario) {
        // Verificar si es una actualización (tiene ID) o creación (no tiene ID)
        if (usuario.getId() != null) {
            // Es una actualización - Buscar el usuario existente
            Optional<Usuario> usuarioExistenteOpt = usuarioRepository.findById(usuario.getId());
            
            if (usuarioExistenteOpt.isPresent()) {
                Usuario usuarioExistente = usuarioExistenteOpt.get();
                
                // Solo encriptar la contraseña si se proporcionó una nueva
                if (usuario.getPassword() != null && !usuario.getPassword().trim().isEmpty()) {
                    usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
                } else {
                    // Mantener la contraseña actual del usuario existente
                    usuario.setPassword(usuarioExistente.getPassword());
                }
            }
        } else {
            // Es una creación - Encriptar la contraseña (debe existir)
            if (usuario.getPassword() != null && !usuario.getPassword().trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            } else {
                throw new IllegalArgumentException("La contraseña es requerida para nuevos usuarios");
            }
        }
        
        if (usuario.getActivo() == null) {
            usuario.setActivo(true);
        }
        
        return usuarioRepository.save(usuario);
    }
    
    public void deleteById(Integer id) {
        usuarioRepository.deleteById(id);
    }
    
    public boolean existsByUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }
    
    // Método para verificar credenciales de login
    public Optional<Usuario> validarCredenciales(String username, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // Verificar si el usuario está activo
            if (Boolean.TRUE.equals(usuario.getActivo())) {
                // Verificar contraseña
                if (passwordEncoder.matches(password, usuario.getPassword())) {
                    return Optional.of(usuario);
                }
            }
        }
        
        return Optional.empty();
    }
}