package com.trizenai.photoshare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Exposes the local ./uploads directory over HTTP so photo URLs resolve in
 * dev/eval without needing real cloud storage. Not used when
 * app.storage.provider=s3 (S3 URLs are served directly from AWS).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local.dir:./uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsDir + "/");
    }
}
