package com.blog.controller;

import com.blog.dto.AuthPayload;
import com.blog.entity.User;
import com.blog.security.JwtUtil;
import com.blog.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @MutationMapping
    public AuthPayload register(@Argument String username, @Argument String email, @Argument String password) {
        return userService.register(username, email, password);
    }

    @MutationMapping
    public AuthPayload login(@Argument String username, @Argument String password) {
        return userService.login(username, password);
    }

    @QueryMapping
    public User me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.findByUsername(authentication.getName());
    }
}
