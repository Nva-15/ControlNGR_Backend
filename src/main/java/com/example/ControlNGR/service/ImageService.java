package com.example.ControlNGR.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    private static final int MAX_WIDTH = 400;
    private static final int MAX_HEIGHT = 400;
    private static final float JPEG_QUALITY = 0.85f;

    @Value("${app.storage.location}")
    private String storageLocation;

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

    private String generarNombreArchivo(String nombreBase, String extension) {
        String nombreLimpio = nombreBase.toLowerCase()
                .replaceAll("[^a-z0-9áéíóúüñ]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");

        String nombreArchivo = nombreLimpio + extension;
        Path rutaCompleta = Paths.get(storageLocation.replace("file:///", ""), nombreArchivo);

        int contador = 1;
        while (Files.exists(rutaCompleta)) {
            nombreArchivo = nombreLimpio + "_" + contador + extension;
            rutaCompleta = Paths.get(storageLocation.replace("file:///", ""), nombreArchivo);
            contador++;
        }
        return nombreArchivo;
    }

    private BufferedImage redimensionarImagen(BufferedImage original) {
        int anchoOriginal = original.getWidth();
        int altoOriginal = original.getHeight();

        if (anchoOriginal <= MAX_WIDTH && altoOriginal <= MAX_HEIGHT) {
            return original;
        }

        double escala = Math.min((double) MAX_WIDTH / anchoOriginal, (double) MAX_HEIGHT / altoOriginal);
        int nuevoAncho = (int) (anchoOriginal * escala);
        int nuevoAlto = (int) (altoOriginal * escala);

        BufferedImage redimensionada = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = redimensionada.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        g2d.dispose();

        logger.info("Imagen redimensionada: {}x{} -> {}x{}", anchoOriginal, altoOriginal, nuevoAncho, nuevoAlto);
        return redimensionada;
    }

    private byte[] comprimirImagen(BufferedImage imagen, String formato) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if ("jpg".equals(formato) || "jpeg".equals(formato)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(imagen, null, null), param);
                }
                writer.dispose();
            }
        } else {
            ImageIO.write(imagen, formato, baos);
        }
        return baos.toByteArray();
    }

    public String subirImagenPerfil(MultipartFile archivo, String nombreEmpleado) throws IOException {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo esta vacio");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !esExtensionValida(nombreOriginal)) {
            throw new IllegalArgumentException("Formato de archivo no permitido. Use PNG, JPG, JPEG, GIF o BMP");
        }

        if (archivo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo es demasiado grande. Maximo 5MB");
        }

        String extensionOriginal = nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).toLowerCase();
        String extensionFinal = extensionOriginal.equals("png") ? ".png" : ".jpg";
        String formatoSalida = extensionOriginal.equals("png") ? "png" : "jpg";

        BufferedImage imagenOriginal = ImageIO.read(archivo.getInputStream());
        if (imagenOriginal == null) {
            throw new IllegalArgumentException("No se pudo leer la imagen");
        }

        long tamanoOriginal = archivo.getSize();
        BufferedImage imagenRedimensionada = redimensionarImagen(imagenOriginal);
        byte[] imagenComprimida = comprimirImagen(imagenRedimensionada, formatoSalida);

        logger.info("Imagen optimizada: {} KB -> {} KB", tamanoOriginal / 1024, imagenComprimida.length / 1024);

        String nombreArchivo = generarNombreArchivo(nombreEmpleado, extensionFinal);
        String rutaDestino = storageLocation.replace("file:///", "") + nombreArchivo;

        Path directorio = Paths.get(storageLocation.replace("file:///", ""));
        if (!Files.exists(directorio)) {
            Files.createDirectories(directorio);
        }

        Path ruta = Paths.get(rutaDestino);
        Files.write(ruta, imagenComprimida);

        return "img/" + nombreArchivo;
    }

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
