package com.example.iropsim.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 用户信息DTO
 */
@Data
@AllArgsConstructor
public class UserInfo {

    private String id;
    private String username;
    private String email;
    private List<String> roles;
}
