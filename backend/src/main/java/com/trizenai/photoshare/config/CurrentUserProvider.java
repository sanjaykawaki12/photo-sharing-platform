package com.trizenai.photoshare.config;

import com.trizenai.photoshare.exception.ApiExceptions.NotFoundException;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user no longer exists"));
    }
}
