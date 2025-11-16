package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.*;
import br.com.cpa.questionario.repository.QuestionRepository;
import br.com.cpa.questionario.repository.QuestionnaireRepository;
import br.com.cpa.questionario.repository.AnswerRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/questionnaires")
public class QuestionnaireController {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public QuestionnaireController(QuestionnaireRepository questionnaireRepository,
                                   QuestionRepository questionRepository,
                                   AnswerRepository answerRepository) {
        this.questionnaireRepository = questionnaireRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    private Questionnaire getQuestionnaireOrThrow(Long id) {
        return questionnaireRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado"));
    }

    // =====================================================================
    //  LISTAGEM (MODELOS)
    // =====================================================================

    @GetMapping
    public String listQuestionnaires(Model model) {
        model.addAttribute("questionnaires", questionnaireRepository.findAll());
        return "questionnaire/list";
    }

    @GetMapping("/available")
    public String availableQuestionnaires(Model model) {
        model.addAttribute("questionnaires",
                questionnaireRepository.findByStatus(StatusDisponibilidade.DISPONIVEL));
        return "questionnaire/available";
    }

    // =====================================================================
    //  RESPONDER QUESTIONÁRIO “PURO” (sem AvaliaçãoAplicada)
    // =====================================================================

    @GetMapping("/{id}/respond")
    public String respondQuestionnaire(@PathVariable Long id, Model model) {
        Questionnaire questionnaire = getQuestionnaireOrThrow(id);
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("questions", questionRepository.findByQuestionnaireId(id));
        return "questionnaire/respond";
    }

    @PostMapping("/{id}/respond")
    public String submitQuestionnaire(@PathVariable Long id,
                                      @RequestParam Map<String, String> formParams,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {

        Questionnaire questionnaire = getQuestionnaireOrThrow(id);

        String username = (principal != null ? principal.getName() : "anonimo");
        System.out.println("Usuário " + username +
                " enviou respostas para o questionário " + questionnaire.getId() +
                ": " + formParams);

        // 1) apaga respostas anteriores desse usuário para este questionário "puro"
        answerRepository.deleteByQuestionQuestionnaireIdAndUserUsernameAndRespostaAlunoIsNull(id, username);

        // 2) monta a lista de Answer novas
        List<Answer> answersToSave = new ArrayList<>();

        formParams.forEach((key, value) -> {
            if (!key.startsWith("responses[")) return;
            if (value == null || value.isBlank()) return;

            try {
                String idStr = key.substring("responses[".length(), key.length() - 1);
                Long questionId = Long.parseLong(idStr);

                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Questão não encontrada: " + questionId));

                Answer answer = new Answer();
                answer.setQuestion(question);
                answer.setResponse(value);
                answer.setUserUsername(username);   // “dono” da resposta (questionário puro)
                answer.setRespostaAluno(null);      // *** importante: sem avaliação aplicada

                answersToSave.add(answer);

            } catch (NumberFormatException ex) {
                System.out.println("Parâmetro de resposta ignorado (id inválido): " + key);
            }
        });

        if (!answersToSave.isEmpty()) {
            answerRepository.saveAll(answersToSave);
        }

        redirectAttributes.addFlashAttribute("success",
                "Respostas enviadas com sucesso!");
        return "redirect:/home";
    }

    // =====================================================================
    //  VISUALIZAR RESPOSTAS DE UM QUESTIONÁRIO (MODO ADMIN / MODELO)
    // =====================================================================

    @GetMapping("/{id}/answers")
    public String viewQuestionnaireAnswers(@PathVariable Long id, Model model) {
        Questionnaire questionnaire = getQuestionnaireOrThrow(id);

        var questions = questionRepository.findByQuestionnaireId(id);
        var answers = answerRepository.findByQuestionQuestionnaireIdAndRespostaAlunoIsNull(id);

        // monta Map<QuestionId, List<Answer>> para o template questionnaire/answers.html
        Map<Long, List<Answer>> answersByQuestion = new HashMap<>();
        for (Question q : questions) {
            answersByQuestion.put(q.getId(), new ArrayList<>());
        }
        for (Answer a : answers) {
            Long qid = a.getQuestion().getId();
            answersByQuestion.computeIfAbsent(qid, k -> new ArrayList<>()).add(a);
        }

        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("questions", questions);
        model.addAttribute("answersByQuestion", answersByQuestion);

        return "questionnaire/answers";
    }

    @GetMapping("/{id}/answers/me")
    public String viewMyAnswers(@PathVariable Long id,
                                Principal principal,
                                Model model) {

        Questionnaire questionnaire = getQuestionnaireOrThrow(id);
        String username = (principal != null ? principal.getName() : null);

        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("questions", questionRepository.findByQuestionnaireId(id));

        if (username != null) {
            model.addAttribute("answers",
                    answerRepository.findByQuestionQuestionnaireIdAndUserUsernameAndRespostaAlunoIsNull(id, username));
        } else {
            model.addAttribute("answers", List.of());
        }

        model.addAttribute("username", username);
        return "questionnaire/my_answers";
    }

    // =====================================================================
    //  CRUD QUESTIONÁRIO (MODELO)
    // =====================================================================

    @GetMapping("/new")
    public String newQuestionnaire(Model model) {
        model.addAttribute("questionnaire", new Questionnaire());
        return "questionnaire/edit";
    }

    @PostMapping("/save")
    public String saveQuestionnaire(@ModelAttribute Questionnaire questionnaire) {
        questionnaireRepository.save(questionnaire);
        return "redirect:/questionnaires";
    }

    @GetMapping("/{id}")
    public String viewQuestionnaire(@PathVariable Long id, Model model) {
        Questionnaire questionnaire = getQuestionnaireOrThrow(id);
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("questions", questionRepository.findByQuestionnaireId(id));
        return "questionnaire/view";
    }

    @GetMapping("/{id}/edit")
    public String editQuestionnaire(@PathVariable Long id, Model model) {
        model.addAttribute("questionnaire", getQuestionnaireOrThrow(id));
        return "questionnaire/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateQuestionnaire(@PathVariable Long id,
                                      @ModelAttribute Questionnaire questionnaire) {
        questionnaire.setId(id);
        questionnaireRepository.save(questionnaire);
        return "redirect:/questionnaires";
    }

    @PostMapping("/{id}/delete")
    public String deleteQuestionnaire(@PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            questionnaireRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success",
                    "Questionário apagado com sucesso.");
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error",
                    "Não é possível excluir este questionário porque ele já possui " +
                    "avaliações aplicadas ou respostas associadas. " +
                    "Altere o status para 'NAO_DISPONIVEL' para arquivá-lo.");
        }
        return "redirect:/questionnaires";
    }

    // =====================================================================
    //  CRUD DE QUESTÕES
    // =====================================================================

    @GetMapping("/{id}/questions/new")
    public String newQuestion(@PathVariable Long id, Model model) {
        Questionnaire questionnaire = getQuestionnaireOrThrow(id);
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("question", new Question());
        return "questionnaire/add_question";
    }

    @PostMapping("/{id}/questions/new")
    public String createQuestion(@PathVariable Long id,
                                 @RequestParam String text,
                                 @RequestParam String type,
                                 @RequestParam(required = false) Integer score,
                                 Model model) {

        Questionnaire questionnaire = getQuestionnaireOrThrow(id);

        Question question = new Question();
        question.setText(text);

        try {
            QuestionType questionType =
                    QuestionType.valueOf(type.toUpperCase(Locale.ROOT).trim());
            question.setType(questionType);

            if (questionType == QuestionType.QUANTITATIVA && score != null) {
                question.setScore(score);
            } else {
                question.setScore(null);
            }

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Tipo de questão inválido: " + type);
            model.addAttribute("questionnaire", questionnaire);
            model.addAttribute("question", question);
            return "questionnaire/add_question";
        }

        question.setQuestionnaire(questionnaire);
        questionRepository.save(question);

        return "redirect:/questionnaires/" + id;
    }
}
