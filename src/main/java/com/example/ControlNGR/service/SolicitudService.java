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

    @Autowired
    private HorarioSemanalService horarioSemanalService;

    private static final DateTimeFormatter AUDIT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public boolean puedeEditarSolicitud(Integer solicitudId, Integer empleadoId, String rolEmpleado) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);

        if (!solicitudOpt.isPresent()) {
            return false;
        }

        Solicitud solicitud = solicitudOpt.get();
        boolean esMiSolicitud = solicitud.getEmpleado().getId().equals(empleadoId);

        // Solo el dueño puede editar su solicitud y solo si está pendiente
        return esMiSolicitud && "pendiente".equals(solicitud.getEstado());
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

        // No puede gestionar su propia solicitud
        if (esMiSolicitud) {
            return false;
        }

        // Permitir gestionar solicitudes pendientes, aprobadas o rechazadas (para correcciones)
        if (!Arrays.asList("pendiente", "aprobado", "rechazado").contains(solicitud.getEstado())) {
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

        // Verificar si es una corrección de estado (ya estaba aprobado o rechazado)
        String estadoAnterior = solicitud.getEstado();
        boolean esCorreccion = Arrays.asList("aprobado", "rechazado").contains(estadoAnterior);

        solicitud.setEstado(estado.toLowerCase());
        solicitud.setAprobadoPor(aprobador);
        solicitud.setFechaAprobacion(LocalDateTime.now());

        // Agregar nota de corrección si aplica
        if (esCorreccion && !estadoAnterior.equals(estado.toLowerCase())) {
            String motivoActual = solicitud.getMotivo() != null ? solicitud.getMotivo() : "";

            // Limpiar notas de corrección anteriores
            if (motivoActual.contains("[Estado corregido")) {
                motivoActual = motivoActual.split("\\[Estado corregido")[0].trim();
            }

            String notaCorreccion = "\n\n[Estado corregido por " + aprobador.getNombre() +
                                   " - " + LocalDateTime.now().format(AUDIT_FORMATTER) +
                                   "] De " + estadoAnterior.toUpperCase() + " a " + estado.toUpperCase();
            solicitud.setMotivo(motivoActual + notaCorreccion);
        } else if (comentarios != null && !comentarios.trim().isEmpty()) {
            String motivoActual = solicitud.getMotivo() != null ? solicitud.getMotivo() : "";
            solicitud.setMotivo(motivoActual + "\n\nComentarios de gestión: " + comentarios);
        }

        Solicitud savedSolicitud = solicitudRepository.save(solicitud);

        // Integración con Horarios Semanales
        try {
            if ("aprobado".equals(estado.toLowerCase())) {
                // Si se aprobó, aplicar a horarios semanales existentes
                horarioSemanalService.aplicarSolicitudAprobada(savedSolicitud);
            } else if (esCorreccion && "rechazado".equals(estado.toLowerCase()) &&
                       "aprobado".equals(estadoAnterior)) {
                // Si se rechazó una que estaba aprobada, revertir en horarios semanales
                horarioSemanalService.revertirSolicitud(savedSolicitud.getId());
            }
        } catch (Exception e) {
            // Log del error pero no fallar la gestión de la solicitud
            System.err.println("Error al actualizar horarios semanales: " + e.getMessage());
        }

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

            // Limpiar notas anteriores (edición y conflictos) para solo mantener la última
            String motivoBase = motivoNuevo;

            // Si el motivo nuevo contiene notas de conflicto, las limpiamos
            if (motivoBase != null && motivoBase.contains("⚠️ CONFLICTO DE FECHAS:")) {
                motivoBase = motivoBase.split("⚠️ CONFLICTO DE FECHAS:")[0].trim();
            }

            // Limpiar notas de edición anteriores del motivo nuevo
            if (motivoBase != null && motivoBase.contains("[Editado por")) {
                motivoBase = motivoBase.split("\\[Editado por")[0].trim();
            }

            // Construir el nuevo motivo con solo la última nota de edición
            nuevoMotivo.append(motivoBase);
            nuevoMotivo.append("\n\n[Editado por ").append(nombreEditor)
                      .append(" - ").append(LocalDateTime.now().format(AUDIT_FORMATTER)).append("]");

            solicitud.setMotivo(nuevoMotivo.toString());
        }

        // Nota: El cambio de estado (aprobar/rechazar) se hace solo a través de gestionarSolicitud
        // La edición es solo para el dueño de la solicitud mientras está pendiente

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
        
        List<Solicitud> conflictosPropios = verificarConflictosFecha(request.getEmpleadoId(), request.getFechaInicio(), request.getFechaFin());

        // Verificar conflictos por rol (otros empleados del mismo rol)
        List<Solicitud> conflictosRol = verificarConflictosPorRolYFechas(
            request.getEmpleadoId(),
            empleado.getRol(),
            request.getFechaInicio(),
            request.getFechaFin()
        );

        String motivo = request.getMotivo();

        // Limpiar notas de conflicto anteriores si existen
        if (motivo != null && motivo.contains("⚠️ CONFLICTO DE FECHAS:")) {
            motivo = motivo.split("⚠️ CONFLICTO DE FECHAS:")[0].trim();
        }

        // Agregar nota de conflicto solo si hay conflictos con otros del mismo rol
        if (!conflictosRol.isEmpty()) {
            StringBuilder conflictoInfo = new StringBuilder();
            conflictoInfo.append("\n\n⚠️ CONFLICTO DE FECHAS: ");
            conflictoInfo.append("Existe(n) ").append(conflictosRol.size()).append(" solicitud(es) ");
            conflictoInfo.append("de compañeros del mismo rol en este período:\n");

            for (Solicitud conf : conflictosRol) {
                conflictoInfo.append("• ").append(conf.getEmpleado().getNombre());
                conflictoInfo.append(" (").append(conf.getTipo()).append(": ");
                conflictoInfo.append(conf.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                conflictoInfo.append(" - ");
                conflictoInfo.append(conf.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                conflictoInfo.append(")\n");
            }

            motivo = motivo + conflictoInfo.toString();
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