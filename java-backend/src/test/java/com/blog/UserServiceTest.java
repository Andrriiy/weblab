package com.blog;

import com.blog.dto.AuthPayload;
import com.blog.entity.User;
import com.blog.repository.UserRepository;
import com.blog.security.JwtUtil;
import com.blog.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private UserService userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtUtil);
        testUser = new User("alice", "alice@example.com", "encoded_password");
        testUser.setId(1L);
    }

    @Test
    void register_newUser_returnsAuthPayload() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt_token");

        AuthPayload result = userService.register("alice", "alice@example.com", "password123");

        assertNotNull(result);
        assertEquals("jwt_token", result.getToken());
        assertNotNull(result.getUser());
    }

    @Test
    void register_existingUsername_throwsException() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.register("alice", "alice@example.com", "password123"));
    }

    @Test
    void register_existingEmail_throwsException() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.register("alice", "alice@example.com", "password123"));
    }

    @Test
    void login_validCredentials_returnsAuthPayload() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt_token");

        AuthPayload result = userService.login("alice", "password123");

        assertNotNull(result);
        assertEquals("jwt_token", result.getToken());
    }

    @Test
    void login_wrongPassword_throwsException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded_password")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.login("alice", "wrong"));
    }

    @Test
    void login_nonExistingUser_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.login("ghost", "password123"));
    }

    @Test
    void findByUsername_existing_returnsUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        User result = userService.findByUsername("alice");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }
}
