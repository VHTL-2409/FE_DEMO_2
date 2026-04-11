package com.example.demo.realtime;

import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Requires {@code access_token} query parameter (SockJS cannot send Authorization header reliably).
 * Rejects the handshake when the JWT is missing or invalid.
 */
@Component
public class WebSocketJwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public WebSocketJwtHandshakeInterceptor(JwtService jwtService,
            CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("access_token");
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(token, userDetails)) {
                return false;
            }
            attributes.put("websocketUsername", username);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
