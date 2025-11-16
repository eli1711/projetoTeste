package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.*;
import br.com.cpa.questionario.repository.*;
import br.com.cpa.questionario.service.AvaliacaoEmailService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/avaliacoes")
public class AvaliacaoAplicadaController {

    private final AvaliacaoAplicadaRepository avaliacaoAplicadaRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final TurmaRepository turmaRepository;
    private final RespostaAlunoRepository respostaAlunoRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final AvaliacaoEmailService avaliacaoEmailService;

    public AvaliacaoAplicadaController(AvaliacaoAplicadaRepository avaliacaoAplicadaRepository,
                                       QuestionnaireRepository questionnaireRepository,
                                       QuestionRepository questionRepository,
                                       TurmaRepository turmaRepository,
                                       RespostaAlunoRepository respostaAlunoRepository,
                                       AnswerRepository answerRepository,
                                       UserRepository userRepository,
                                       AvaliacaoEmailService avaliacaoEmailService) {
        this.avaliacaoAplicadaRepository = avaliacaoAplicadaRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.questionRepository = questionRepository;
        this.turmaRepository = turmaRepository;
        this.respostaAlunoRepository = respostaAlunoRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.avaliacaoEmailService = avaliacaoEmailService;
    }

    // ====== LISTAGEM (Admin / Gestor) ======
    @GetMapping
    public String list(Model model) {
        model.addAttribute("avaliacoes", avaliacaoAplicadaRepository.findAll());
        return "avaliacao/list";
    }

    // ====== LISTAR AVALIAÇÕES DISPONÍVEIS PARA O ALUNO LOGADO ======
// ====== LISTAR AVALIAÇÕES DISPONÍVEIS PARA O ALUNO LOGADO ======
@GetMapping("/disponiveis")
public String avaliacoesDisponiveisParaAluno(Model model) {
    User aluno = getCurrentUser();
    if (aluno == null) {
        return "redirect:/login";
    }

    System.out.println(">>> Aluno logado: " + aluno.getUsername()
            + " | role=" + aluno.getRole()
            + " | turma=" + (aluno.getTurma() != null ? aluno.getTurma().getNome() : "SEM TURMA"));

    List<AvaliacaoAplicada> disponiveis = List.of();

    if (aluno.getTurma() != null) {
        // Busca só as avaliações da turma do aluno e com status ABERTA
        disponiveis = avaliacaoAplicadaRepository
                .findByTurmaIdAndStatus(aluno.getTurma().getId(), StatusAvaliacao.ABERTA);

        LocalDateTime agora = LocalDateTime.now();

        disponiveis = disponiveis.stream()
                // período (se quiser, pode comentar esse filtro pra testar)
                .filter(a ->
                        (a.getDataInicio() == null || !agora.isBefore(a.getDataInicio())) &&
                        (a.getDataFim() == null || !agora.isAfter(a.getDataFim()))
                )
                // ainda não respondeu essa avaliação
                .filter(a -> !respostaAlunoRepository.existsByAlunoAndAvaliacaoAplicada(aluno, a))
                .toList();
    }

    System.out.println(">>> Avaliações encontradas para o aluno: " + disponiveis.size());
    disponiveis.forEach(a -> System.out.println("   - Aval " + a.getId()
            + " | turma=" + a.getTurma().getNome()
            + " | status=" + a.getStatus()
            + " | inicio=" + a.getDataInicio()
            + " | fim=" + a.getDataFim()));

    model.addAttribute("avaliacoes", disponiveis);
    model.addAttribute("aluno", aluno);
    return "avaliacao/disponiveis";
}

    // ====== FORM NOVA AVALIAÇÃO APLICADA ======
    @GetMapping("/new")
    public String newAvaliacao(Model model) {
        model.addAttribute("avaliacao", new AvaliacaoAplicada());
        model.addAttribute("turmas", turmaRepository.findAll());
        model.addAttribute("questionnaires", questionnaireRepository.findAll());
        return "avaliacao/edit";   // template que você já criou para "Nova Avaliação"
    }

    // ====== CRIA AVALIAÇÃO APLICADA + ENVIA E-MAILS ======
    @PostMapping
    @Transactional
    public String createAvaliacao(@RequestParam Long turmaId,
                                  @RequestParam Long questionnaireId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                  LocalDateTime dataInicio,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                  LocalDateTime dataFim,
                                  RedirectAttributes redirectAttributes) {

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new IllegalArgumentException("Turma não encontrada"));
        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado"));

        AvaliacaoAplicada avaliacao = new AvaliacaoAplicada();
        avaliacao.setTurma(turma);
        avaliacao.setQuestionario(questionnaire);
        avaliacao.setDataInicio(dataInicio);
        avaliacao.setDataFim(dataFim);
        avaliacao.setStatus(StatusAvaliacao.ABERTA);

        avaliacao = avaliacaoAplicadaRepository.save(avaliacao);

        // Envia e-mails para todos os alunos ATIVOS da turma
        avaliacaoEmailService.enviarConvites(avaliacao);

        redirectAttributes.addFlashAttribute("success", "Avaliação criada e e-mails enviados.");
        return "redirect:/avaliacoes";
    }

    // ====== ALUNO RESPONDE AVALIAÇÃO ======
    @GetMapping("/{id}/responder")
    public String responder(@PathVariable Long id, Model model) {
        AvaliacaoAplicada avaliacao = avaliacaoAplicadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada"));

        User aluno = getCurrentUser();
        if (aluno == null) {
            return "redirect:/login";
        }

        // aluno só pode responder UMA vez por avaliação
        if (respostaAlunoRepository.existsByAlunoAndAvaliacaoAplicada(aluno, avaliacao)) {
            model.addAttribute("error", "Você já respondeu esta avaliação.");
            return "avaliacao/ja_respondida";
        }

        Questionnaire q = avaliacao.getQuestionario();
        model.addAttribute("avaliacao", avaliacao);
        model.addAttribute("questionnaire", q);
        model.addAttribute("questions", questionRepository.findByQuestionnaireId(q.getId()));
        return "avaliacao/responder";
    }

    @PostMapping("/{id}/responder")
    @Transactional
    public String salvarRespostas(@PathVariable Long id,
                                  @RequestParam Map<String, String> params,
                                  RedirectAttributes redirectAttributes) {

        AvaliacaoAplicada avaliacao = avaliacaoAplicadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada"));

        User aluno = getCurrentUser();
        if (aluno == null) {
            return "redirect:/login";
        }

        if (respostaAlunoRepository.existsByAlunoAndAvaliacaoAplicada(aluno, avaliacao)) {
            redirectAttributes.addFlashAttribute("error", "Você já respondeu esta avaliação.");
            return "redirect:/avaliacoes/" + id + "/responder";
        }

        // cria o "envelope" da resposta do aluno para ESSA avaliação
        RespostaAluno respostaAluno = new RespostaAluno();
        respostaAluno.setAluno(aluno);
        respostaAluno.setAvaliacaoAplicada(avaliacao);
        respostaAluno.setDataResposta(LocalDateTime.now());
        respostaAluno.setStatusResposta(StatusResposta.RESPONDIDO);

        RespostaAluno respostaAlunoSalvo = respostaAlunoRepository.save(respostaAluno);

        Map<Long, String> responses = extractResponses(params);
        responses.forEach((qid, value) -> {
            Question question = questionRepository.findById(qid)
                    .orElseThrow(() -> new IllegalArgumentException("Questão não encontrada: " + qid));

            Answer a = new Answer();
            a.setQuestion(question);
            a.setResponse(value);
            a.setRespostaAluno(respostaAlunoSalvo);      // liga à avaliação
            a.setUserUsername(aluno.getUsername());      // para facilitar visualização

            answerRepository.save(a);
        });

        redirectAttributes.addFlashAttribute("success", "Respostas enviadas com sucesso!");
        return "redirect:/home";
    }

    // ====== ADMIN VER RESPOSTAS DE UMA AVALIAÇÃO ESPECÍFICA ======
    @GetMapping("/{id}/respostas")
    public String verRespostasAvaliacao(@PathVariable Long id, Model model) {
        AvaliacaoAplicada avaliacao = avaliacaoAplicadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada"));

        Questionnaire questionnaire = avaliacao.getQuestionario();
        var questions = questionRepository.findByQuestionnaireId(questionnaire.getId());

        // todas as respostas (de todos os alunos) desta avaliação
        var answers = answerRepository.findByRespostaAlunoAvaliacaoAplicadaId(id);

        // monta Map<QuestionId, List<Answer>> para o template avaliacao/respostas.html
        Map<Long, List<Answer>> answersByQuestion = new HashMap<>();
        for (Question q : questions) {
            answersByQuestion.put(q.getId(), new ArrayList<>());
        }
        for (Answer a : answers) {
            Long qid = a.getQuestion().getId();
            answersByQuestion.computeIfAbsent(qid, k -> new ArrayList<>()).add(a);
        }

        model.addAttribute("avaliacao", avaliacao);
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("questions", questions);
        model.addAttribute("answersByQuestion", answersByQuestion);

        return "avaliacao/respostas";
    }

    // ====== AUXILIARES ======
    private Map<Long, String> extractResponses(Map<String, String> params) {
        Map<Long, String> out = new HashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("responses[")) {
                String idStr = k.substring("responses[".length(), k.length() - 1);
                try {
                    out.put(Long.parseLong(idStr), v);
                } catch (Exception ignored) {}
            }
        });
        return out;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName());
    }
}
