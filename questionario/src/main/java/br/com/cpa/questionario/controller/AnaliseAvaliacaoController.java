package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.*;
import br.com.cpa.questionario.repository.AvaliacaoAplicadaRepository;
import br.com.cpa.questionario.repository.AnswerRepository;
import br.com.cpa.questionario.repository.TurmaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/analise")
public class AnaliseAvaliacaoController {

    private final TurmaRepository turmaRepository;
    private final AvaliacaoAplicadaRepository avaliacaoAplicadaRepository;
    private final AnswerRepository answerRepository;

    public AnaliseAvaliacaoController(TurmaRepository turmaRepository,
                                      AvaliacaoAplicadaRepository avaliacaoAplicadaRepository,
                                      AnswerRepository answerRepository) {
        this.turmaRepository = turmaRepository;
        this.avaliacaoAplicadaRepository = avaliacaoAplicadaRepository;
        this.answerRepository = answerRepository;
    }

    @GetMapping("/avaliacoes")
    public String analiseAvaliacoes(@RequestParam(required = false) Long turmaId,
                                    @RequestParam(required = false) Integer ano,
                                    @RequestParam(required = false) Long avaliacaoId,
                                    Model model) {

        // -------- Filtros (turmas, anos, avaliações) ----------
        List<Turma> turmas = turmaRepository.findAll();
        List<AvaliacaoAplicada> todasAvaliacoes = avaliacaoAplicadaRepository.findAll();

        // anos distintos dos questionários
        Set<Integer> anosSet = todasAvaliacoes.stream()
                .map(a -> a.getQuestionario().getYear())
                .collect(Collectors.toCollection(TreeSet::new));
        List<Integer> anos = new ArrayList<>(anosSet);

        // aplicar filtros
        List<AvaliacaoAplicada> avaliacoesFiltradas = todasAvaliacoes.stream()
                .filter(a -> turmaId == null || a.getTurma().getId().equals(turmaId))
                .filter(a -> ano == null || a.getQuestionario().getYear() == ano)
                .filter(a -> avaliacaoId == null || a.getId().equals(avaliacaoId))
                .toList();

        long qtdAvaliacoesConsideradas = avaliacoesFiltradas.size();

        // -------- Carregar respostas dessas avaliações ----------
        List<Answer> respostas = new ArrayList<>();
        for (AvaliacaoAplicada av : avaliacoesFiltradas) {
            respostas.addAll(answerRepository.findByRespostaAlunoAvaliacaoAplicadaId(av.getId()));
        }

        long totalRespostas = respostas.size();

        // -------- Estatísticas por pergunta (apenas QUANTITATIVA) ----------
        Map<Question, Map<Integer, Long>> distribuicaoPorPergunta = new LinkedHashMap<>();
        Map<Question, Long> totalRespostasPergunta = new LinkedHashMap<>();
        Map<Question, Double> somaNotasPergunta = new LinkedHashMap<>();
        Map<Question, Long> totalNotasPergunta = new LinkedHashMap<>();

        // Qualitativas: guardar cada resposta
        Map<Question, List<String>> respostasQualitativasPorPergunta = new LinkedHashMap<>();

        // Por avaliação: soma e média das notas quantitativas
        Map<Long, Long> somaNotasAvaliacao = new LinkedHashMap<>();
        Map<Long, Long> totalNotasAvaliacao = new LinkedHashMap<>();

        double somaNotasGlobais = 0.0;
        long totalNotasGlobais = 0L;

        for (Answer ans : respostas) {
            Question q = ans.getQuestion();
            totalRespostasPergunta.merge(q, 1L, Long::sum);

            boolean isQuant = (q.getType() == QuestionType.QUANTITATIVA);

            // -------- QUALITATIVAS: guarda texto da resposta --------
            if (!isQuant) {
                respostasQualitativasPorPergunta
                        .computeIfAbsent(q, k -> new ArrayList<>())
                        .add(ans.getResponse());
                continue;
            }

            // -------- QUANTITATIVAS --------
            try {
                int nota = Integer.parseInt(ans.getResponse());

                // distribuição por pergunta (nota x quantidade)
                Map<Integer, Long> dist = distribuicaoPorPergunta
                        .computeIfAbsent(q, k -> new TreeMap<>());
                dist.merge(nota, 1L, Long::sum);

                // soma e contagem por pergunta
                somaNotasPergunta.merge(q, (double) nota, Double::sum);
                totalNotasPergunta.merge(q, 1L, Long::sum);

                // global
                somaNotasGlobais += nota;
                totalNotasGlobais++;

                // por avaliação
                if (ans.getRespostaAluno() != null &&
                        ans.getRespostaAluno().getAvaliacaoAplicada() != null) {
                    Long avId = ans.getRespostaAluno().getAvaliacaoAplicada().getId();
                    somaNotasAvaliacao.merge(avId, (long) nota, Long::sum);
                    totalNotasAvaliacao.merge(avId, 1L, Long::sum);
                }

            } catch (NumberFormatException ignored) {
                // se não conseguir converter para número, ignora na parte quantitativa
            }
        }

        // média global
        Double mediaQuantitativa = null;
        if (totalNotasGlobais > 0) {
            mediaQuantitativa = somaNotasGlobais / totalNotasGlobais;
        }

        // média por pergunta
        Map<Question, Double> mediaPergunta = new LinkedHashMap<>();
        for (Map.Entry<Question, Double> e : somaNotasPergunta.entrySet()) {
            Question q = e.getKey();
            double soma = e.getValue();
            long qtd = totalNotasPergunta.getOrDefault(q, 0L);
            if (qtd > 0) {
                mediaPergunta.put(q, soma / qtd);
            }
        }

        // média por avaliação (além da soma)
        Map<Long, Double> mediaNotasAvaliacao = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> e : somaNotasAvaliacao.entrySet()) {
            long soma = e.getValue();
            long qtd = totalNotasAvaliacao.getOrDefault(e.getKey(), 0L);
            if (qtd > 0) {
                mediaNotasAvaliacao.put(e.getKey(), soma / (double) qtd);
            }
        }

        // -------- Adicionar ao model ----------
        model.addAttribute("turmas", turmas);
        model.addAttribute("anos", anos);
        model.addAttribute("avaliacoes", todasAvaliacoes);
        model.addAttribute("avaliacoesFiltradas", avaliacoesFiltradas);

        model.addAttribute("selectedTurmaId", turmaId);
        model.addAttribute("selectedAno", ano);
        model.addAttribute("selectedAvaliacaoId", avaliacaoId);

        model.addAttribute("totalRespostas", totalRespostas);
        model.addAttribute("mediaQuantitativa", mediaQuantitativa);
        model.addAttribute("qtdAvaliacoesConsideradas", qtdAvaliacoesConsideradas);

        model.addAttribute("distribuicaoPorPergunta", distribuicaoPorPergunta);
        model.addAttribute("totalRespostasPergunta", totalRespostasPergunta);
        model.addAttribute("mediaPergunta", mediaPergunta);

        // novos atributos
        model.addAttribute("somaNotasAvaliacao", somaNotasAvaliacao);
        model.addAttribute("mediaNotasAvaliacao", mediaNotasAvaliacao);
        model.addAttribute("respostasQualitativasPorPergunta", respostasQualitativasPorPergunta);

        return "analise/avaliacoes";
    }
}
