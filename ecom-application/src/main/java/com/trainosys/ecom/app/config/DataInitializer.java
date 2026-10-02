package com.trainosys.ecom.app.config;

import com.trainosys.ecom.app.model.User;
import com.trainosys.ecom.app.model.UserRole;
import com.trainosys.ecom.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DATA INITIALIZER: creates the accounts you log in with.
 *
 * <p>H2 runs in memory ({@code jdbc:h2:mem:test}, {@code ddl-auto: create}), so the database is
 * EMPTY on every start. Without this class there would be nobody to sign in as, and no way to get
 * an ADMIN (sign up always creates customers).
 *
 * <pre>
 *   username   password      role       use it for
 *   --------   -----------   --------   ------------------------------------------
 *   admin      admin123      ADMIN      products (create/update/delete), /api/users
 *   user1      password123   CUSTOMER   cart, orders; gets 403 on admin URLs
 * </pre>
 *
 * <p>{@code CommandLineRunner}: Spring Boot calls {@link #run} once, right after the app has started
 * (database tables created, all beans ready).
 *
 * <p>Training data only. Real applications never ship hard-coded passwords.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    /** The BCrypt encoder from SecurityConfig: the same one sign up and sign in use. */
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUser("admin", "admin@trainosys.com", "admin123", UserRole.ADMIN);
        createUser("user1", "user1@trainosys.com", "password123", UserRole.CUSTOMER);
    }

    /**
     * Saves one user, unless the username already exists (safe to run twice, e.g. with a database
     * that is NOT erased on restart).
     *
     * <p>{@code passwordEncoder.encode(rawPassword)}: the database gets {@code $2a$10$...}, never
     * "admin123". Saving the raw password would make sign in fail: BCrypt can't match a stored
     * value that isn't a BCrypt hash (console: "Encoded password does not look like BCrypt").
     */
    private void createUser(String username, String email, String rawPassword, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }
}
