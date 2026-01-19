package com.example.ControlNGR.service;

import com.example.ControlNGR.dto.SolicitudRequestDTO;
import com.example.ControlNGR.dto.SolicitudResponseDTO;
import com.example.ControlNGR.entity.Empleado;
import com.example.ControlNGR.entity.Solicitud;
import com.example.ControlNGR.repository.EmpleadoRepository;
import com.example.ControlNGR.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SolicitudService {
    
    @Autowired
    private SolicitudRepository solicitudRepository;
    
    @Autowired
    private EmpleadoRepository empleadoRepository;
    
    private static final DateTimeFormatter AUDIT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public boolean puedeEditarSolicitud(Integer solicitudId, Integer empleadoId, String rolEmpleado) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        
        if (!solicitudOpt.isPresent() || !empleadoOpt.isPresent()) {
            return false;
        }
        
        Solicitud solicitud = solicitudOpt.get();
        Empleado empleado = empleadoOpt.get();
        boolean esMiSolicitud = solicitud.getEmpleado().getId().equals(empleadoId);
        String rolSolicitud = solicitud.getEmpleado().getRol();
        
        if ("pendiente".equals(solicitud.getEstado())) {
            switch (rolSolicitud) {
                case "tecnico":
                case "hd":
                case "noc":
                    return esMiSolicitud || "supervisor".equals(rolEmpleado) || "admin".equals(rolEmpleado);
                case "supervisor":
                    return esMiSolicitud || "admin".equals(rolEmpleado);
                case "admin":
                    return "admin".equals(rolEmpleado);
                default:
                    return esMiSolicitud || "admin".equals(rolEmpleado);
            }
        }
        
        if (Arrays.asList("aprobada", "rechazada", "cancelada").contains(solicitud.getEstado())) {
            if (esMiSolicitud) {
                return false;
            }
            
            switch (rolSolicitud) {
                case "tecnico":
                case "hd":
                case "noc":
                    return "supervisor".equals(rolEmpleado) || "admin".equals(rolEmpleado);
                case "supervisor":
                    return "admin".equals(rolEmpleado);
                case "admin":
                    return "admin".equals(rolEmpleado);
                default:
                    return "admin".equals(rolEmpleado);
            }
        }
        
        return false;
    }
    
    public boolean puedeGestionarSolicitud(Integer solicitudId, Integer empleadoId, String rolEmpleado, String accion) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoId);
        
        if (!solicitudOpt.isPresent() || !empleadoOpt.isPresent()) {
            return false;
        }
        
        Solicitud solicitud = solicitudOpt.get();
        Empleado empleado = empleadoOpt.get();
        boolean esMiSolicitud = solicitud.getEmpleado().getId().equals(empleadoId);
        String rolSolicitud = solicitud.getEmpleado().getRol();
        
        if (esMiSolicitud) {
            return false;
        }
        
        if (!"pendiente".equals(solicitud.getEstado())) {
            return false;
        }
        
        switch (rolSolicitud) {
            case "tecnico":
            case "hd":
            case "noc":
                return "supervisor".equals(rolEmpleado) || "admin".equals(rolEmpleado);
            case "supervisor":
                return "admin".equals(rolEmpleado);
            case "admin":
                return "admin".equals(rolEmpleado);
            default:
                return false;
        }
    }
    
    public SolicitudResponseDTO gestionarSolicitud(Integer id, String estado, Integer idAprobador, String comentarios) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(id);
        Optional<Empleado> aprobadorOpt = empleadoRepository.findById(idAprobador);
        
        if (!solicitudOpt.isPresent()) {
            throw new RuntimeException("Solicitud no encontrada");
        }
        
        if (!aprobadorOpt.isPresent()) {
            throw new RuntimeException("Aprobador no encontrado");
        }
        
        Solicitud solicitud = solicitudOpt.get();
        Empleado aprobador = aprobadorOpt.get();
        
        if (!puedeGestionarSolicitud(id, idAprobador, aprobador.getRol(), estado)) {
            throw new RuntimeException("No tiene permisos para gestionar esta solicitud");
        }
        
        if (solicitud.getEmpleado().getId().equals(idAprobador)) {
            throw new RuntimeException("No puede aprobar/rechazar su propia solicitud");
        }
        
        if (!Arrays.asList("aprobado", "rechazado").contains(estado.toLowerCase())) {
            throw new RuntimeException("Estado inválido para gestión");
        }
        
        solicitud.setEstado(estado.toLowerCase());
        solicitud.setAprobadoPor(aprobador);
        solicitud.setFechaAprobacion(LocalDateTime.now());
        
        if (comentarios != null && !comentarios.trim().isEmpty()) {
            String motivoActual = solicitud.getMotivo() != null ? solicitud.getMotivo() : "";
            solicitud.setMotivo(motivoActual + "\n\nComentarios de gestión: " + comentarios);
        }
        
        Solicitud savedSolicitud = solicitudRepository.save(solicitud);
        return new SolicitudResponseDTO(savedSolicitud);
    }
    
    public SolicitudResponseDTO editarSolicitud(Integer id, Map<String, Object> payload, 
                                                Integer empleadoEditorId, String rolEditor) {
        
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(id);
        if (!solicitudOpt.isPresent()) {
            throw new RuntimeException("Solicitud no encontrada");
        }
        
        Solicitud solicitud = solicitudOpt.get();
        
        if (!puedeEditarSolicitud(id, empleadoEditorId, rolEditor)) {
            throw new RuntimeException("No tiene permisos para editar esta solicitud");
        }
        
        Optional<Empleado> editorOpt = empleadoRepository.findById(empleadoEditorId);
        if (!editorOpt.isPresent()) {
            throw new RuntimeException("Empleado editor no encontrado");
        }
        String nombreEditor = editorOpt.get().getNombre();
        
        String motivoOriginal = solicitud.getMotivo();
        StringBuilder nuevoMotivo = new StringBuilder();
        
        if (payload.containsKey("fechaInicio")) {
            String fechaInicioStr = (String) payload.get("fechaInicio");
            LocalDate fechaInicio = LocalDate.parse(fechaInicioStr);
            if (solicitud.getFechaFin() != null && fechaInicio.isAfter(solicitud.getFechaFin())) {
                throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de fin");
            }
            solicitud.setFechaInicio(fechaInicio);
        }
        
        if (payload.containsKey("fechaFin")) {
            String fechaFinStr = (String) payload.get("fechaFin");
            LocalDate fechaFin = LocalDate.parse(fechaFinStr);
            if (solicitud.getFechaInicio() != null && fechaFin.isBefore(solicitud.getFechaInicio())) {
                throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
            }
            solicitud.setFechaFin(fechaFin);
        }
        
        if (payload.containsKey("tipo")) {
            solicitud.setTipo((String) payload.get("tipo"));
        }
        
        if (payload.containsKey("motivo")) {
            String motivoNuevo = (String) payload.get("motivo");
            
            if (motivoOriginal != null && !motivoOriginal.contains("[Editado por")) {
                nuevoMotivo.append(motivoOriginal);
                nuevoMotivo.append("\n\n[Editado por ").append(nombreEditor)
                          .append(" - ").append(LocalDateTime.now().format(AUDIT_FORMATTER))
                          .append("]\n").append(motivoNuevo);
            } else {
                if (motivoOriginal != null) {
                    String[] partes = motivoOriginal.split("\\[Editado por");
                    if (partes.length > 0) {
                        nuevoMotivo.append(partes[0].trim());
                    }
                }
                
                nuevoMotivo.append("\n\n[Editado por ").append(nombreEditor)
                          .append(" - ").append(LocalDateTime.now().format(AUDIT_FORMATTER))
                          .append("]\n").append(motivoNuevo);
            }
            
            solicitud.setMotivo(nuevoMotivo.toString());
        }
        
        if (payload.containsKey("estado") && puedeGestionarSolicitud(id, empleadoEditorId, rolEditor, (String) payload.get("estado"))) {
            String nuevoEstado = (String) payload.get("estado");
            if (Arrays.asList("aprobado", "rechazado").contains(nuevoEstado.toLowerCase())) {
                solicitud.setEstado(nuevoEstado.toLowerCase());
                Optional<Empleado> aprobadorOpt = empleadoRepository.findById(empleadoEditorId);
                if (aprobadorOpt.isPresent()) {
                    solicitud.setAprobadoPor(aprobadorOpt.get());
                    solicitud.setFechaAprobacion(LocalDateTime.now());
                }
            }
        }
        
        Solicitud solicitudActualizada = solicitudRepository.save(solicitud);
        return new SolicitudResponseDTO(solicitudActualizada);
    }
    
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findById(request.getEmpleadoId());
        if (!empleadoOpt.isPresent()) {
            throw new RuntimeException("Empleado no encontrado");
        }
        
        Empleado empleado = empleadoOpt.get();
        
        if (request.getFechaInicio() == null || request.getFechaFin() == null) {
            throw new RuntimeException("Fechas requeridas");
        }
        
        if (request.getFechaInicio().isAfter(request.getFechaFin())) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        List<Solicitud> conflictos = verificarConflictosFecha(request.getEmpleadoId(), request.getFechaInicio(), request.getFechaFin());
        
        String motivo = request.getMotivo();
        if (!conflictos.isEmpty()) {
            String conflictoInfo = "\n\n⚠️ NOTIFICACIÓN DEL SISTEMA: Se detectaron " + conflictos.size() + 
                                   " solicitud(es) existente(s) en el rango de fechas seleccionado.";
            motivo = motivo + conflictoInfo;
        }
        
        Solicitud solicitud = new Solicitud();
        solicitud.setEmpleado(empleado);
        solicitud.setTipo(request.getTipo());
        solicitud.setFechaInicio(request.getFechaInicio());
        solicitud.setFechaFin(request.getFechaFin());
        solicitud.setMotivo(motivo);
        solicitud.setEstado("pendiente");
        
        Solicitud savedSolicitud = solicitudRepository.save(solicitud);
        return new SolicitudResponseDTO(savedSolicitud);
    }
    
    public List<Solicitud> verificarConflictosFecha(Integer empleadoId, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new RuntimeException("Fechas inválidas");
        }
        
        if (fechaInicio.isAfter(fechaFin)) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        return solicitudRepository.findConflictosPorRangoFechas(empleadoId, fechaInicio, fechaFin);
    }
    
    public List<Solicitud> verificarConflictosPorRolYFechas(Integer empleadoId, String rolEmpleado, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new RuntimeException("Fechas inválidas");
        }
        
        if (fechaInicio.isAfter(fechaFin)) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        return solicitudRepository.findConflictosPorRolYRangoFechas(empleadoId, rolEmpleado, fechaInicio, fechaFin);
    }
    
    public List<SolicitudResponseDTO> obtenerMisSolicitudes(Integer empleadoId) {
        return solicitudRepository.findByEmpleadoIdOrderByFechaSolicitudDesc(empleadoId)
                .stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    public List<SolicitudResponseDTO> obtenerPendientes() {
        return solicitudRepository.findSolicitudesPendientes()
                .stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    public List<SolicitudResponseDTO> obtenerTodas() {
        return solicitudRepository.findAll()
                .stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    public SolicitudResponseDTO editarSolicitud(Integer id, Map<String, Object> payload) {
        if (!payload.containsKey("empleadoEditorId") || !payload.containsKey("rolEditor")) {
            throw new RuntimeException("Datos de editor requeridos");
        }
        return editarSolicitud(id, payload, (Integer) payload.get("empleadoEditorId"), (String) payload.get("rolEditor"));
    }
    
    public Map<String, Object> exportarSolicitudes(String tipoReporte, Integer empleadoId) {
        Map<String, Object> reporte = new HashMap<>();
        
        if ("mis-solicitudes".equals(tipoReporte) && empleadoId != null) {
            List<SolicitudResponseDTO> solicitudes = obtenerMisSolicitudes(empleadoId);
            reporte.put("titulo", "Mis Solicitudes");
            reporte.put("total", solicitudes.size());
            reporte.put("solicitudes", solicitudes);
        } else if ("pendientes".equals(tipoReporte)) {
            List<SolicitudResponseDTO> solicitudes = obtenerPendientes();
            reporte.put("titulo", "Solicitudes Pendientes");
            reporte.put("total", solicitudes.size());
            reporte.put("solicitudes", solicitudes);
        } else if ("historial".equals(tipoReporte)) {
            List<SolicitudResponseDTO> todas = obtenerTodas();
            List<SolicitudResponseDTO> historial = todas.stream()
                    .filter(s -> !"pendiente".equals(s.getEstado()))
                    .collect(Collectors.toList());
            reporte.put("titulo", "Historial de Solicitudes");
            reporte.put("total", historial.size());
            reporte.put("solicitudes", historial);
        } else {
            List<SolicitudResponseDTO> todas = obtenerTodas();
            reporte.put("titulo", "Todas las Solicitudes");
            reporte.put("total", todas.size());
            reporte.put("solicitudes", todas);
        }
        
        reporte.put("fecha_generacion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        return reporte;
    }
}