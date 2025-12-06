package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.Aluno;
import br.com.cpa.questionario.repository.AlunoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final AlunoRepository alunoRepository;

    public HomeController(AlunoRepository alunoRepository) {
        this.alunoRepository = alunoRepository;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model, Principal principal) {

        if (principal != null) {
            String username = principal.getName(); // RA
            Aluno aluno = alunoRepository.findByUserUsername(username).orElse(null);

            if (aluno != null) {
                model.addAttribute("aluno", aluno);

                // se o aluno ainda não tiver turma, força definir
                if (aluno.getTurma() == null) {
                    return "redirect:/alunos/definir-turma";
                }
            }
        }

        return "home"; // sua view home.html
    }
}
