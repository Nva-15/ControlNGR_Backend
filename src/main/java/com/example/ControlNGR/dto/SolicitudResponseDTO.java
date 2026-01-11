package com.example.ControlNGR.dto;

import com.example.ControlNGR.entity.Solicitud;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SolicitudResponseDTO {
    private Integer id;
    private Integer empleadoId;
    private String empleadoNombre;
    private String tipo;
    private LocalDateTime fechaSolicitud;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivo;
    private String estado;
    private String aprobadoPor; // Nombre del jefe que aprobó
    private LocalDateTime fechaAprobacion;

    public SolicitudResponseDTO(Solicitud solicitud) {
        this.id = solicitud.getId();
        this.empleadoId = solicitud.getEmpleado().getId();
        this.empleadoNombre = solicitud.getEmpleado().getNombre();
        this.tipo = solicitud.getTipo();
        this.fechaSolicitud = solicitud.getFechaSolicitud();
        this.fechaInicio = solicitud.getFechaInicio();
        this.fechaFin = solicitud.getFechaFin();
        this.motivo = solicitud.getMotivo();
        this.estado = solicitud.getEstado();
        
        // CORREGIDO: Obtener nombre del empleado aprobador
        if (solicitud.getAprobadoPor() != null) {
            this.aprobadoPor = solicitud.getAprobadoPor().getNombre();
        } else {
            this.aprobadoPor = null; // Para que el frontend muestre "-- Pendiente --"
        }
        
        this.fechaAprobacion = solicitud.getFechaAprobacion();
    }

    // Getters y Setters (Omitidos, genera todos standard)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(Integer empleadoId) { this.empleadoId = empleadoId; }
    public String getEmpleadoNombre() { return empleadoNombre; }
    public void setEmpleadoNombre(String empleadoNombre) { this.empleadoNombre = empleadoNombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(String aprobadoPor) { this.aprobadoPor = aprobadoPor; }
    public LocalDateTime getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDateTime fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }
}