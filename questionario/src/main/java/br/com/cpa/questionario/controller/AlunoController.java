package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.Aluno;
import br.com.cpa.questionario.model.StatusAluno;
import br.com.cpa.questionario.model.Turma;
import br.com.cpa.questionario.model.User;
import br.com.cpa.questionario.repository.AlunoRepository;
import br.com.cpa.questionario.repository.TurmaRepository;
import br.com.cpa.questionario.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunoRepository alunoRepository;
    private final TurmaRepository turmaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AlunoController(AlunoRepository alunoRepository,
                           TurmaRepository turmaRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.alunoRepository = alunoRepository;
        this.turmaRepository = turmaRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===============================================================
    // LISTAR ALUNOS (RAIZ /alunos)
    // ===============================================================
    @GetMapping
    public String listarAlunosRaiz(Model model) {
        return listarAlunos(model);
    }

    // ===============================================================
    // LISTAR ALUNOS (/alunos/list)
    // ===============================================================
    @GetMapping("/list")
    public String listarAlunos(Model model) {
        List<Aluno> alunos = alunoRepository.findAll();
        model.addAttribute("alunos", alunos);
        return "aluno/list"; // templates/aluno/list.html
    }

    // ===============================================================
    // DEFINIR TURMA (Aluno define sua própria turma)
    // ===============================================================
    @GetMapping("/definir-turma")
    public String escolherTurma(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        Aluno aluno = alunoRepository.findByUserUsername(username).orElse(null);
        if (aluno == null) {
            return "redirect:/home";
        }

        model.addAttribute("aluno", aluno);
        model.addAttribute("turmas", turmaRepository.findAll());
        return "aluno/definir_turma";
    }

    @PostMapping("/definir-turma")
    public String salvarTurma(@RequestParam Long turmaId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        Aluno aluno = alunoRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new IllegalArgumentException("Turma não encontrada"));

        aluno.setTurma(turma);
        alunoRepository.save(aluno);

        // mantém User sincronizado
        User user = userRepository.findByUsername(username);
        if (user != null) {
            user.setTurma(turma);
            userRepository.save(user);
        }

        redirectAttributes.addFlashAttribute("success", "Turma definida com sucesso!");
        return "redirect:/home";
    }

    // ===============================================================
    // EXEMPLO DE CSV PARA IMPORTAR ALUNOS
    // ===============================================================
    @GetMapping("/import/exemplo")
    public void downloadExemploCsv(HttpServletResponse response) throws IOException {
        String filename = "exemplo_import_alunos.csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("nome;ra;cpf");
            writer.println("João da Silva;123456;12345678900");
        }
    }

    // ===============================================================
    // FORM DE IMPORTAÇÃO
    // ===============================================================
    @GetMapping("/import")
    public String importarAlunosForm(Model model) {
        model.addAttribute("turmas", turmaRepository.findAll());
        return "aluno/import";
    }

    // ===============================================================
    // IMPORTAR ALUNOS (CSV) - login = RA, senha = CPF
    // ===============================================================
    @PostMapping("/import")
    public String importarAlunos(@RequestParam("file") MultipartFile file,
                                 @RequestParam("turmaId") Long turmaId,
                                 RedirectAttributes redirectAttributes) {

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Selecione um arquivo CSV para importar.");
            return "redirect:/alunos/import";
        }

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new IllegalArgumentException("Turma não encontrada."));

        int criados = 0;
        int ignorados = 0;
        int linhaAtual = 0;

        List<Aluno> alunosImportados = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                linhaAtual++;

                // pula cabeçalho "nome;ra;cpf"
                if (linhaAtual == 1 && line.toLowerCase().contains("nome;ra;cpf")) {
                    continue;
                }

                if (line.isBlank()) {
                    continue;
                }

                String[] cols = line.split(";", -1);
                if (cols.length < 3) {
                    ignorados++;
                    continue;
                }

                String nome = cols[0].trim();
                String ra   = cols[1].trim();
                String cpf  = cols[2].trim();

                if (ra.isEmpty() || cpf.isEmpty()) {
                    ignorados++;
                    continue;
                }

                String username = ra; // login = RA

                boolean usernameJaExiste = userRepository.existsById(username);
                boolean raJaExisteEmUser = userRepository.existsByRa(ra);
                boolean raJaExisteEmAluno = alunoRepository.findByRa(ra).isPresent();

                if (usernameJaExiste || raJaExisteEmUser || raJaExisteEmAluno) {
                    ignorados++;
                    continue;
                }

                // ====== cria User (login) ======
                User user = new User();
                user.setUsername(username);
                user.setName(nome);
                user.setEmail(ra + "@aluno.sem-email.com");
                user.setRole("ROLE_ALUNO");
                user.setStatus(StatusAluno.ATIVO);
                user.setTurma(turma);
                user.setRa(ra);

                // senha = CPF criptografado
                user.setPassword(passwordEncoder.encode(cpf));

                userRepository.save(user);

                // ====== cria Aluno (tabela separada) ======
                Aluno aluno = new Aluno();
                aluno.setNome(nome);
                aluno.setRa(ra);
                aluno.setCpf(cpf);
                aluno.setEmail(user.getEmail());
                aluno.setUser(user);
                aluno.setTurma(turma);

                alunoRepository.save(aluno);

                alunosImportados.add(aluno);
                criados++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Erro ao importar: " + e.getMessage());
            return "redirect:/alunos/import";
        }

        redirectAttributes.addFlashAttribute("success",
                "Importação concluída. Criados: " + criados + ", ignorados: " + ignorados + ".");
        redirectAttributes.addFlashAttribute("alunosImportados", alunosImportados);

        return "redirect:/alunos/import";
    }
}
