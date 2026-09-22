package com.pukaar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward (not redirect): behind nginx /pukaar, a redirect to /admin/index.html
        // loses the prefix and lands on https://pukaaralert.com/admin → 404.
        // Nginx also strips .html (…/index.html → …/index), so keep the URL at /admin.
        registry.addViewController("/admin").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }
}
