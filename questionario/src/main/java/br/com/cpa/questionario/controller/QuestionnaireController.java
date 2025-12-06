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
    //  RESPONDER QUESTIONÁRIO “PURO” (DESATIVADO COMO COLETA OFICIAL)
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

        redirectAttributes.addFlashAttribute("error",
                "Este questionário é um modelo. As respostas oficiais devem ser enviadas pela Avaliação Aplicada da sua turma.");
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

        // popula o combo de "Item de Avaliação"
        model.addAttribute("itensAvaliacao", ItemAvaliacao.values());

        return "questionnaire/add_question";
    }

    @PostMapping("/{id}/questions/new")
    public String createQuestion(@PathVariable Long id,
                                 @RequestParam String text,
                                 @RequestParam String type,
                                 @RequestParam ItemAvaliacao item,
                                 @RequestParam(required = false) Integer score,
                                 @RequestParam(required = false) String option1Label,
                                 @RequestParam(required = false) String option2Label,
                                 @RequestParam(required = false) String option3Label,
                                 @RequestParam(required = false) String option4Label,
                                 @RequestParam(required = false) String option5Label,
                                 Model model) {

        Questionnaire questionnaire = getQuestionnaireOrThrow(id);

        Question question = new Question();
        question.setText(text);

        // Item de avaliação (vem do select)
        question.setItemAvaliacao(item);

        // Como o grau de importância agora é respondido pelo aluno,
        // definimos um valor padrão interno apenas para satisfazer o banco.
        // Se não precisar mais da coluna, você pode removê-la depois.
        try {
            question.setGrauImportanciaModelo(GrauImportancia.MEDIA);
        } catch (Exception ignored) {
            // se o atributo não existir na entidade Question, pode remover esse bloco
        }

        try {
            QuestionType questionType =
                    QuestionType.valueOf(type.toUpperCase(Locale.ROOT).trim());
            question.setType(questionType);

            if (questionType == QuestionType.QUANTITATIVA) {

                if (score != null) {
                    question.setScore(score);
                } else {
                    question.setScore(4);
                }

                if (option1Label == null || option1Label.isBlank()) {
                    option1Label = "Discordo totalmente";
                }
                if (option2Label == null || option2Label.isBlank()) {
                    option2Label = "Discordo parcialmente";
                }
                if (option3Label == null || option3Label.isBlank()) {
                    option3Label = "Concordo parcialmente";
                }
                if (option4Label == null || option4Label.isBlank()) {
                    option4Label = "Concordo totalmente";
                }

                question.setOption1Label(option1Label);
                question.setOption2Label(option2Label);
                question.setOption3Label(option3Label);
                question.setOption4Label(option4Label);

                if (option5Label != null && !option5Label.isBlank()) {
                    question.setOption5Label(option5Label);
                } else {
                    question.setOption5Label(null);
                }

            } else {
                // qualitativa
                question.setScore(null);
                question.setOption1Label(null);
                question.setOption2Label(null);
                question.setOption3Label(null);
                question.setOption4Label(null);
                question.setOption5Label(null);
            }

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Tipo de questão inválido: " + type);
            model.addAttribute("questionnaire", questionnaire);
            model.addAttribute("question", question);
            model.addAttribute("itensAvaliacao", ItemAvaliacao.values());
            return "questionnaire/add_question";
        }

        question.setQuestionnaire(questionnaire);
        questionRepository.save(question);

        return "redirect:/questionnaires/" + id;
    }
}
