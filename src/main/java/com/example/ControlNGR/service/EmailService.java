package com.example.ControlNGR.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {
    
    @Value("${formspree.url:https://formspree.io/f/xbddlqkb}")
    private String formspreeUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public void enviarNotificacionSolicitud(String destinatarioReal, String empleadoNombre, 
                                           String tipoSolicitud, String estado, 
                                           String comentarios, String fechaInicio, 
                                           String fechaFin) {
        try {
            String tipoFormateado = formatearTipoSolicitud(tipoSolicitud);
            String estadoFormateado = formatearEstado(estado);
            
            Map<String, String> emailData = new HashMap<>();
            emailData.put("_replyto", "sistema@controlngr.com");
            emailData.put("_subject", "Notificación de Solicitud - Sistema ControlNGR");
            emailData.put("email", destinatarioReal);
            emailData.put("empleado", empleadoNombre);
            emailData.put("tipo", tipoFormateado);
            emailData.put("estado", estadoFormateado);
            emailData.put("fecha_inicio", fechaInicio);
            emailData.put("fecha_fin", fechaFin);
            emailData.put("comentarios", comentarios != null ? comentarios : "Sin comentarios adicionales");
            emailData.put("message", 
                "Hola " + empleadoNombre + ",\n\n" +
                "📋 **ESTADO DE TU SOLICITUD**\n" +
                "─────────────────────────────\n\n" +
                "🔹 **Tipo**: " + tipoFormateado + "\n" +
                "🔹 **Estado**: " + estadoFormateado + "\n" +
                "🔹 **Período**: " + fechaInicio + " al " + fechaFin + "\n" +
                (comentarios != null && !comentarios.trim().isEmpty() ? 
                 "🔹 **Comentarios**: " + comentarios + "\n\n" : "\n") +
                "─────────────────────────────\n" +
                "📅 **Detalles Adicionales**\n" +
                "• Fecha de notificación: " + java.time.LocalDate.now() + "\n" +
                "• Sistema: ControlNGR\n\n" +
                "Si tienes alguna pregunta, contacta a tu supervisor.\n\n" +
                "Saludos,\n" +
                "✅ Sistema ControlNGR"
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(emailData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(formspreeUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅ Email enviado a: " + destinatarioReal);
            } else {
                System.out.println("❌ Error al enviar email a " + destinatarioReal + ": " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en servicio de email para " + destinatarioReal + ": " + e.getMessage());
        }
    }
    
    public void enviarNotificacionNuevaSolicitud(String destinatarioReal, String supervisorNombre, 
                                                String empleadoNombre, String tipoSolicitud,
                                                String fechaInicio, String fechaFin) {
        try {
            String tipoFormateado = formatearTipoSolicitud(tipoSolicitud);
            
            Map<String, String> emailData = new HashMap<>();
            emailData.put("_replyto", "sistema@controlngr.com");
            emailData.put("_subject", "⚠️ NUEVA SOLICITUD PENDIENTE - Sistema ControlNGR");
            emailData.put("email", destinatarioReal);
            emailData.put("supervisor", supervisorNombre);
            emailData.put("empleado", empleadoNombre);
            emailData.put("tipo", tipoFormateado);
            emailData.put("fecha_inicio", fechaInicio);
            emailData.put("fecha_fin", fechaFin);
            emailData.put("message", 
                "Hola " + supervisorNombre + ",\n\n" +
                "🔔 **NUEVA SOLICITUD PENDIENTE DE REVISIÓN**\n" +
                "─────────────────────────────────────\n\n" +
                "👤 **Empleado**: " + empleadoNombre + "\n" +
                "📋 **Tipo de Solicitud**: " + tipoFormateado + "\n" +
                "📅 **Período Solicitado**: " + fechaInicio + " al " + fechaFin + "\n" +
                "⏰ **Fecha de Solicitud**: " + java.time.LocalDate.now() + "\n\n" +
                "─────────────────────────────────────\n" +
                "📋 **ACCION REQUERIDA**\n\n" +
                "Por favor, revise esta solicitud en el sistema:\n" +
                "• Ingrese al módulo de Solicitudes\n" +
                "• Verifique disponibilidad y políticas\n" +
                "• Aprobe o rechace según corresponda\n" +
                "• Agregue comentarios si es necesario\n\n" +
                "📊 **ESTADÍSTICAS RÁPIDAS**\n" +
                "• Tiempo promedio de respuesta: 24-48 horas\n" +
                "• Urgencia: Media\n\n" +
                "Saludos,\n" +
                "✅ Sistema ControlNGR"
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(emailData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(formspreeUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅ Notificación enviada a supervisor: " + destinatarioReal);
            } else {
                System.out.println("❌ Error enviando a supervisor " + destinatarioReal);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando notificación a " + destinatarioReal + ": " + e.getMessage());
        }
    }
    
    public void enviarNotificacionCambioPassword(String destinatarioReal, String empleadoNombre) {
        try {
            Map<String, String> emailData = new HashMap<>();
            emailData.put("_replyto", "sistema@controlngr.com");
            emailData.put("_subject", "🔐 Cambio de Contraseña - Sistema ControlNGR");
            emailData.put("email", destinatarioReal);
            emailData.put("empleado", empleadoNombre);
            emailData.put("message", 
                "Hola " + empleadoNombre + ",\n\n" +
                "✅ **CAMBIO DE CONTRASEÑA EXITOSO**\n" +
                "─────────────────────────────────\n\n" +
                "Se ha completado exitosamente el cambio de tu contraseña en el sistema ControlNGR.\n\n" +
                "📋 **DETALLES**\n" +
                "• Fecha del cambio: " + java.time.LocalDate.now() + "\n" +
                "• Hora: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
                "• Sistema: ControlNGR v1.0\n\n" +
                "⚠️ **IMPORTANTE**\n" +
                "Si no realizaste este cambio, por favor:\n" +
                "1. Contacta inmediatamente al administrador\n" +
                "2. Cambia tu contraseña nuevamente\n" +
                "3. Reporta cualquier actividad sospechosa\n\n" +
                "🔒 **CONSEJOS DE SEGURIDAD**\n" +
                "• Usa contraseñas fuertes\n" +
                "• No compartas tu contraseña\n" +
                "• Cambia periódicamente tu contraseña\n\n" +
                "Saludos,\n" +
                "✅ Sistema ControlNGR"
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(emailData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(formspreeUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅ Notificación de cambio de password enviada a: " + destinatarioReal);
            } else {
                System.out.println("❌ Error enviando notificación a " + destinatarioReal);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando notificación de cambio de password a " + destinatarioReal + ": " + e.getMessage());
        }
    }
    
    public void enviarNotificacionActualizacionPerfil(String destinatarioReal, String empleadoNombre) {
        try {
            Map<String, String> emailData = new HashMap<>();
            emailData.put("_replyto", "sistema@controlngr.com");
            emailData.put("_subject", "📝 Actualización de Perfil - Sistema ControlNGR");
            emailData.put("email", destinatarioReal);
            emailData.put("empleado", empleadoNombre);
            emailData.put("message", 
                "Hola " + empleadoNombre + ",\n\n" +
                "✅ **PERFIL ACTUALIZADO EXITOSAMENTE**\n" +
                "─────────────────────────────────────\n\n" +
                "Tu información de perfil ha sido actualizada correctamente en el sistema ControlNGR.\n\n" +
                "📋 **DETALLES**\n" +
                "• Fecha de actualización: " + java.time.LocalDate.now() + "\n" +
                "• Hora: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
                "• Sistema: ControlNGR v1.0\n\n" +
                "🔍 **QUÉ SE ACTUALIZÓ**\n" +
                "• Información personal\n" +
                "• Datos de contacto\n" +
                "• Preferencias del perfil\n\n" +
                "⚠️ **VERIFICACIÓN**\n" +
                "Por favor verifica que toda tu información esté correcta:\n" +
                "1. Ingresa a tu perfil en el sistema\n" +
                "2. Revisa todos los datos\n" +
                "3. Reporta cualquier error\n\n" +
                "Saludos,\n" +
                "✅ Sistema ControlNGR"
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(emailData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(formspreeUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅ Notificación de actualización de perfil enviada a: " + destinatarioReal);
            } else {
                System.out.println("❌ Error enviando notificación a " + destinatarioReal);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando notificación de actualización de perfil a " + destinatarioReal + ": " + e.getMessage());
        }
    }
    
    private String formatearTipoSolicitud(String tipo) {
        switch(tipo.toLowerCase()) {
            case "vacaciones": return "🏖️ Vacaciones";
            case "permiso": return "📋 Permiso Personal";
            case "descanso": return "🏥 Descanso Médico";
            case "compensacion": return "⏰ Compensación de Horas";
            case "licencia": return "📄 Licencia Especial";
            default: return "📝 " + tipo;
        }
    }
    
    private String formatearEstado(String estado) {
        switch(estado.toLowerCase()) {
            case "aprobado": return "✅ Aprobado";
            case "rechazado": return "❌ Rechazado";
            case "pendiente": return "⏳ Pendiente";
            case "en_revision": return "🔍 En Revisión";
            default: return estado;
        }
    }
}