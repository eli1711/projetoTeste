package br.com.cpa.questionario;

import br.com.cpa.questionario.model.StatusAluno;
import br.com.cpa.questionario.model.Turma;
import br.com.cpa.questionario.model.User;
import br.com.cpa.questionario.repository.TurmaRepository;
import br.com.cpa.questionario.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class QuestionarioApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuestionarioApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(UserRepository userRepository,
                                  TurmaRepository turmaRepository,
                                  PasswordEncoder encoder) {
        return args -> {

            // ---------- Turma padrão ----------
            Turma turmaPadrao = turmaRepository.findByNome("ADS 2º Semestre 2025")
                    .orElseGet(() -> {
                        Turma t = new Turma();
                        t.setNome("ADS 2º Semestre 2025");
                        t.setCurso("Análise e Desenvolvimento de Sistemas");
                        t.setSemestre(2);
                        t.setAno(2025);
                        return turmaRepository.save(t);
                    });

            // ---------- ALUNO ----------
            if (userRepository.findByUsername("aluno") == null) {
                User u = new User();
                u.setUsername("aluno");
                u.setPassword(encoder.encode("123456"));
                u.setName("Aluno Padrão");
                u.setEmail("aluno@instituicao.com");
                u.setRa("RA0001");
                u.setRole("ROLE_ALUNO");
                u.setTurma(turmaPadrao);
                u.setStatus(StatusAluno.ATIVO);
                userRepository.save(u);
            }

            // ---------- PROFESSOR ----------
            if (userRepository.findByUsername("prof") == null) {
                User u = new User();
                u.setUsername("prof");
                u.setPassword(encoder.encode("123456"));
                u.setName("Professor Padrão");
                u.setEmail("professor@instituicao.com");
                u.setRa("PROF001");
                u.setRole("ROLE_PROFESSOR");
                u.setTurma(turmaPadrao); // se quiser associar
                u.setStatus(StatusAluno.ATIVO);
                userRepository.save(u);
            }

            // ---------- ADMIN ----------
            if (userRepository.findByUsername("admin") == null) {
                User u = new User();
                u.setUsername("admin");
                u.setPassword(encoder.encode("admin")); // senha: admin
                u.setName("Administrador do Sistema");
                u.setEmail("admin@instituicao.com");
                u.setRa("ADMIN001");
                u.setRole("ROLE_ADMIN");
                u.setTurma(turmaPadrao); // opcional, mas ajuda nos relacionamentos
                u.setStatus(StatusAluno.ATIVO);
                userRepository.save(u);
            }
        };
    }
}
