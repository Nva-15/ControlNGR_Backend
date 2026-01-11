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

    // Crear una solicitud
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

    // Ver mis solicitudes
    @GetMapping("/mis-solicitudes/{empleadoId}")
    public ResponseEntity<List<SolicitudResponseDTO>> getMisSolicitudes(@PathVariable("empleadoId") Integer empleadoId) {
        return ResponseEntity.ok(solicitudService.obtenerMisSolicitudes(empleadoId));
    }

    // Ver pendientes
    @GetMapping("/pendientes")
    public ResponseEntity<List<SolicitudResponseDTO>> getPendientes() {
        return ResponseEntity.ok(solicitudService.obtenerPendientes());
    }
    
    // Ver todas
    @GetMapping("/todas")
    public ResponseEntity<List<SolicitudResponseDTO>> getTodas() {
        return ResponseEntity.ok(solicitudService.obtenerTodas());
    }

    // Aprobar o Rechazar
    // Payload esperado: { "estado": "aprobado", "empleadoId": 5 } <-- ID del empleado que aprueba
    @PutMapping("/gestionar/{id}")
    public ResponseEntity<?> gestionarSolicitud(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> payload) {
        try {
            String estado = (String) payload.get("estado");
            Integer idAprobador;
            if (payload.containsKey("empleadoId")) {
                idAprobador = (Integer) payload.get("empleadoId");
            } else {
                idAprobador = (Integer) payload.get("usuarioId");
            }

            if (idAprobador == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "ID del aprobador requerido"));
            }

            SolicitudResponseDTO response = solicitudService.gestionarSolicitud(id, estado, idAprobador);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}