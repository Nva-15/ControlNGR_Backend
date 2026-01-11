package com.example.ControlNGR.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Inyectamos la ruta desde application.properties
    @Value("${app.storage.location}")
    private String storageLocation;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Configuración para servir archivos estáticos del propio JAR (Frontend, CSS, JS)
        registry.addResourceHandler("/**")
                .addResourceLocations(
                    "classpath:/static/",
                    "classpath:/public/",
                    "classpath:/resources/",
                    "classpath:/META-INF/resources/"
                )
                .setCachePeriod(0);
        
        // 2. Configuración para servir imágenes desde la RUTA EXTERNA
        // Esto mapea http://localhost:8080/img/foto.png -> D:/Mis Proyectos personales/img/foto.png
        System.out.println("📂 Mapeando /img/** a: " + storageLocation); // Log para verificar
        
        registry.addResourceHandler("/img/**")
                .addResourceLocations(storageLocation)
                .setCachePeriod(0); // Sin caché para desarrollo (puedes quitarlo en prod)
    }
}