package com.pauta.colorprint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. DESACTIVAR CSRF (Esto arregla el error 403 en los botones)
                .csrf(csrf -> csrf.disable())

                // 2. Configurar permisos
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 3. Configurar Login
                .formLogin((form) -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/ingreso", true)
                        .permitAll()
                )
                .logout((logout) -> logout.permitAll());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Usuarios (Contraseña 1234 para todos)
        UserDetails jorge = User.withDefaultPasswordEncoder().username("jorge").password("gerencia1234").roles("ADMIN").build();
        UserDetails pamela = User.withDefaultPasswordEncoder().username("pamela").password("ventas1234").roles("VENTAS").build();
        UserDetails anita = User.withDefaultPasswordEncoder().username("anita").password("ventas1234").roles("VENTAS").build();
        UserDetails alex = User.withDefaultPasswordEncoder().username("alex").password("prensa1234").roles("PREPRENSA").build();
        UserDetails matias = User.withDefaultPasswordEncoder().username("matias").password("prensa1234").roles("PREPRENSA").build();
        UserDetails gloria = User.withDefaultPasswordEncoder().username("gloria").password("produccion1234").roles("PRODUCCION").build();
        UserDetails german = User.withDefaultPasswordEncoder().username("german").password("produccion1234").roles("PRODUCCION").build();
        UserDetails diego = User.withDefaultPasswordEncoder().username("diego").password("produccion1234").roles("PRODUCCION").build();

        return new InMemoryUserDetailsManager(jorge, pamela, anita, alex, matias, gloria, german, diego);
    }
}