package com.example.ControlNGR.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.ControlNGR.entity.Empleado;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {
    Optional<Empleado> findByIdentificador(String identificador);
    Optional<Empleado> findByDni(String dni);
    Optional<Empleado> findByUsername(String username);
    Optional<Empleado> findByEmail(String email); // NUEVO
    
    Boolean existsByDni(String dni);
    Boolean existsByUsername(String username);
    
    List<Empleado> findByActivo(Boolean activo);
    List<Empleado> findByNivel(String nivel);
    List<Empleado> findByRol(String rol);
    List<Empleado> findByUsuarioActivo(Boolean usuarioActivo);
    
    @Query("SELECT e FROM Empleado e WHERE e.nombre LIKE %:nombre%")
    List<Empleado> buscarPorNombre(@Param("nombre") String nombre);
}