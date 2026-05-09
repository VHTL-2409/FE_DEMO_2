package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ProctorEvidenceImageConfig implements WebMvcConfigurer {

    private final Path evidenceImageDir;

    public ProctorEvidenceImageConfig(
            @Value("${app.proctor.evidence-image-dir:${user.dir}/data/proctor-evidence}") String evidenceImageDir
    ) {
        this.evidenceImageDir = Path.of(evidenceImageDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = evidenceImageDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/evidence-images/**")
                .addResourceLocations(location);
    }
}
