package br.com.cpa.questionario.service;

import br.com.cpa.questionario.model.*;
import br.com.cpa.questionario.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AvaliacaoImportacaoService {

    private final AvaliacaoAplicadaRepository avaliacaoRepo;
    private final RespostaAlunoRepository respostaAlunoRepo;
    private final AlunoRepository alunoRepo;
    private final QuestionRepository questionRepo;

    private final ImportanciaQuestaoRespostaRepository importanciaQuestaoRepo;
    private final ImportanciaItemRespostaRepository importanciaItemRepo;

    public AvaliacaoImportacaoService(
            AvaliacaoAplicadaRepository avaliacaoRepo,
            RespostaAlunoRepository respostaAlunoRepo,
            AlunoRepository alunoRepo,
            QuestionRepository questionRepo,
            ImportanciaQuestaoRespostaRepository importanciaQuestaoRepo,
            ImportanciaItemRespostaRepository importanciaItemRepo
    ) {
        this.avaliacaoRepo = avaliacaoRepo;
        this.respostaAlunoRepo = respostaAlunoRepo;
        this.alunoRepo = alunoRepo;
        this.questionRepo = questionRepo;
        this.importanciaQuestaoRepo = importanciaQuestaoRepo;
        this.importanciaItemRepo = importanciaItemRepo;
    }

    @Transactional
    public void importarCsvEApagar(Long avaliacaoId, MultipartFile file) {
        AvaliacaoAplicada avaliacao = avaliacaoRepo.findById(avaliacaoId)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada: " + avaliacaoId));

        importarCsv(file, avaliacao);

        deletarAvaliacaoComSeguranca(avaliacaoId);
    }

    private void importarCsv(MultipartFile file, AvaliacaoAplicada avaliacao) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String header = br.readLine();
            if (header == null) throw new RuntimeException("CSV vazio");

            // Índice alunoId -> RespostaAluno (carrega 1x)
            List<RespostaAluno> existentes = respostaAlunoRepo.findByAvaliacaoAplicadaId(avaliacao.getId());
            Map<Long, RespostaAluno> respostaPorAlunoId = new HashMap<>();
            for (RespostaAluno ra : existentes) {
                if (ra.getAluno() != null && ra.getAluno().getId() != null) {
                    respostaPorAlunoId.put(ra.getAluno().getId(), ra);
                }
                if (ra.getRespostas() == null) ra.setRespostas(new ArrayList<>());
            }

            String line;
            int lineNo = 1; // header
            while ((line = br.readLine()) != null) {
                lineNo++;
                final int currentLineNo = lineNo; // ✅ agora é "final" para mensagens/erros

                if (line.isBlank()) continue;

                String[] c = line.split(";", -1);

                Long alunoId = parseLong(c, 0);
                Long questionId = parseLong(c, 1);
                String answerValue = parseString(c, 2);
                String grauQuestaoStr = parseString(c, 3);
                String itemStr = parseString(c, 4);
                String grauItemStr = parseString(c, 5);

                if (alunoId == null) {
                    throw new RuntimeException("Linha " + currentLineNo + " inválida (aluno_id obrigatório): " + line);
                }

                // ✅ sem lambda capturando lineNo
                Aluno aluno = alunoRepo.findById(alunoId)
                        .orElseThrow(() -> new RuntimeException("Linha " + currentLineNo + ": Aluno não encontrado: " + alunoId));

                RespostaAluno respostaAluno = respostaPorAlunoId.get(alunoId);
                if (respostaAluno == null) {
                    respostaAluno = new RespostaAluno();
                    respostaAluno.setAluno(aluno);
                    respostaAluno.setAvaliacaoAplicada(avaliacao);
                    respostaAluno.setDataResposta(LocalDateTime.now());
                    respostaAluno.setStatusResposta(StatusResposta.RESPONDIDO);
                    respostaAluno.setRespostas(new ArrayList<>());

                    // precisa salvar para ter ID (Importancia* usa resposta_aluno_id)
                    respostaAluno = respostaAlunoRepo.save(respostaAluno);
                    respostaPorAlunoId.put(alunoId, respostaAluno);
                } else if (respostaAluno.getRespostas() == null) {
                    respostaAluno.setRespostas(new ArrayList<>());
                }

                // (A) Answer
                if (questionId != null && answerValue != null && !answerValue.isBlank()) {
                    Question q = questionRepo.findById(questionId)
                            .orElseThrow(() -> new RuntimeException("Linha " + currentLineNo + ": Question não encontrada: " + questionId));

                    Answer ans = new Answer();
                    ans.setQuestion(q);
                    ans.setResponse(answerValue); // ✅ seu entity tem setResponse
                    ans.setRespostaAluno(respostaAluno);

                    // ✅ obrigatório no Answer: userUsername (nullable=false)
                    // Ajuste essa parte se seu Aluno não tiver getUser()
                    String username = null;
                    try {
                        // se existir aluno.getUser().getUsername()
                        if (aluno.getUser() != null) {
                            username = aluno.getUser().getUsername();
                        }
                    } catch (Exception ignored) {
                        // se não existir, você precisa adaptar conforme seu modelo
                    }

                    if (username == null || username.isBlank()) {
                        throw new RuntimeException("Linha " + currentLineNo + ": não foi possível obter username para o aluno " + alunoId
                                + " (campo Answer.userUsername é obrigatório)");
                    }

                    ans.setUserUsername(username);

                    respostaAluno.getRespostas().add(ans);

                    // cascade em RespostaAluno.respostas salva Answer
                    respostaAlunoRepo.save(respostaAluno);
                }

                // (B) Importância por Questão
                if (questionId != null && grauQuestaoStr != null && !grauQuestaoStr.isBlank()) {
                    Question q = questionRepo.findById(questionId)
                            .orElseThrow(() -> new RuntimeException("Linha " + currentLineNo + ": Question não encontrada: " + questionId));

                    GrauImportancia grauQuestao = parseGrauImportancia(grauQuestaoStr, currentLineNo, "grau_importancia_questao");

                    ImportanciaQuestaoResposta impQ = new ImportanciaQuestaoResposta();
                    impQ.setQuestion(q);
                    impQ.setRespostaAluno(respostaAluno);
                    impQ.setGrauImportancia(grauQuestao);

                    importanciaQuestaoRepo.save(impQ);
                }

                // (C) Importância por Item
                if (itemStr != null && !itemStr.isBlank() && grauItemStr != null && !grauItemStr.isBlank()) {
                    GrauImportancia grauItem = parseGrauImportancia(grauItemStr, currentLineNo, "grau_importancia_item");

                    ItemAvaliacao item;
                    try {
                        item = ItemAvaliacao.valueOf(itemStr.trim().toUpperCase());
                    } catch (Exception ex) {
                        throw new RuntimeException("Linha " + currentLineNo + ": item inválido: '" + itemStr + "'");
                    }

                    ImportanciaItemResposta impI = new ImportanciaItemResposta();
                    impI.setRespostaAluno(respostaAluno);
                    impI.setItem(item);
                    impI.setGrauImportancia(grauItem);

                    importanciaItemRepo.save(impI);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao importar CSV: " + e.getMessage(), e);
        }
    }

    private void deletarAvaliacaoComSeguranca(Long avaliacaoId) {
        List<RespostaAluno> respostas = respostaAlunoRepo.findByAvaliacaoAplicadaId(avaliacaoId);

        // apaga filhos que geram FK
        for (RespostaAluno ra : respostas) {
            if (ra.getId() == null) continue;
            importanciaQuestaoRepo.deleteAll(importanciaQuestaoRepo.findByRespostaAlunoId(ra.getId()));
            importanciaItemRepo.deleteAll(importanciaItemRepo.findByRespostaAlunoId(ra.getId()));
        }

        // apaga respostas (Answer vai junto por cascade/orphanRemoval em RespostaAluno.respostas)
        respostaAlunoRepo.deleteAll(respostas);

        // apaga avaliação
        avaliacaoRepo.deleteById(avaliacaoId);
    }

    private static GrauImportancia parseGrauImportancia(String raw, int lineNo, String campo) {
        try {
            return GrauImportancia.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new RuntimeException("Linha " + lineNo + ": " + campo + " inválido: '" + raw + "'");
        }
    }

    private static Long parseLong(String[] c, int idx) {
        if (idx >= c.length) return null;
        String v = c[idx];
        if (v == null) return null;
        v = v.trim();
        if (v.isEmpty()) return null;
        return Long.parseLong(v);
    }

    private static String parseString(String[] c, int idx) {
        if (idx >= c.length) return null;
        String v = c[idx];
        return v == null ? null : v.trim();
    }
}
