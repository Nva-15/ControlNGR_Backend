package com.example.ControlNGR.service;

import com.example.ControlNGR.dto.SolicitudRequestDTO;
import com.example.ControlNGR.dto.SolicitudResponseDTO;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.entity.Solicitud;
import com.example.ControlNGR.repository.EmpleadoRepository;
import com.example.ControlNGR.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
            .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        
        if (request.getFechaInicio().isAfter(request.getFechaFin())) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        List<Solicitud> conflictos = verificarConflictosFecha(
            request.getEmpleadoId(), 
            request.getFechaInicio(), 
            request.getFechaFin()
        );
        
        boolean tieneConflictos = !conflictos.isEmpty();
        
        Solicitud solicitud = new Solicitud();
        solicitud.setEmpleado(empleado);
        solicitud.setTipo(request.getTipo());
        solicitud.setFechaInicio(request.getFechaInicio());
        solicitud.setFechaFin(request.getFechaFin());
        solicitud.setMotivo(request.getMotivo());
        solicitud.setEstado("pendiente");
        
        if (tieneConflictos) {
            String motivoOriginal = solicitud.getMotivo() != null ? solicitud.getMotivo() : "";
            String notaConflictos = "\n\n⚠️ NOTA: Esta solicitud presenta conflictos de fecha con " + 
                conflictos.size() + " solicitud(es) existente(s).";
            solicitud.setMotivo(motivoOriginal + notaConflictos);
        }
        
        boolean esAutoAprobada = false;
        if (("supervisor".equals(empleado.getRol()) || "admin".equals(empleado.getRol())) && 
            !"vacaciones".equals(request.getTipo())) {
            solicitud.setEstado("aprobado");
            solicitud.setAprobadoPor(empleado);
            solicitud.setFechaAprobacion(LocalDateTime.now());
            esAutoAprobada = true;
        }
        
        Solicitud saved = solicitudRepository.save(solicitud);
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String fechaInicioStr = request.getFechaInicio().format(formatter);
            String fechaFinStr = request.getFechaFin().format(formatter);
            
            if (esAutoAprobada) {
                if (empleado.getEmail() != null && !empleado.getEmail().trim().isEmpty()) {
                    emailService.enviarNotificacionSolicitud(
                        empleado.getEmail(),
                        empleado.getNombre(),
                        request.getTipo(),
                        "aprobado",
                        "Solicitud auto-aprobada",
                        fechaInicioStr,
                        fechaFinStr
                    );
                }
            } else {
                if (empleado.getEmail() != null && !empleado.getEmail().trim().isEmpty()) {
                    String comentarioEmail = tieneConflictos ? 
                        "Tu solicitud ha sido recibida. ⚠️ NOTA: Hay conflictos de fechas con solicitudes existentes." :
                        "Tu solicitud ha sido recibida y está en revisión";
                    
                    emailService.enviarNotificacionSolicitud(
                        empleado.getEmail(),
                        empleado.getNombre(),
                        request.getTipo(),
                        "pendiente",
                        comentarioEmail,
                        fechaInicioStr,
                        fechaFinStr
                    );
                }
                
                List<Empleado> supervisores = empleadoRepository.findByRol("supervisor");
                
                for (Empleado supervisor : supervisores) {
                    if (supervisor.getEmail() != null && 
                        !supervisor.getEmail().trim().isEmpty() &&
                        supervisor.getUsuarioActivo() != null && 
                        supervisor.getUsuarioActivo() &&
                        !supervisor.getId().equals(empleado.getId())) {
                        
                        emailService.enviarNotificacionNuevaSolicitud(
                            supervisor.getEmail(),
                            supervisor.getNombre(),
                            empleado.getNombre(),
                            request.getTipo(),
                            fechaInicioStr,
                            fechaFinStr
                        );
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error enviando emails: " + e.getMessage());
        }
        
        return new SolicitudResponseDTO(saved);
    }

    public List<Solicitud> verificarConflictosFecha(Integer empleadoId, 
                                                   java.time.LocalDate fechaInicio, 
                                                   java.time.LocalDate fechaFin) {
        return solicitudRepository.findConflictosPorRangoFechas(empleadoId, fechaInicio, fechaFin);
    }

    public SolicitudResponseDTO gestionarSolicitud(Integer id, String estado, 
                                                   Integer idAprobador, String comentarios) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        
        Empleado aprobador = empleadoRepository.findById(idAprobador)
                .orElseThrow(() -> new RuntimeException("Empleado aprobador no encontrado"));
        
        if (!"supervisor".equals(aprobador.getRol()) && !"admin".equals(aprobador.getRol())) {
            throw new RuntimeException("Solo supervisores o administradores pueden gestionar solicitudes");
        }
        
        String estadoAnterior = solicitud.getEstado();
        solicitud.setEstado(estado);
        solicitud.setAprobadoPor(aprobador);
        solicitud.setFechaAprobacion(LocalDateTime.now());
        
        if (comentarios != null && !comentarios.trim().isEmpty()) {
            String motivoActual = solicitud.getMotivo() != null ? solicitud.getMotivo() : "";
            solicitud.setMotivo(motivoActual + "\n\nComentarios del aprobador: " + comentarios);
        }
        
        Solicitud updated = solicitudRepository.save(solicitud);
        
        try {
            if (!estado.equals(estadoAnterior)) {
                Empleado empleadoSolicitante = updated.getEmpleado();
                if (empleadoSolicitante.getEmail() != null && 
                    !empleadoSolicitante.getEmail().trim().isEmpty()) {
                    
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    String fechaInicioStr = updated.getFechaInicio().format(formatter);
                    String fechaFinStr = updated.getFechaFin().format(formatter);
                    
                    emailService.enviarNotificacionSolicitud(
                        empleadoSolicitante.getEmail(),
                        empleadoSolicitante.getNombre(),
                        updated.getTipo(),
                        estado,
                        comentarios,
                        fechaInicioStr,
                        fechaFinStr
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error enviando email de notificación: " + e.getMessage());
        }
        
        return new SolicitudResponseDTO(updated);
    }

    public List<SolicitudResponseDTO> obtenerMisSolicitudes(Integer empleadoId) {
        List<Solicitud> solicitudes = solicitudRepository.findByEmpleadoIdOrderByFechaSolicitudDesc(empleadoId);
        return solicitudes.stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<SolicitudResponseDTO> obtenerPendientes() {
        List<Solicitud> solicitudes = solicitudRepository.findSolicitudesPendientes();
        return solicitudes.stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<SolicitudResponseDTO> obtenerTodas() {
        List<Solicitud> solicitudes = solicitudRepository.findAll();
        return solicitudes.stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }

    public SolicitudResponseDTO editarSolicitud(Integer id, Map<String, Object> payload) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrado"));
        
        if (!"pendiente".equals(solicitud.getEstado())) {
            throw new RuntimeException("Solo se pueden editar solicitudes pendientes");
        }
        
        if (payload.containsKey("tipo")) {
            solicitud.setTipo((String) payload.get("tipo"));
        }
        
        if (payload.containsKey("fechaInicio")) {
            String fechaInicioStr = (String) payload.get("fechaInicio");
            solicitud.setFechaInicio(java.time.LocalDate.parse(fechaInicioStr));
        }
        
        if (payload.containsKey("fechaFin")) {
            String fechaFinStr = (String) payload.get("fechaFin");
            solicitud.setFechaFin(java.time.LocalDate.parse(fechaFinStr));
        }
        
        if (payload.containsKey("motivo")) {
            solicitud.setMotivo((String) payload.get("motivo"));
        }
        
        Solicitud updated = solicitudRepository.save(solicitud);
        return new SolicitudResponseDTO(updated);
    }

    public Map<String, Object> exportarSolicitudes(String tipoReporte, Integer empleadoId) {
        List<Solicitud> solicitudes;
        
        if ("todos".equals(tipoReporte)) {
            solicitudes = solicitudRepository.findAll();
        } else if ("mis-solicitudes".equals(tipoReporte) && empleadoId != null) {
            solicitudes = solicitudRepository.findByEmpleadoIdOrderByFechaSolicitudDesc(empleadoId);
        } else if ("pendientes".equals(tipoReporte)) {
            solicitudes = solicitudRepository.findSolicitudesPendientes();
        } else if ("aprobadas".equals(tipoReporte)) {
            solicitudes = solicitudRepository.findByEstado("aprobado");
        } else if ("rechazadas".equals(tipoReporte)) {
            solicitudes = solicitudRepository.findByEstado("rechazado");
        } else {
            solicitudes = solicitudRepository.findAll();
        }
        
        List<Map<String, Object>> datos = solicitudes.stream()
                .map(s -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("id", s.getId());
                    item.put("empleado", s.getEmpleado().getNombre());
                    item.put("tipo", s.getTipo());
                    item.put("fecha_inicio", s.getFechaInicio().toString());
                    item.put("fecha_fin", s.getFechaFin().toString());
                    item.put("estado", s.getEstado());
                    item.put("fecha_solicitud", s.getFechaSolicitud() != null ? 
                        s.getFechaSolicitud().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                    item.put("aprobado_por", s.getAprobadoPor() != null ? s.getAprobadoPor().getNombre() : "");
                    item.put("motivo", s.getMotivo());
                    return item;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> reporte = new java.util.HashMap<>();
        reporte.put("titulo", "Reporte de Solicitudes - " + tipoReporte);
        reporte.put("fecha_generacion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        reporte.put("total_registros", datos.size());
        reporte.put("datos", datos);
        
        return reporte;
    }
}