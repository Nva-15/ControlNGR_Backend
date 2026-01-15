package com.example.ControlNGR.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ControlNGR.dto.SolicitudRequestDTO;
import com.example.ControlNGR.dto.SolicitudResponseDTO;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.entity.Solicitud;
import com.example.ControlNGR.repository.EmpleadoRepository;
import com.example.ControlNGR.repository.SolicitudRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;
    
    @Autowired
    private EmailService emailService;

    @Transactional
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request) {
        if (request.getFechaInicio().isAfter(request.getFechaFin())) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha fin");
        }

        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        // Verificar conflictos de fechas
        List<Solicitud> conflictos = verificarConflictosFecha(
            empleado.getId(), 
            request.getFechaInicio(), 
            request.getFechaFin()
        );
        
        Solicitud solicitud = new Solicitud();
        solicitud.setEmpleado(empleado);
        solicitud.setTipo(request.getTipo());
        solicitud.setFechaInicio(request.getFechaInicio());
        solicitud.setFechaFin(request.getFechaFin());
        solicitud.setMotivo(request.getMotivo());
        solicitud.setEstado("pendiente");
        solicitud.setFechaSolicitud(LocalDateTime.now());

        // Si hay conflictos, agregar advertencia al motivo
        if (!conflictos.isEmpty()) {
            String motivoOriginal = solicitud.getMotivo();
            String advertencia = "\n\n[ADVERTENCIA DEL SISTEMA]: Ya existe(n) solicitud(es) para este período:";
            for (Solicitud conflicto : conflictos) {
                advertencia += "\n- " + conflicto.getTipo() + " (" + 
                             conflicto.getFechaInicio() + " al " + 
                             conflicto.getFechaFin() + ") - Estado: " + 
                             conflicto.getEstado();
            }
            advertencia += "\nSu solicitud será evaluada cuidadosamente.";
            solicitud.setMotivo(motivoOriginal + advertencia);
        }
        
        Solicitud saved = solicitudRepository.save(solicitud);
        
        // Notificar a supervisores sobre nueva solicitud
        notificarNuevaSolicitud(saved);
        
        SolicitudResponseDTO response = new SolicitudResponseDTO(saved);
        response.setTieneConflictos(!conflictos.isEmpty());
        
        return response;
    }
    
    public List<Solicitud> verificarConflictosFecha(Integer empleadoId, LocalDate fechaInicio, LocalDate fechaFin) {
        return solicitudRepository.findByEmpleadoId(empleadoId).stream()
                .filter(s -> !"rechazado".equals(s.getEstado()))
                .filter(s -> tieneConflicto(s.getFechaInicio(), s.getFechaFin(), fechaInicio, fechaFin))
                .collect(Collectors.toList());
    }
    
    private boolean tieneConflicto(LocalDate inicio1, LocalDate fin1, LocalDate inicio2, LocalDate fin2) {
        return !(fin1.isBefore(inicio2) || inicio1.isAfter(fin2));
    }
    
    private void notificarNuevaSolicitud(Solicitud solicitud) {
        try {
            // Buscar supervisores/admin para notificar
            List<Empleado> supervisores = empleadoRepository.findByRol("admin");
            supervisores.addAll(empleadoRepository.findByRol("supervisor"));
            
            for (Empleado supervisor : supervisores) {
                if (supervisor.getEmail() != null && Boolean.TRUE.equals(supervisor.getUsuarioActivo())) {
                    emailService.enviarNotificacionNuevaSolicitud(
                        supervisor.getEmail(),
                        supervisor.getNombre(),
                        solicitud.getEmpleado().getNombre(),
                        solicitud.getTipo(),
                        solicitud.getFechaInicio().toString(),
                        solicitud.getFechaFin().toString()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error notificando nueva solicitud: " + e.getMessage());
        }
    }

    @Transactional
    public SolicitudResponseDTO gestionarSolicitud(Integer solicitudId, String nuevoEstado, 
                                                  Integer idEmpleadoAprobador, String comentarios) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"pendiente".equals(solicitud.getEstado())) {
            throw new RuntimeException("La solicitud ya fue procesada anteriormente");
        }

        Empleado aprobador = empleadoRepository.findById(idEmpleadoAprobador)
                .orElseThrow(() -> new RuntimeException("Empleado aprobador no encontrado"));

        solicitud.setEstado(nuevoEstado.toLowerCase());
        solicitud.setAprobadoPor(aprobador);
        solicitud.setFechaAprobacion(LocalDateTime.now());
        
        // Agregar comentarios al motivo si existen
        if (comentarios != null && !comentarios.trim().isEmpty()) {
            String motivoActual = solicitud.getMotivo();
            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            solicitud.setMotivo(motivoActual + "\n\n[COMENTARIOS SUPERVISOR - " + fechaActual + "]: " + comentarios);
        }

        Solicitud updated = solicitudRepository.save(solicitud);
        
        // Enviar notificación por email al empleado
        enviarNotificacionEstado(updated);
        
        SolicitudResponseDTO response = new SolicitudResponseDTO(updated);
        response.setTieneConflictos(false);
        
        return response;
    }
    
    private void enviarNotificacionEstado(Solicitud solicitud) {
        try {
            Empleado empleado = solicitud.getEmpleado();
            if (empleado.getEmail() != null) {
                // Extraer solo los comentarios más recientes
                String comentariosRecientes = obtenerComentariosRecientes(solicitud.getMotivo());
                
                emailService.enviarNotificacionSolicitud(
                    empleado.getEmail(),
                    empleado.getNombre(),
                    solicitud.getTipo(),
                    solicitud.getEstado(),
                    comentariosRecientes,
                    solicitud.getFechaInicio().toString(),
                    solicitud.getFechaFin().toString()
                );
            }
        } catch (Exception e) {
            System.err.println("❌ Error enviando notificación de estado: " + e.getMessage());
        }
    }
    
    private String obtenerComentariosRecientes(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            return "Sin comentarios adicionales";
        }
        
        // Buscar los comentarios más recientes en el motivo
        if (motivo.contains("[COMENTARIOS SUPERVISOR]")) {
            String[] partes = motivo.split("\\[COMENTARIOS SUPERVISOR");
            String ultimaParte = partes[partes.length - 1];
            if (ultimaParte.contains("]:")) {
                return ultimaParte.substring(ultimaParte.indexOf("]:") + 2).trim();
            }
        }
        
        if (motivo.contains("[OBSERVACIONES ADMIN]")) {
            String[] partes = motivo.split("\\[OBSERVACIONES ADMIN");
            String ultimaParte = partes[partes.length - 1];
            if (ultimaParte.contains("]:")) {
                return ultimaParte.substring(ultimaParte.indexOf("]:") + 2).trim();
            }
        }
        
        if (motivo.contains("[ADVERTENCIA DEL SISTEMA]")) {
            String[] partes = motivo.split("\\[ADVERTENCIA DEL SISTEMA");
            String ultimaParte = partes[partes.length - 1];
            if (ultimaParte.contains("]:")) {
                return ultimaParte.substring(ultimaParte.indexOf("]:") + 2).trim();
            }
        }
        
        return "Sin comentarios específicos";
    }
    
    @Transactional
    public SolicitudResponseDTO editarSolicitud(Integer solicitudId, Map<String, Object> datos) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        
        if (!"pendiente".equals(solicitud.getEstado())) {
            throw new RuntimeException("Solo se pueden editar solicitudes pendientes");
        }
        
        // Actualizar campos
        if (datos.containsKey("tipo")) {
            solicitud.setTipo((String) datos.get("tipo"));
        }
        
        if (datos.containsKey("fechaInicio")) {
            LocalDate fechaInicio = LocalDate.parse(datos.get("fechaInicio").toString());
            solicitud.setFechaInicio(fechaInicio);
        }
        
        if (datos.containsKey("fechaFin")) {
            LocalDate fechaFin = LocalDate.parse(datos.get("fechaFin").toString());
            solicitud.setFechaFin(fechaFin);
        }
        
        if (datos.containsKey("motivo")) {
            solicitud.setMotivo((String) datos.get("motivo"));
        }
        
        // Agregar observaciones del administrador
        if (datos.containsKey("observacionesAdmin")) {
            String observaciones = (String) datos.get("observacionesAdmin");
            if (observaciones != null && !observaciones.trim().isEmpty()) {
                String motivoActual = solicitud.getMotivo();
                String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                String observacionConFecha = "\n\n[OBSERVACIONES ADMIN - " + fechaActual + "]: " + observaciones;
                solicitud.setMotivo(motivoActual + observacionConFecha);
                
                // Enviar notificación al empleado sobre los cambios
                enviarNotificacionEstado(solicitud);
            }
        }
        
        Solicitud updated = solicitudRepository.save(solicitud);
        
        SolicitudResponseDTO response = new SolicitudResponseDTO(updated);
        response.setTieneConflictos(false);
        
        return response;
    }
    
    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerMisSolicitudes(Integer empleadoId) {
        return solicitudRepository.findByEmpleadoIdOrderByFechaSolicitudDesc(empleadoId).stream()
                .map(s -> {
                    SolicitudResponseDTO dto = new SolicitudResponseDTO(s);
                    // Verificar conflictos para cada solicitud
                    List<Solicitud> conflictos = verificarConflictosFecha(
                        s.getEmpleado().getId(), 
                        s.getFechaInicio(), 
                        s.getFechaFin()
                    ).stream()
                    .filter(c -> !c.getId().equals(s.getId())) // Excluir la propia solicitud
                    .collect(Collectors.toList());
                    
                    dto.setTieneConflictos(!conflictos.isEmpty());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerPendientes() {
        return solicitudRepository.findSolicitudesPendientes().stream()
                .map(s -> {
                    SolicitudResponseDTO dto = new SolicitudResponseDTO(s);
                    // Verificar conflictos
                    List<Solicitud> conflictos = verificarConflictosFecha(
                        s.getEmpleado().getId(), 
                        s.getFechaInicio(), 
                        s.getFechaFin()
                    ).stream()
                    .filter(c -> !c.getId().equals(s.getId()))
                    .collect(Collectors.toList());
                    
                    dto.setTieneConflictos(!conflictos.isEmpty());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerTodas() {
        return solicitudRepository.findAll().stream()
                .map(s -> {
                    SolicitudResponseDTO dto = new SolicitudResponseDTO(s);
                    // Verificar conflictos para cada solicitud
                    List<Solicitud> conflictos = verificarConflictosFecha(
                        s.getEmpleado().getId(), 
                        s.getFechaInicio(), 
                        s.getFechaFin()
                    ).stream()
                    .filter(c -> !c.getId().equals(s.getId())) // Excluir la propia solicitud
                    .collect(Collectors.toList());
                    
                    dto.setTieneConflictos(!conflictos.isEmpty());
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> exportarSolicitudes(String tipoReporte, Integer empleadoId) {
        List<Solicitud> solicitudes;
        
        switch(tipoReporte.toLowerCase()) {
            case "mis-solicitudes":
                if (empleadoId == null || empleadoId <= 0) {
                    throw new RuntimeException("Se requiere ID de empleado para este reporte");
                }
                solicitudes = solicitudRepository.findByEmpleadoIdOrderByFechaSolicitudDesc(empleadoId);
                break;
            case "pendientes":
                solicitudes = solicitudRepository.findSolicitudesPendientes();
                break;
            case "todas":
                solicitudes = solicitudRepository.findAll();
                break;
            case "historial":
                solicitudes = solicitudRepository.findAll().stream()
                        .filter(s -> !"pendiente".equals(s.getEstado()))
                        .collect(Collectors.toList());
                break;
            default:
                throw new RuntimeException("Tipo de reporte no válido. Use: mis-solicitudes, pendientes, todas, historial");
        }
        
        List<Map<String, Object>> datos = solicitudes.stream()
                .map(s -> {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("ID", s.getId());
                    fila.put("Empleado", s.getEmpleado().getNombre());
                    fila.put("DNI", s.getEmpleado().getDni());
                    fila.put("Tipo", capitalizar(s.getTipo()));
                    fila.put("Fecha_Solicitud", s.getFechaSolicitud() != null ? 
                        s.getFechaSolicitud().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                    fila.put("Fecha_Inicio", s.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    fila.put("Fecha_Fin", s.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    fila.put("Dias", calcularDias(s.getFechaInicio(), s.getFechaFin()));
                    fila.put("Motivo", limitarTexto(s.getMotivo(), 100));
                    fila.put("Estado", capitalizar(s.getEstado()));
                    fila.put("Autorizado_Por", s.getAprobadoPor() != null ? s.getAprobadoPor().getNombre() : "Pendiente");
                    fila.put("Fecha_Aprobacion", s.getFechaAprobacion() != null ? 
                        s.getFechaAprobacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                    
                    // Verificar conflictos
                    List<Solicitud> conflictos = verificarConflictosFecha(
                        s.getEmpleado().getId(), 
                        s.getFechaInicio(), 
                        s.getFechaFin()
                    ).stream()
                    .filter(c -> !c.getId().equals(s.getId()))
                    .collect(Collectors.toList());
                    
                    fila.put("Tiene_Conflictos", !conflictos.isEmpty());
                    fila.put("Conflictos", conflictos.size());
                    
                    return fila;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("titulo", "Reporte de Solicitudes - " + capitalizar(tipoReporte));
        reporte.put("fecha_generacion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        reporte.put("total_registros", datos.size());
        reporte.put("datos", datos);
        
        return reporte;
    }
    
    private String capitalizar(String texto) {
        if (texto == null || texto.trim().isEmpty()) return "";
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }
    
    private String limitarTexto(String texto, int longitud) {
        if (texto == null) return "";
        if (texto.length() <= longitud) return texto;
        return texto.substring(0, longitud) + "...";
    }
    
    private long calcularDias(LocalDate inicio, LocalDate fin) {
        return java.time.temporal.ChronoUnit.DAYS.between(inicio, fin) + 1;
    }
}