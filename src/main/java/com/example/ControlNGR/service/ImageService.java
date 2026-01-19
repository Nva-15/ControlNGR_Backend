package com.example.ControlNGR.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageService {
    
    @Value("${app.storage.location}")
    private String storageLocation;
    
    // Validar extensión de archivo
    private boolean esExtensionValida(String filename) {
        String[] extensionesValidas = {".png", ".jpg", ".jpeg", ".gif", ".bmp"};
        String extension = filename.toLowerCase();
        
        for (String ext : extensionesValidas) {
            if (extension.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    // Generar nombre único para archivo
    private String generarNombreArchivo(String nombreBase, String extension) {
        String nombreLimpio = nombreBase.toLowerCase()
                .replaceAll("[^a-z0-9áéíóúüñ]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
        
        String nombreArchivo = nombreLimpio + extension;
        Path rutaCompleta = Paths.get(storageLocation.replace("file:///", ""), nombreArchivo);
        
        // Si ya existe, agregar número
        int contador = 1;
        while (Files.exists(rutaCompleta)) {
            nombreArchivo = nombreLimpio + "_" + contador + extension;
            rutaCompleta = Paths.get(storageLocation.replace("file:///", ""), nombreArchivo);
            contador++;
        }
        
        return nombreArchivo;
    }
    
    // Subir imagen de perfil
    public String subirImagenPerfil(MultipartFile archivo, String nombreEmpleado) throws IOException {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        
        // Validar tipo de archivo
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !esExtensionValida(nombreOriginal)) {
            throw new IllegalArgumentException("Formato de archivo no permitido. Use PNG, JPG, JPEG, GIF o BMP");
        }
        
        // Validar tamaño (máx 5MB)
        if (archivo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo es demasiado grande. Máximo 5MB");
        }
        
        // Obtener extensión
        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
        
        // Generar nombre de archivo
        String nombreArchivo = generarNombreArchivo(nombreEmpleado, extension);
        
        // Ruta completa de destino
        String rutaDestino = storageLocation.replace("file:///", "") + nombreArchivo;
        
        // Crear directorio si no existe
        Path directorio = Paths.get(storageLocation.replace("file:///", ""));
        if (!Files.exists(directorio)) {
            Files.createDirectories(directorio);
        }
        
        // Copiar archivo
        Path ruta = Paths.get(rutaDestino);
        Files.copy(archivo.getInputStream(), ruta, StandardCopyOption.REPLACE_EXISTING);
        
        // Retornar solo el nombre del archivo para guardar en BD
        return "img/" + nombreArchivo;
    }
    
    // Eliminar imagen anterior si existe
    public void eliminarImagenAnterior(String nombreArchivo) throws IOException {
        if (nombreArchivo != null && !nombreArchivo.isEmpty() && !nombreArchivo.equals("img/perfil.png")) {
            String rutaCompleta = storageLocation.replace("file:///", "") + 
                nombreArchivo.replace("img/", "");
            Path ruta = Paths.get(rutaCompleta);
            
            if (Files.exists(ruta)) {
                Files.delete(ruta);
            }
        }
    }
    
    // Verificar si imagen existe
    public boolean existeImagen(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            return false;
        }
        
        String rutaCompleta = storageLocation.replace("file:///", "") + 
            nombreArchivo.replace("img/", "");
        Path ruta = Paths.get(rutaCompleta);
        
        return Files.exists(ruta);
    }
}