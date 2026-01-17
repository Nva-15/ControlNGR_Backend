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
    
    // Métodos estándar
    List<Solicitud> findByEmpleadoId(Integer empleadoId);
    List<Solicitud> findByEstado(String estado);
    List<Solicitud> findByTipo(String tipo);
    
    // Historial ordenado (Para "Mis Solicitudes")
    List<Solicitud> findByEmpleadoIdOrderByFechaSolicitudDesc(Integer empleadoId);
    
    // Bandeja de entrada del Supervisor (filtrado por estado y ordenado)
    List<Solicitud> findByEstadoOrderByFechaSolicitudDesc(String estado);
    
    // Validar conflictos de fechas (Útil para no pedir vacaciones en días ya aprobados)
    @Query("SELECT s FROM Solicitud s WHERE s.empleado.id = :empleadoId AND s.estado = 'aprobado' " +
           "AND :fecha BETWEEN s.fechaInicio AND s.fechaFin")
    List<Solicitud> findSolicitudesAprobadasPorEmpleadoEnFecha(
            @Param("empleadoId") Integer empleadoId,
            @Param("fecha") LocalDate fecha);
    
    // Reporte por aprobador
    List<Solicitud> findByAprobadoPorId(Integer aprobadoPorId);
    
    // Consulta explícita para pendientes
    @Query("SELECT s FROM Solicitud s WHERE s.estado = 'pendiente'")
    List<Solicitud> findSolicitudesPendientes();
    
    // NUEVO MÉTODO PARA CONFLICTOS
    @Query("SELECT s FROM Solicitud s WHERE s.empleado.id = :empleadoId " +
           "AND s.estado IN ('pendiente', 'aprobado') " +
           "AND ((s.fechaInicio BETWEEN :fechaInicio AND :fechaFin) OR " +
           "(s.fechaFin BETWEEN :fechaInicio AND :fechaFin) OR " +
           "(:fechaInicio BETWEEN s.fechaInicio AND s.fechaFin) OR " +
           "(:fechaFin BETWEEN s.fechaInicio AND s.fechaFin))")
    List<Solicitud> findConflictosPorRangoFechas(
            @Param("empleadoId") Integer empleadoId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);
    
}