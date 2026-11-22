package com.cashflow.demo.config;

import com.cashflow.demo.repository.UserRepository;
import com.cashflow.demo.security.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    SecretKey jwtKey(JwtProperties properties) {
        byte[] bytes = Base64.getDecoder().decode(properties.secret());
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes em Base64");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, JwtProperties properties) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var audience = new JwtClaimValidator<List<String>>("aud", value ->
                value != null && value.contains(properties.audience()));
        var subject = new JwtClaimValidator<String>("sub", value -> value != null && value.matches("[1-9][0-9]*"));
        var expires = new JwtClaimValidator<Object>("exp", Objects::nonNull);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), audience, subject, expires));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errors,
                                             UserRepository users) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(errors).accessDeniedHandler(errors)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                            Long id;
                            try { id = Long.valueOf(token.getSubject()); }
                            catch (NumberFormatException ex) { throw new OAuth2AuthenticationException("invalid_token"); }
                            var user = users.findById(id).orElseThrow(() -> new OAuth2AuthenticationException("invalid_token"));
                            String role = "ADMIN".equals(user.getRole()) ? "ADMIN" : "USER";
                            return new JwtAuthenticationToken(token,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role)), token.getSubject());
                        })))
                .build();
    }
}
