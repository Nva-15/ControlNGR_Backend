package com.example.ControlNGR.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.ControlNGR.dto.SolicitudRequestDTO;
import com.example.ControlNGR.dto.SolicitudResponseDTO;
import com.example.ControlNGR.service.SolicitudService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @PostMapping("/crear")
    public ResponseEntity<?> crearSolicitud(@RequestBody SolicitudRequestDTO request) {
        try {
            SolicitudResponseDTO response = solicitudService.crearSolicitud(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear solicitud: " + e.getMessage()));
        }
    }

    @PostMapping("/verificar-conflictos")
    public ResponseEntity<?> verificarConflictos(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== ENDPOINT /verificar-conflictos INICIADO ===");
            System.out.println("Datos recibidos: " + request);
            
            Integer empleadoId = (Integer) request.get("empleadoId");
            String fechaInicioStr = (String) request.get("fechaInicio");
            String fechaFinStr = (String) request.get("fechaFin");
            
            if (empleadoId == null || fechaInicioStr == null || fechaFinStr == null) {
                System.out.println("Error: Datos incompletos");
                return ResponseEntity.badRequest().body(Map.of("error", "Datos incompletos", "success", false));
            }
            
            java.time.LocalDate fechaInicio = java.time.LocalDate.parse(fechaInicioStr);
            java.time.LocalDate fechaFin = java.time.LocalDate.parse(fechaFinStr);
            
            System.out.println("Parseando fechas:");
            System.out.println("  fechaInicio: " + fechaInicio);
            System.out.println("  fechaFin: " + fechaFin);
            
            List<com.example.ControlNGR.entity.Solicitud> conflictos = 
                solicitudService.verificarConflictosFecha(empleadoId, fechaInicio, fechaFin);
            
            boolean tieneConflictos = !conflictos.isEmpty();
            String mensaje = tieneConflictos ? 
                "⚠️ Ya existen " + conflictos.size() + " solicitud(es) para este período" :
                "✅ No hay conflictos de fecha";
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            List<Map<String, Object>> detallesConflictos = conflictos.stream()
                    .map(s -> {
                        Map<String, Object> detalle = new java.util.HashMap<>();
                        detalle.put("id", s.getId());
                        detalle.put("tipo", s.getTipo());
                        detalle.put("fechaInicio", s.getFechaInicio().format(formatter));
                        detalle.put("fechaFin", s.getFechaFin().format(formatter));
                        detalle.put("estado", s.getEstado());
                        detalle.put("motivo", s.getMotivo());
                        return detalle;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> respuesta = new java.util.HashMap<>();
            respuesta.put("tieneConflictos", tieneConflictos);
            respuesta.put("mensaje", mensaje);
            respuesta.put("totalConflictos", conflictos.size());
            respuesta.put("conflictos", detallesConflictos);
            respuesta.put("success", true);
            
            System.out.println("Respuesta enviada: " + respuesta);
            System.out.println("=== ENDPOINT /verificar-conflictos FINALIZADO ===");
            
            return ResponseEntity.ok(respuesta);
            
        } catch (RuntimeException e) {
            System.err.println("❌ Error en /verificar-conflictos: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), "success", false));
        } catch (Exception e) {
            System.err.println("❌ Error en /verificar-conflictos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar conflictos: " + e.getMessage(), "success", false));
        }
    }

    @GetMapping("/mis-solicitudes/{empleadoId}")
    public ResponseEntity<List<SolicitudResponseDTO>> getMisSolicitudes(@PathVariable("empleadoId") Integer empleadoId) {
        return ResponseEntity.ok(solicitudService.obtenerMisSolicitudes(empleadoId));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<SolicitudResponseDTO>> getPendientes() {
        return ResponseEntity.ok(solicitudService.obtenerPendientes());
    }
    
    @GetMapping("/todas")
    public ResponseEntity<List<SolicitudResponseDTO>> getTodas() {
        return ResponseEntity.ok(solicitudService.obtenerTodas());
    }
    
    @GetMapping("/historial")
    public ResponseEntity<List<SolicitudResponseDTO>> getHistorial() {
        List<SolicitudResponseDTO> todas = solicitudService.obtenerTodas();
        List<SolicitudResponseDTO> historial = todas.stream()
                .filter(s -> !"pendiente".equals(s.getEstado()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(historial);
    }

    @PutMapping("/gestionar/{id}")
    public ResponseEntity<?> gestionarSolicitud(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> payload) {
        try {
            String estado = (String) payload.get("estado");
            Integer idAprobador;
            
            if (payload.containsKey("empleadoId")) {
                idAprobador = (Integer) payload.get("empleadoId");
            } else if (payload.containsKey("usuarioId")) {
                idAprobador = (Integer) payload.get("usuarioId");
            } else if (payload.containsKey("aprobadorId")) {
                idAprobador = (Integer) payload.get("aprobadorId");
            } else if (payload.containsKey("idEmpleadoAprobador")) {
                idAprobador = (Integer) payload.get("idEmpleadoAprobador");
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "ID del aprobador requerido"));
            }
            
            String comentarios = (String) payload.get("comentarios");

            if (idAprobador == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "ID del aprobador requerido"));
            }

            SolicitudResponseDTO response = solicitudService.gestionarSolicitud(id, estado, idAprobador, comentarios);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/editar/{id}")
    public ResponseEntity<?> editarSolicitud(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> payload) {
        try {
            SolicitudResponseDTO response = solicitudService.editarSolicitud(id, payload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/exportar/{tipo}")
    public ResponseEntity<?> exportarSolicitudes(
            @PathVariable("tipo") String tipoReporte,
            @RequestParam(value = "empleadoId", required = false) Integer empleadoId,
            @RequestParam(value = "formato", defaultValue = "json") String formato) {
        try {
            Map<String, Object> reporte = solicitudService.exportarSolicitudes(tipoReporte, empleadoId);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(reporte);
                    
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al exportar: " + e.getMessage()));
        }
    }
}