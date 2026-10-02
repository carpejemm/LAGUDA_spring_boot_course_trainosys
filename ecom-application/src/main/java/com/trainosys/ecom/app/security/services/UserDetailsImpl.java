package com.trainosys.ecom.app.security.services;

import com.trainosys.ecom.app.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * USER DETAILS IMPL: our {@link User}, in the shape Spring Security understands.
 *
 * <p>Spring Security doesn't know our {@code User} entity (firstName, address, cart...).
 * It only knows the {@link UserDetails} interface: a username, a password hash and a list of
 * authorities (roles). This class is the adapter between the two:
 *
 * <pre>
 *   User (JPA entity, database)          UserDetailsImpl (Spring Security)
 *   ---------------------------          ---------------------------------
 *   id         2                    ->   id            2
 *   username   "user1"              ->   username      "user1"
 *   email      "user1@..."          ->   email         "user1@..."
 *   password   "$2a$10$..."         ->   password      "$2a$10$..."   (only used during sign in)
 *   role       CUSTOMER             ->   authorities   [ROLE_CUSTOMER]
 * </pre>
 *
 * <p>Where you meet it:
 * <ul>
 *   <li>UserDetailsServiceImpl builds it from the database (sign in, and every request in AuthTokenFilter).</li>
 *   <li>It is the "principal" of the logged-in request. Controllers get it with
 *       {@code @AuthenticationPrincipal UserDetailsImpl userDetails}
 *       (CartController/OrderController use {@code userDetails.getId()} instead of an X-User-ID header).</li>
 * </ul>
 *
 * <p>All fields are {@code final}: built once, never changed. No setters on purpose.
 */
public class UserDetailsImpl implements UserDetails {
    private final Long id;
    private final String username;
    private final String email;
    /** The BCrypt hash. Spring compares the typed password against it at sign in. */
    private final String password;
    /** The roles, e.g. [ROLE_ADMIN]. hasRole("ADMIN") in SecurityConfig checks this list. */
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String email, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    /**
     * Converts a database {@link User} into a UserDetailsImpl.
     *
     * <p>Roles: Spring Security calls them "authorities" and, by convention, a role's authority
     * starts with {@code ROLE_}. {@code hasRole("ADMIN")} looks for {@code "ROLE_ADMIN"}.
     * Forget the prefix and admins get 403 on admin URLs.
     *
     * <p>Our User has ONE role (the UserRole enum), so the list has one item.
     * A user with several roles would get one SimpleGrantedAuthority per role.
     */
    public static UserDetailsImpl build(User user) {
        // CUSTOMER -> ROLE_CUSTOMER, ADMIN -> ROLE_ADMIN (hasRole("ADMIN") looks for ROLE_ADMIN)
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities);
    }

    // ---------------------------------------------------------------------------------------------
    // Extra getters: not part of UserDetails, our own additions for controllers
    // ---------------------------------------------------------------------------------------------

    /** Database id of the logged-in user. Cart and Orders use it to find "my" cart. */
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    // ---------------------------------------------------------------------------------------------
    // UserDetails methods: Spring Security calls these
    // ---------------------------------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired() and isEnabled()
    // are default methods of UserDetails that return true. Override one to block an account,
    // e.g. isEnabled() { return emailVerified; } -> sign in fails until the email is verified.
}
