package com.trainosys.ecom.app.security.services;

import com.trainosys.ecom.app.model.User;
import com.trainosys.ecom.app.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * USER DETAILS SERVICE IMPL: "find this username in OUR database" (slide 8).
 *
 * <p>Spring Security doesn't know where our users live (H2, MySQL, LDAP...). It asks through
 * the {@link UserDetailsService} interface, and this class answers from {@code user_table}.
 * Same interface + Impl pattern as our own services; this time the interface comes from Spring.
 *
 * <p>Called in two places:
 * <pre>
 *   1. SIGN IN    AuthController -> AuthenticationManager -> DaoAuthenticationProvider
 *                   -> loadUserByUsername("admin") -> then PasswordEncoder.matches(...)
 *   2. EVERY REQUEST WITH A TOKEN
 *                 AuthTokenFilter -> loadUserByUsername(username from the token)
 *                   -> fresh roles from the database on every request
 * </pre>
 *
 * <p>Because this bean exists, Spring Boot no longer creates its default "user" with a generated
 * password (that line disappears from the console in step 2 of the guide).
 *
 * <p>{@code @Service}: Spring creates one instance and injects it into SecurityConfig.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by username and converts it to {@link UserDetailsImpl}.
     *
     * <p>{@code findByUsername} is a Spring Data method: from the method name it writes
     * {@code SELECT * FROM user_table WHERE username = ?}.
     *
     * <p>Not found -> {@link UsernameNotFoundException}. At sign in Spring turns it into
     * "Bad credentials" (the same message as a wrong password, so nobody can probe which usernames
     * exist). In AuthTokenFilter it means the token's user no longer exists -> not logged in -> 401.
     *
     * <p>{@code @Transactional}: keeps the database session open while the User is read and converted.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        return UserDetailsImpl.build(user);
    }
}
