package com.trizenai.photoshare.config;

import com.trizenai.photoshare.model.Role;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the two demo accounts referenced in the submission deliverables
 * (Demo Admin credentials / Demo Team Member credentials) so the evaluator
 * can log in immediately. Only runs in the "dev"/default profile - disable
 * or change credentials before a real production deployment.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    @Profile("!prod")
    public CommandLineRunner seedDemoUsers(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@trizen-demo.com")) {
                userRepository.save(User.builder()
                        .name("Demo Admin")
                        .email("admin@trizen-demo.com")
                        .passwordHash(encoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .build());
            }
            if (!userRepository.existsByEmail("member@trizen-demo.com")) {
                userRepository.save(User.builder()
                        .name("Demo Team Member")
                        .email("member@trizen-demo.com")
                        .passwordHash(encoder.encode("Member@123"))
                        .role(Role.TEAM_MEMBER)
                        .build());
            }
            log.info("Demo accounts ready -> admin@trizen-demo.com / Admin@123 | member@trizen-demo.com / Member@123");
        };
    }
}
