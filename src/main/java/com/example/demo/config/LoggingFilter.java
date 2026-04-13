package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Generate a unique Request ID (Full UUID)
        String requestId = UUID.randomUUID().toString();
        // Cụ thể hóa DTrace ID và Vị trí rõ ràng
        MDC.put("requestId", requestId);
        MDC.put("partnerURL", req.getRequestURL().toString());

        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Incoming Request: Method={}, URI={}, ClientIP={}", 
                    req.getMethod(), req.getRequestURI(), req.getRemoteAddr());
            
            chain.doFilter(request, response);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = res.getStatus();
            String message = String.format("Outgoing Response: Method=%s, URI=%s, Status=%d, Duration=%dms", 
                    req.getMethod(), req.getRequestURI(), status, duration);
            
            if (status >= 500) {
                logger.error(message); // Server Error
            } else if (status >= 400) {
                logger.warn(message); // Client Error
            } else {
                logger.info(message); // Success
            }
            
            MDC.remove("requestId");
            MDC.remove("partnerURL");
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
