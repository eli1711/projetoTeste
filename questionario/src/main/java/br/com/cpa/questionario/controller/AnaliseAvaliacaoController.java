package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.*;
import br.com.cpa.questionario.repository.AvaliacaoAplicadaRepository;
import br.com.cpa.questionario.repository.AnswerRepository;
import br.com.cpa.questionario.repository.ImportanciaItemRespostaRepository;
import br.com.cpa.questionario.repository.ImportanciaQuestaoRespostaRepository;
import br.com.cpa.questionario.repository.RespostaAlunoRepository;
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
    private final ImportanciaItemRespostaRepository importanciaItemRespostaRepository;

    // ✅ NOVO
    private final RespostaAlunoRepository respostaAlunoRepository;
    private final ImportanciaQuestaoRespostaRepository importanciaQuestaoRespostaRepository;

    public AnaliseAvaliacaoController(TurmaRepository turmaRepository,
                                      AvaliacaoAplicadaRepository avaliacaoAplicadaRepository,
                                      AnswerRepository answerRepository,
                                      ImportanciaItemRespostaRepository importanciaItemRespostaRepository,
                                      RespostaAlunoRepository respostaAlunoRepository,
                                      ImportanciaQuestaoRespostaRepository importanciaQuestaoRespostaRepository) {
        this.turmaRepository = turmaRepository;
        this.avaliacaoAplicadaRepository = avaliacaoAplicadaRepository;
        this.answerRepository = answerRepository;
        this.importanciaItemRespostaRepository = importanciaItemRespostaRepository;
        this.respostaAlunoRepository = respostaAlunoRepository;
        this.importanciaQuestaoRespostaRepository = importanciaQuestaoRespostaRepository;
    }

    @GetMapping("/avaliacoes")
    public String analiseAvaliacoes(@RequestParam(required = false) Long turmaId,
                                    @RequestParam(required = false) Integer ano,
                                    @RequestParam(required = false) Long avaliacaoId,
                                    // ✅ NOVO: filtro por envio do aluno
                                    @RequestParam(required = false) Long respostaAlunoId,
                                    Model model) {

        List<Turma> turmas = turmaRepository.findAll();
        List<AvaliacaoAplicada> todasAvaliacoes = avaliacaoAplicadaRepository.findAll();

        Set<Integer> anosSet = todasAvaliacoes.stream()
                .map(a -> a.getQuestionario().getYear())
                .collect(Collectors.toCollection(TreeSet::new));
        List<Integer> anos = new ArrayList<>(anosSet);

        List<AvaliacaoAplicada> avaliacoesFiltradas = todasAvaliacoes.stream()
                .filter(a -> turmaId == null || a.getTurma().getId().equals(turmaId))
                .filter(a -> ano == null || a.getQuestionario().getYear() == ano)
                .filter(a -> avaliacaoId == null || a.getId().equals(avaliacaoId))
                .toList();

        long qtdAvaliacoesConsideradas = avaliacoesFiltradas.size();

        // ===========================
        // Respostas (geral)
        // ===========================
        List<Answer> respostas = new ArrayList<>();
        for (AvaliacaoAplicada av : avaliacoesFiltradas) {
            List<Answer> respostasAvaliacao =
                    answerRepository.findByRespostaAlunoAvaliacaoAplicadaId(av.getId());

            if (respostasAvaliacao != null) {
                respostasAvaliacao.stream()
                        .filter(a -> a.getRespostaAluno() != null)
                        .forEach(respostas::add);
            }
        }

        long totalRespostas = respostas.size();

        Map<Question, Map<Integer, Long>> distribuicaoPorPergunta = new LinkedHashMap<>();
        Map<Question, Long> totalRespostasPergunta = new LinkedHashMap<>();
        Map<Question, Double> somaNotasPergunta = new LinkedHashMap<>();
        Map<Question, Long> totalNotasPergunta = new LinkedHashMap<>();

        Map<Question, List<String>> respostasQualitativasPorPergunta = new LinkedHashMap<>();

        Map<Long, Long> somaNotasAvaliacao = new LinkedHashMap<>();
        Map<Long, Long> totalNotasAvaliacao = new LinkedHashMap<>();

        double somaNotasGlobais = 0.0;
        long totalNotasGlobais = 0L;

        Map<RespostaAluno, Map<ItemAvaliacao, List<Integer>>> notasPorAlunoItem = new HashMap<>();

        for (Answer ans : respostas) {
            Question q = ans.getQuestion();
            totalRespostasPergunta.merge(q, 1L, Long::sum);

            boolean isQuant = (q.getType() == QuestionType.QUANTITATIVA);

            if (!isQuant) {
                respostasQualitativasPorPergunta
                        .computeIfAbsent(q, k -> new ArrayList<>())
                        .add(ans.getResponse());
                continue;
            }

            try {
                int nota = Integer.parseInt(ans.getResponse());

                Map<Integer, Long> dist = distribuicaoPorPergunta
                        .computeIfAbsent(q, k -> new TreeMap<>());
                dist.merge(nota, 1L, Long::sum);

                somaNotasPergunta.merge(q, (double) nota, Double::sum);
                totalNotasPergunta.merge(q, 1L, Long::sum);

                somaNotasGlobais += nota;
                totalNotasGlobais++;

                if (ans.getRespostaAluno() != null &&
                        ans.getRespostaAluno().getAvaliacaoAplicada() != null) {
                    Long avId = ans.getRespostaAluno().getAvaliacaoAplicada().getId();
                    somaNotasAvaliacao.merge(avId, (long) nota, Long::sum);
                    totalNotasAvaliacao.merge(avId, 1L, Long::sum);
                }

                if (ans.getRespostaAluno() != null && q.getItemAvaliacao() != null) {
                    RespostaAluno ra = ans.getRespostaAluno();
                    ItemAvaliacao item = q.getItemAvaliacao();

                    // ⚠️ Se ItemAvaliacao NÃO for enum, troque EnumMap por HashMap.
                    notasPorAlunoItem
                            .computeIfAbsent(ra, k -> new EnumMap<>(ItemAvaliacao.class))
                            .computeIfAbsent(item, k -> new ArrayList<>())
                            .add(nota);
                }

            } catch (NumberFormatException ignored) {
            }
        }

        Double mediaQuantitativa = null;
        if (totalNotasGlobais > 0) {
            mediaQuantitativa = somaNotasGlobais / totalNotasGlobais;
        }

        Map<Question, Double> mediaPergunta = new LinkedHashMap<>();
        for (Map.Entry<Question, Double> e : somaNotasPergunta.entrySet()) {
            Question q = e.getKey();
            double soma = e.getValue();
            long qtd = totalNotasPergunta.getOrDefault(q, 0L);
            if (qtd > 0) {
                mediaPergunta.put(q, soma / qtd);
            }
        }

        Map<Long, Double> mediaNotasAvaliacao = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> e : somaNotasAvaliacao.entrySet()) {
            long soma = e.getValue();
            long qtd = totalNotasAvaliacao.getOrDefault(e.getKey(), 0L);
            if (qtd > 0) {
                mediaNotasAvaliacao.put(e.getKey(), soma / (double) qtd);
            }
        }

        List<AnaliseItemPorAlunoDTO> analisePorAluno = new ArrayList<>();
        for (Map.Entry<RespostaAluno, Map<ItemAvaliacao, List<Integer>>> entry : notasPorAlunoItem.entrySet()) {
            RespostaAluno ra = entry.getKey();
            Map<ItemAvaliacao, List<Integer>> porItem = entry.getValue();

            var importancias = importanciaItemRespostaRepository.findByRespostaAluno(ra);

            Map<ItemAvaliacao, GrauImportancia> importanciaPorItem =
                    new EnumMap<>(ItemAvaliacao.class);

            for (ImportanciaItemResposta imp : importancias) {
                importanciaPorItem.put(imp.getItem(), imp.getGrauImportancia());
            }

            for (Map.Entry<ItemAvaliacao, List<Integer>> e : porItem.entrySet()) {
                ItemAvaliacao item = e.getKey();
                List<Integer> notas = e.getValue();
                if (notas.isEmpty()) continue;

                double soma = 0;
                for (Integer n : notas) soma += n;
                double media = soma / notas.size();

                int classificacao;
                if (media < 1.5) classificacao = 1;
                else if (media < 2.5) classificacao = 2;
                else if (media < 3.5) classificacao = 3;
                else classificacao = 4;

                AnaliseItemPorAlunoDTO dto = new AnaliseItemPorAlunoDTO();
                dto.setAlunoNome(ra.getAluno().getNome());
                dto.setAlunoRa(ra.getAluno().getRa());
                dto.setItem(item);
                dto.setImportanciaAluno(importanciaPorItem.get(item));
                dto.setMediaNotaItem(media);
                dto.setClassificacao(classificacao);

                analisePorAluno.add(dto);
            }
        }

        // =====================================================
        // ✅ NOVO: filtro/relatório de respostas DO ALUNO
        // =====================================================
        // Lista de "envios" (RespostaAluno) disponíveis de acordo com o filtro de avaliações
        List<RespostaAluno> enviosDisponiveis = new ArrayList<>();
        for (AvaliacaoAplicada av : avaliacoesFiltradas) {
            enviosDisponiveis.addAll(respostaAlunoRepository.findByAvaliacaoAplicadaId(av.getId()));
        }
        // ordena por nome
        enviosDisponiveis.sort(Comparator.comparing(r -> {
            if (r.getAluno() == null) return "";
            if (r.getAluno().getNome() == null) return "";
            return r.getAluno().getNome().toLowerCase(Locale.ROOT);
        }));

        List<RelatorioRespostaAlunoDTO> relatorioAluno = List.of();
        Double mediaAluno = null;        // média só de quantitativas 1..4
        Double mediaAlunoComZero = null; // incluindo 0

        if (respostaAlunoId != null) {

            List<Answer> answersAluno = answerRepository.findByRespostaAlunoId(respostaAlunoId);
            List<ImportanciaQuestaoResposta> importancias =
                    importanciaQuestaoRespostaRepository.findByRespostaAlunoId(respostaAlunoId);

            // Map (questionId -> GrauImportancia)
            Map<Long, GrauImportancia> impPorQuestao = new HashMap<>();
            for (ImportanciaQuestaoResposta imp : importancias) {
                if (imp.getQuestion() != null && imp.getQuestion().getId() != null) {
                    impPorQuestao.put(imp.getQuestion().getId(), imp.getGrauImportancia());
                }
            }

            List<RelatorioRespostaAlunoDTO> linhas = new ArrayList<>();

            double soma14 = 0;
            long qtd14 = 0;

            double somaCom0 = 0;
            long qtdCom0 = 0;

            for (Answer a : answersAluno) {
                Question q = a.getQuestion();
                if (q == null) continue;

                RelatorioRespostaAlunoDTO dto = new RelatorioRespostaAlunoDTO();
                dto.setPerguntaId(q.getId());
                dto.setPerguntaTexto(q.getText());
                dto.setTipo(q.getType());
                dto.setItem(q.getItemAvaliacao());
                dto.setResposta(a.getResponse());

                if (q.getType() == QuestionType.QUANTITATIVA) {
                    dto.setImportancia(impPorQuestao.get(q.getId()));

                    // número da resposta para marcar o checkbox
                    Integer nota = tryParseInt(a.getResponse());
                    dto.setRespostaNumero(nota);

                    // labels das alternativas (fallback)
                    dto.setOption1Label(safe(q.getOption1Label(), "Discordo totalmente"));
                    dto.setOption2Label(safe(q.getOption2Label(), "Discordo parcialmente"));
                    dto.setOption3Label(safe(q.getOption3Label(), "Concordo parcialmente"));
                    dto.setOption4Label(safe(q.getOption4Label(), "Concordo totalmente"));
                    dto.setOption5Label(safe(q.getOption5Label(), "N/A"));

                    // médias (1..4) e (0..4)
                    if (nota != null) {
                        if (nota >= 0 && nota <= 4) {
                            somaCom0 += nota;
                            qtdCom0++;
                        }
                        if (nota >= 1 && nota <= 4) {
                            soma14 += nota;
                            qtd14++;
                        }
                    }
                }

                linhas.add(dto);
            }

            // ordena por item + pergunta
            linhas.sort(Comparator
                    .comparing((RelatorioRespostaAlunoDTO d) -> d.getItem() != null ? d.getItem().toString() : "")
                    .thenComparing(d -> d.getPerguntaId() != null ? d.getPerguntaId() : 0L));

            relatorioAluno = linhas;

            if (qtd14 > 0) mediaAluno = soma14 / qtd14;
            if (qtdCom0 > 0) mediaAlunoComZero = somaCom0 / qtdCom0;
        }

        // ===========================
        // model
        // ===========================
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

        model.addAttribute("somaNotasAvaliacao", somaNotasAvaliacao);
        model.addAttribute("mediaNotasAvaliacao", mediaNotasAvaliacao);
        model.addAttribute("respostasQualitativasPorPergunta", respostasQualitativasPorPergunta);

        model.addAttribute("analisePorAluno", analisePorAluno);

        // ✅ NOVO
        model.addAttribute("enviosDisponiveis", enviosDisponiveis);
        model.addAttribute("selectedRespostaAlunoId", respostaAlunoId);
        model.addAttribute("relatorioAluno", relatorioAluno);
        model.addAttribute("mediaAluno", mediaAluno);
        model.addAttribute("mediaAlunoComZero", mediaAlunoComZero);

        return "analise/avaliacoes";
    }

    private static Integer tryParseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    private static String safe(String v, String fallback) {
        if (v == null || v.isBlank()) return fallback;
        return v;
    }
}
