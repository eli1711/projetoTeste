package br.com.cpa.questionario.config;

import br.com.cpa.questionario.model.User;
import br.com.cpa.questionario.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas (sem login)
                .requestMatchers(
                        "/login",
                        "/perform_login",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/h2-console/**",
                        "/users/aluno/registro",
                        "/users/aluno/registrar"
                ).permitAll()

                // Rotas específicas para ALUNO
                // (precisam estar ANTES do "/avaliacoes/**")
                .requestMatchers("/avaliacoes/disponiveis/**").hasAuthority("ROLE_ALUNO")
                .requestMatchers("/avaliacoes/*/responder/**").hasAuthority("ROLE_ALUNO") // <- ADICIONADO
                // se tiver questionário "puro" para aluno:
                .requestMatchers("/questionnaires/available/**").hasAuthority("ROLE_ALUNO")

                // Rotas de gestão (ADMIN / GESTOR)
                .requestMatchers(
                        "/avaliacoes/**",
                        "/questionnaires/**",
                        "/users/**",
                        "/turmas/**",
                        "/alunos/**",
                        "/analise/**"
                ).hasAnyAuthority("ROLE_ADMIN", "ROLE_GESTOR")

                // /home pode ser visto por qualquer usuário autenticado
                .requestMatchers("/home").authenticated()

                // Qualquer outra rota autenticada (se existir algo extra)
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            // para conseguir abrir o H2-console em frame
            .headers(h -> h.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    // ===================== USER DETAILS =====================
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User u = userRepository.findByUsername(username);
            if (u == null) {
                throw new UsernameNotFoundException("Usuário não encontrado");
            }

            String role = (u.getRole() == null || u.getRole().isBlank())
                    ? "ROLE_USER"
                    : u.getRole();

            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getUsername())
                    .password(u.getPassword())
                    .authorities(role)
                    .build();
        };
    }

    // ===================== PASSWORD ENCODER =====================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ===================== AUTH MANAGER =====================
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) throws Exception {
        AuthenticationManagerBuilder amb = http.getSharedObject(AuthenticationManagerBuilder.class);
        amb.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        return amb.build();
    }
}
