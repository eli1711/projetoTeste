package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.Turma;
import br.com.cpa.questionario.repository.TurmaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/turmas")
public class TurmaController {

    private final TurmaRepository turmaRepository;

    public TurmaController(TurmaRepository turmaRepository) {
        this.turmaRepository = turmaRepository;
    }

    // LISTA TODAS AS TURMAS
    @GetMapping
    public String list(Model model) {
        model.addAttribute("turmas", turmaRepository.findAll());
        return "turma/list"; // templates/turma/list.html
    }

    // FORM NOVA TURMA
    @GetMapping("/new")
    public String newTurma(Model model) {
        model.addAttribute("turma", new Turma());
        return "turma/form"; // templates/turma/form.html
    }

    // FORM EDITAR TURMA
    @GetMapping("/{id}/edit")
    public String editTurma(@PathVariable Long id, Model model) {
        Turma turma = turmaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turma não encontrada"));
        model.addAttribute("turma", turma);
        return "turma/form";
    }

    // SALVAR (CRIAR/EDITAR) TURMA
    @PostMapping
    public String saveTurma(@ModelAttribute Turma turma,
                            RedirectAttributes redirectAttributes) {
        turmaRepository.save(turma);
        redirectAttributes.addFlashAttribute("success", "Turma salva com sucesso!");
        return "redirect:/turmas";
    }

    // APAGAR TURMA (se for permitido)
    @PostMapping("/{id}/delete")
    public String deleteTurma(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        turmaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Turma removida com sucesso!");
        return "redirect:/turmas";
    }
}
