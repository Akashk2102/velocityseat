package com.velocityseat.modules.user.service;

import com.velocityseat.common.exception.BadRequestException;
import com.velocityseat.modules.user.dto.AuthResponse;
import com.velocityseat.modules.user.dto.LoginRequest;
import com.velocityseat.modules.user.dto.RegisterRequest;
import com.velocityseat.modules.user.model.Role;
import com.velocityseat.modules.user.model.User;
import com.velocityseat.modules.user.repository.UserRepository;
import com.velocityseat.security.CustomUserDetails;
import com.velocityseat.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("alex@velocityseat.com")
                .password("hashed_pwd_123")
                .fullName("Alex Morgan")
                .phoneNumber("+1234567890")
                .role(Role.ROLE_CUSTOMER)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new customer")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alex@velocityseat.com")
                .password("plain_pwd_123")
                .fullName("Alex Morgan")
                .phoneNumber("+1234567890")
                .build();

        when(userRepository.existsByEmail("alex@velocityseat.com")).thenReturn(false);
        when(passwordEncoder.encode("plain_pwd_123")).thenReturn("hashed_pwd_123");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(any(CustomUserDetails.class), eq(1L), eq("ROLE_CUSTOMER")))
                .thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getUser().getEmail()).isEqualTo("alex@velocityseat.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alex@velocityseat.com")
                .password("plain_pwd_123")
                .fullName("Alex Morgan")
                .build();

        when(userRepository.existsByEmail("alex@velocityseat.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate user and return token")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("alex@velocityseat.com")
                .password("plain_pwd_123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("alex@velocityseat.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(any(CustomUserDetails.class), eq(1L), eq("ROLE_CUSTOMER")))
                .thenReturn("valid.jwt.token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("valid.jwt.token");
        assertThat(response.getUser().getFullName()).isEqualTo("Alex Morgan");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password does not match")
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("alex@velocityseat.com")
                .password("wrong_password")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                .hasMessage("Bad credentials");

        verify(jwtService, never()).generateToken(any(), any(), any());
    }
}
