package com.gcivil.user.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcivil.user.domain.Role;
import com.gcivil.user.domain.User;
import com.gcivil.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final SecretKey testKey = Keys.hmacShaKeyFor(
            "test-jwt-secret-key-must-be-at-least-32b".getBytes(StandardCharsets.UTF_8)
    );

    @Test
    void signupCreatesCitizen() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupFixture(
                                "citizen01",
                                "Civil!2026#",
                                "홍길동",
                                "citizen01@email.com",
                                "010-1234-5678",
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("citizen01"))
                .andExpect(jsonPath("$.data.role").value("CITIZEN"));
    }

    @Test
    void signupRejectsDuplicateLoginId() throws Exception {
        userRepository.save(new User(
                "citizen01",
                passwordEncoder.encode("Civil!2026#"),
                "홍길동",
                "dup@email.com",
                "010-1111-2222",
                Role.CITIZEN,
                null,
                false,
                true
        ));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupFixture(
                                "citizen01",
                                "Civil!2026#",
                                "김길동",
                                "citizen02@email.com",
                                "010-1234-5678",
                                true
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void loginReturnsTokens() throws Exception {
        userRepository.save(new User(
                "citizen01",
                passwordEncoder.encode("Civil!2026#"),
                "홍길동",
                "citizen01@email.com",
                "010-1234-5678",
                Role.CITIZEN,
                null,
                true,
                true
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginFixture(
                                "citizen01",
                                "Civil!2026#"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.loginId").value("citizen01"))
                .andExpect(jsonPath("$.data.user.role").value("CITIZEN"));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        userRepository.save(new User(
                "citizen01",
                passwordEncoder.encode("Civil!2026#"),
                "홍길동",
                "citizen01@email.com",
                "010-1234-5678",
                Role.CITIZEN,
                null,
                true,
                true
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginFixture(
                                "citizen01",
                                "Wrong!2026#"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void logoutAcceptsValidBearerToken() throws Exception {
        String token = Jwts.builder()
                .subject("101")
                .issuer("minwonon-auth")
                .claim("userId", 101L)
                .claim("tokenType", "REFRESH")
                .signWith(testKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutFixture(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loggedOut").value(true));
    }

    private record SignupFixture(
            String loginId,
            String password,
            String name,
            String email,
            String phone,
            boolean emailNotifyAgreed
    ) {
    }

    private record LoginFixture(
            String loginId,
            String password
    ) {
    }

    private record LogoutFixture(
            String refreshToken
    ) {
    }
}
