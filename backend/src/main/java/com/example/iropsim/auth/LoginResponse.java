package com.example.iropsim.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 登录响应DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private String id;
    private String username;
    private String email;
    private List<String> roles;

    public LoginResponse(String accessToken, String id, String username, String email, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}
