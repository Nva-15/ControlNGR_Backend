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
    //Configuración para servir archivos estáticos del propio JAR
    registry.addResourceHandler("/**")
            .addResourceLocations(
                "classpath:/static/",
                "classpath:/public/",
                "classpath:/resources/",
                "classpath:/META-INF/resources/"
            )
            .setCachePeriod(0);
    
    // 2. Configuración para servir imágenes desde la RUTA EXTERNA
    // Eliminar "file:///" del path para Spring
    String rutaLocal = storageLocation.replace("file:///", "");
    System.out.println("📂 Mapeando /img/** a: " + rutaLocal);
    
    registry.addResourceHandler("/img/**")
            .addResourceLocations("file:" + rutaLocal)
            .setCachePeriod(0);
}
}