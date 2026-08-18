package com.gcivil.user.service;

import com.gcivil.user.domain.Role;
import com.gcivil.user.domain.User;
import com.gcivil.user.dto.LoginRequest;
import com.gcivil.user.dto.LoginResponse;
import com.gcivil.user.dto.LogoutRequest;
import com.gcivil.user.dto.LogoutResponse;
import com.gcivil.user.dto.SignupRequest;
import com.gcivil.user.dto.SignupResponse;
import com.gcivil.user.exception.ApiException;
import com.gcivil.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String loginId = request.getLoginId().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByLoginId(loginId)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 사용 중인 loginId 입니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 사용 중인 email 입니다.");
        }

        User user = userRepository.saveAndFlush(new User(
                loginId,
                passwordEncoder.encode(request.getPassword()),
                request.getName().trim(),
                email,
                request.getPhone().trim(),
                Role.CITIZEN,
                null,
                Boolean.TRUE.equals(request.getEmailNotifyAgreed()),
                true
        ));

        return new SignupResponse(
                user.getId(),
                user.getLoginId(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId().trim())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "아이디 또는 비밀번호가 일치하지 않습니다."
                ));

        if (!user.isActive() || !matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "아이디 또는 비밀번호가 일치하지 않습니다."
            );
        }

        return new LoginResponse(
                jwtTokenService.createAccessToken(user),
                jwtTokenService.createRefreshToken(user),
                new LoginResponse.UserSummary(
                        user.getId(),
                        user.getLoginId(),
                        user.getRole().name(),
                        user.getDepartmentId()
                )
        );
    }

    public LogoutResponse logout(LogoutRequest request) {
        jwtTokenService.parseAndValidateRefreshToken(request.getRefreshToken().trim());
        return new LogoutResponse(true);
    }

    private boolean matches(String rawPassword, String storedPasswordHash) {
        if (storedPasswordHash != null && storedPasswordHash.startsWith("{noop}")) {
            return storedPasswordHash.substring("{noop}".length()).equals(rawPassword);
        }
        return passwordEncoder.matches(rawPassword, storedPasswordHash);
    }
}
