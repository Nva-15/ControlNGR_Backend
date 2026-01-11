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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    // NO USAMOS UsuarioRepository

    @Transactional
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request) {
        if (request.getFechaInicio().isAfter(request.getFechaFin())) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha fin");
        }

        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        Solicitud solicitud = new Solicitud();
        solicitud.setEmpleado(empleado);
        solicitud.setTipo(request.getTipo());
        solicitud.setFechaInicio(request.getFechaInicio());
        solicitud.setFechaFin(request.getFechaFin());
        solicitud.setMotivo(request.getMotivo());
        solicitud.setEstado("pendiente");
        solicitud.setFechaSolicitud(LocalDateTime.now());

        Solicitud saved = solicitudRepository.save(solicitud);
        return new SolicitudResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerMisSolicitudes(Integer empleadoId) {
        return solicitudRepository.findByEmpleadoIdOrderByFechaSolicitudDesc(empleadoId).stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerPendientes() {
        return solicitudRepository.findSolicitudesPendientes().stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerTodas() {
        return solicitudRepository.findAll().stream()
                .map(SolicitudResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudResponseDTO gestionarSolicitud(Integer solicitudId, String nuevoEstado, Integer idEmpleadoAprobador) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"pendiente".equals(solicitud.getEstado())) {
            throw new RuntimeException("La solicitud ya fue procesada anteriormente");
        }

        // CORREGIDO: Buscamos al aprobador en la tabla EMPLEADOS
        Empleado aprobador = empleadoRepository.findById(idEmpleadoAprobador)
                .orElseThrow(() -> new RuntimeException("Empleado aprobador no encontrado"));
       

        solicitud.setEstado(nuevoEstado.toLowerCase()); 
        solicitud.setAprobadoPor(aprobador); // Asignamos el objeto Empleado
        solicitud.setFechaAprobacion(LocalDateTime.now());

        Solicitud updated = solicitudRepository.save(solicitud);
        return new SolicitudResponseDTO(updated);
    }
}