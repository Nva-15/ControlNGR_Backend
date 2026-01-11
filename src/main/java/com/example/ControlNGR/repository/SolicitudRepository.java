package com.example.ControlNGR.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.ControlNGR.entity.Solicitud;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {
    
    List<Solicitud> findByEmpleadoId(Integer empleadoId);
    
    List<Solicitud> findByEstado(String estado);
    
    List<Solicitud> findByTipo(String tipo);
    
    @Query("SELECT s FROM Solicitud s WHERE s.empleado.id = :empleadoId AND s.estado = 'aprobado' " +
           "AND :fecha BETWEEN s.fechaInicio AND s.fechaFin")
    List<Solicitud> findSolicitudesAprobadasPorEmpleadoEnFecha(
            @Param("empleadoId") Integer empleadoId,
            @Param("fecha") LocalDate fecha);
    
    List<Solicitud> findByAprobadoPorId(Integer aprobadoPorId);
    
    @Query("SELECT s FROM Solicitud s WHERE s.estado = 'pendiente'")
    List<Solicitud> findSolicitudesPendientes();
}