package com.glmx.tools.service;

import com.glmx.tools.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebSocketAuthService {

    @Autowired
    private JwtUtil jwtUtil;

    public String authenticate(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUsernameFromToken(token);
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }
}
