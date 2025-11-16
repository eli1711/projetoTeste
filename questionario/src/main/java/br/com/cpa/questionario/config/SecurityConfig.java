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
            .authorizeHttpRequests(auth -> auth
    // público / login / cadastro
    .requestMatchers(
        "/login", "/perform_login", "/error",
        "/css/**", "/js/**", "/images/**",
        "/users/aluno/registro", "/users/aluno/registrar"
    ).permitAll()

    // ROTAS DO ALUNO
    .requestMatchers("/avaliacoes/disponiveis", "/avaliacoes/*/responder/**")
        .hasAuthority("ROLE_ALUNO")

    .requestMatchers("/questionnaires/available/**")
        .hasAuthority("ROLE_ALUNO")

    // ANÁLISE
    .requestMatchers("/analise/**")
        .hasAnyAuthority("ROLE_ADMIN", "ROLE_PROFESSOR")

    // USUÁRIOS
    .requestMatchers("/users/**")
        .hasAnyAuthority("ROLE_ADMIN", "ADMIN")

    // TURMAS
    .requestMatchers(HttpMethod.GET, "/turmas")
        .authenticated()
    .requestMatchers(HttpMethod.GET, "/turmas/new", "/turmas/*/edit")
        .hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDENADOR")
    .requestMatchers(HttpMethod.POST, "/turmas/**")
        .hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDENADOR")

    // QUESTIONÁRIOS & AVALIAÇÕES (GESTÃO)
    .requestMatchers("/questionnaires/**", "/avaliacoes/**")
        .hasAnyAuthority("ROLE_ADMIN", "ROLE_PROFESSOR")

    // HOME
    .requestMatchers("/", "/home").authenticated()

    // qualquer outra rota
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
            .exceptionHandling(e -> e.accessDeniedPage("/error"))
            .csrf(csrf -> csrf.disable());

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

            // valores no banco devem ser: ROLE_ADMIN, ROLE_ALUNO, ROLE_PROFESSOR, ROLE_COORDENADOR
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getUsername())
                    .password(u.getPassword())
                    .authorities(u.getRole())
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
