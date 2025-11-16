package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.StatusAluno;
import br.com.cpa.questionario.model.Turma;
import br.com.cpa.questionario.model.User;
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

    public UserController(UserRepository userRepository,
                          TurmaRepository turmaRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.turmaRepository = turmaRepository;
        this.passwordEncoder = passwordEncoder;
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
            redirectAttributes.addFlashAttribute("error", "Login é obrigatório.");
            return "redirect:/users/aluno/registro";
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Senha é obrigatória.");
            return "redirect:/users/aluno/registro";
        }

        if (userRepository.existsById(user.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Já existe um usuário com este login.");
            return "redirect:/users/aluno/registro";
        }

        // turma
        if (turmaId != null) {
            Turma turma = turmaRepository.findById(turmaId).orElse(null);
            user.setTurma(turma);
        } else {
            user.setTurma(null);
        }

        // força status e role
        user.setStatus(StatusAluno.ATIVO);
        user.setRole("ROLE_ALUNO");

        // criptografa senha
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

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
            writer.println("nome;email;username;senha;ra;turmaNome;role");
            writer.println("João da Silva;joao.silva@faculdade.edu;br.joao;Senha123;123456;ADS 2º Semestre 2025;ROLE_ALUNO");
        }
    }

    // ========= IMPORTAR CSV DE ALUNOS =========
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

                if (linhaAtual == 1 && line.toLowerCase().contains("nome;email;username")) {
                    continue;
                }

                if (line.isBlank()) {
                    continue;
                }

                String[] cols = line.split(";", -1);
                if (cols.length < 6) {
                    ignorados++;
                    continue;
                }

                String nome = cols[0].trim();
                String email = cols[1].trim();
                String username = cols[2].trim();
                String senhaPlano = cols[3].trim();
                String ra = cols[4].trim();
                String turmaNome = cols[5].trim();
                String roleCsv = (cols.length >= 7 ? cols[6].trim() : "ROLE_ALUNO");

                if (username.isEmpty() || email.isEmpty()) {
                    ignorados++;
                    continue;
                }

                if (userRepository.existsById(username)) {
                    ignorados++;
                    continue;
                }

                Turma turma = null;
                if (!turmaNome.isEmpty()) {
                    turma = turmaRepository.findByNome(turmaNome).orElse(null);
                }

                User user = new User();
                user.setUsername(username);
                user.setName(nome);
                user.setEmail(email);
                user.setRa(ra);
                user.setTurma(turma);
                user.setStatus(StatusAluno.ATIVO);
                user.setRole("ROLE_ALUNO");

                if (senhaPlano == null || senhaPlano.isBlank()) {
                    senhaPlano = "Aluno123";
                }
                user.setPassword(passwordEncoder.encode(senhaPlano));

                userRepository.save(user);
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
