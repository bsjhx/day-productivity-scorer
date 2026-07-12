package com.bsjhx.dayproductivityscore;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfiguration {

    @Bean
    public ServletRegistrationBean<?> h2servletRegistration() {
        try {
            Class<?> webServletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            Object webServlet = webServletClass.getDeclaredConstructor().newInstance();
            ServletRegistrationBean<?> registration = new ServletRegistrationBean<>((jakarta.servlet.Servlet) webServlet);
            registration.addUrlMappings("/h2-console/*");
            return registration;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create H2 console servlet", e);
        }
    }
}
