package com.gcivil.user.service;

import com.gcivil.user.domain.User;
import com.gcivil.user.dto.InternalUserResponse;
import com.gcivil.user.dto.InternalNotificationPreferenceResponse;
import com.gcivil.user.exception.ApiException;
import com.gcivil.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InternalUserLookupService {

    private final UserRepository userRepository;

    public InternalUserLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public InternalUserResponse getUser(Long userId) {
        User user = findUser(userId);

        return new InternalUserResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getRole().name(),
                user.getDepartmentId(),
                user.isActive()
        );
    }

    public InternalNotificationPreferenceResponse getNotificationPreference(Long userId) {
        User user = findUser(userId);
        return new InternalNotificationPreferenceResponse(
                user.getId(), user.getEmail(), user.isEmailNotifyAgreed(), user.isActive());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));
    }
}
