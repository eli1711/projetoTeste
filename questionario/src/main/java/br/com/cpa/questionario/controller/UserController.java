package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.*;
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
import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final TurmaRepository turmaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlunoRepository alunoRepository;

    public UserController(UserRepository userRepository,
                          TurmaRepository turmaRepository,
                          PasswordEncoder passwordEncoder,
                          AlunoRepository alunoRepository) {
        this.userRepository = userRepository;
        this.turmaRepository = turmaRepository;
        this.passwordEncoder = passwordEncoder;
        this.alunoRepository = alunoRepository;
    }

    // ========== CADASTRO PÚBLICO DE ALUNO ==========

    @GetMapping("/aluno/registro")
    public String formCadastroAluno(Model model) {
        User user = new User();
        user.setStatus(StatusAluno.ATIVO);
        user.setRole("ROLE_ALUNO"); // default no objeto

        model.addAttribute("user", user);
        model.addAttribute("turmas", turmaRepository.findAll());
        model.addAttribute("cadastroAluno", true);

        return "user/edit"; // reutilizando o mesmo formulário
    }

    @PostMapping("/aluno/registrar")
    public String processarCadastroAluno(@ModelAttribute("user") User user,
                                         @RequestParam(value = "turmaId", required = false) Long turmaId,
                                         RedirectAttributes redirectAttributes) {

        // validações simples
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Login (RA) é obrigatório.");
            return "redirect:/users/aluno/registro";
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Senha (CPF) é obrigatória.");
            return "redirect:/users/aluno/registro";
        }

        if (userRepository.existsById(user.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Já existe um usuário com este login.");
            return "redirect:/users/aluno/registro";
        }

        // turma (pode ser null, mas sua regra de negócio é que ele escolha no primeiro login;
        // aqui podemos ignorar e deixar null, ou permitir escolher já)
        if (turmaId != null) {
            Turma turma = turmaRepository.findById(turmaId).orElse(null);
            user.setTurma(turma);
        } else {
            user.setTurma(null);
        }

        // força status e role
        user.setStatus(StatusAluno.ATIVO);
        user.setRole("ROLE_ALUNO");

        // senha = CPF criptografado
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // RA espelhado no campo ra
        user.setRa(user.getUsername());

        userRepository.save(user);

        // cria o registro na tabela aluno
        Aluno aluno = new Aluno();
        aluno.setNome(user.getName());
        aluno.setRa(user.getUsername());
        aluno.setCpf("********"); // se quiser guardar CPF real, passe antes da criptografia
        aluno.setEmail(user.getEmail());
        aluno.setUser(user);
        aluno.setTurma(user.getTurma());
        alunoRepository.save(aluno);

        redirectAttributes.addFlashAttribute("success",
                "Cadastro realizado com sucesso! Agora você já pode fazer login.");
        return "redirect:/login";
    }

    // ========== LISTA DE USUÁRIOS (ADMIN) ==========

    @GetMapping
    public String list(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "user/list";
    }

    // ========== FORMULÁRIO NOVO USUÁRIO (ADMIN) ==========

    @GetMapping("/new")
    public String newUser(Model model) {
        User user = new User();
        user.setStatus(StatusAluno.ATIVO);

        model.addAttribute("user", user);
        model.addAttribute("turmas", turmaRepository.findAll());
        model.addAttribute("cadastroAluno", false);
        return "user/edit";
    }

    // ========== EDITAR USUÁRIO EXISTENTE (ADMIN) ==========

    @GetMapping("/{username}/edit")
    public String editUser(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return "redirect:/users";
        }
        model.addAttribute("user", user);
        model.addAttribute("turmas", turmaRepository.findAll());
        model.addAttribute("cadastroAluno", false);
        return "user/edit";
    }

    // ========== SALVAR (CRIAR/ATUALIZAR) (ADMIN) ==========

    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") User user,
                           @RequestParam(value = "turmaId", required = false) Long turmaId,
                           RedirectAttributes redirectAttributes) {

        if (turmaId != null) {
            Turma turma = turmaRepository.findById(turmaId).orElse(null);
            user.setTurma(turma);
        } else {
            user.setTurma(null);
        }

        User existing = userRepository.findByUsername(user.getUsername());
        if (existing == null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(existing.getPassword());
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }

        if (user.getStatus() == null) {
            user.setStatus(StatusAluno.ATIVO);
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Usuário salvo com sucesso!");
        return "redirect:/users";
    }

    // ========== EXCLUIR (ADMIN) ==========

    @PostMapping("/{username}/delete")
    public String delete(@PathVariable String username,
                         RedirectAttributes redirectAttributes) {
        userRepository.deleteById(username);
        redirectAttributes.addFlashAttribute("success", "Usuário excluído com sucesso!");
        return "redirect:/users";
    }

    // ========= CSV MODELO DE ALUNOS =========

    @GetMapping("/alunos/template-csv")
    public void downloadAlunoTemplate(HttpServletResponse response) throws IOException {
        String filename = "modelo_cadastro_alunos.csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("nome;ra;cpf");
            writer.println("João da Silva;123456;12345678900");
        }
    }

    // ========= IMPORTAR CSV DE ALUNOS =========
    // Formato: Nome;RA;CPF
    @PostMapping("/alunos/import")
    public String importAlunosCsv(@RequestParam("file") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Nenhum arquivo CSV enviado.");
            return "redirect:/users";
        }

        int criados = 0;
        int ignorados = 0;
        int linhaAtual = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                linhaAtual++;

                // Cabeçalho: nome;ra;cpf
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

                if (userRepository.existsById(username)) {
                    ignorados++;
                    continue;
                }

                // ====== cria User (login) ======
                User user = new User();
                user.setUsername(username);
                user.setName(nome);
                user.setEmail(ra + "@aluno.sem-email.com"); // pode trocar depois
                user.setRole("ROLE_ALUNO");
                user.setStatus(StatusAluno.ATIVO);
                user.setTurma(null); // será definido no primeiro login
                user.setRa(ra);

                // senha = CPF
                user.setPassword(passwordEncoder.encode(cpf));

                userRepository.save(user);

                // ====== cria Aluno (tabela separada) ======
                Aluno aluno = new Aluno();
                aluno.setNome(nome);
                aluno.setRa(ra);
                aluno.setCpf(cpf);
                aluno.setEmail(user.getEmail());
                aluno.setUser(user);
                aluno.setTurma(null);

                alunoRepository.save(aluno);

                criados++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Erro ao processar CSV: " + e.getMessage());
            return "redirect:/users";
        }

        redirectAttributes.addFlashAttribute("success",
                "Importação concluída. Criados: " + criados + ", ignorados: " + ignorados + ".");
        return "redirect:/users";
    }
}
