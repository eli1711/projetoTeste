package br.com.cpa.questionario.controller;

import br.com.cpa.questionario.model.Answer;
import br.com.cpa.questionario.model.RespostaAluno;
import br.com.cpa.questionario.repository.RespostaAlunoRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoCsvController {

    private final RespostaAlunoRepository respostaAlunoRepo;

    public AvaliacaoCsvController(RespostaAlunoRepository respostaAlunoRepo) {
        this.respostaAlunoRepo = respostaAlunoRepo;
    }

    @GetMapping("/{avaliacaoId}/csv-template")
    public ResponseEntity<byte[]> baixarCsvTemplate(@PathVariable Long avaliacaoId) {
        List<RespostaAluno> respostas = respostaAlunoRepo.findByAvaliacaoAplicadaId(avaliacaoId);

        // Ordena pra ficar bem legível
        respostas.sort(Comparator.comparing(ra -> ra.getAluno().getNome(), String.CASE_INSENSITIVE_ORDER));

        StringBuilder csv = new StringBuilder();

        // Cabeçalho novo:
        csv.append("aluno_nome;questao_texto;resposta\n");

        for (RespostaAluno ra : respostas) {
            String alunoNome = safe(ra.getAluno() != null ? ra.getAluno().getNome() : null);

            List<Answer> ansList = ra.getRespostas();
            if (ansList == null || ansList.isEmpty()) {
                // se você quiser listar mesmo sem resposta, descomente:
                // csv.append(alunoNome).append(";;\n");
                continue;
            }

            // ordena por id da questão (opcional)
            ansList.sort(Comparator.comparing(a -> a.getQuestion().getId()));

            for (Answer ans : ansList) {
                String questaoTexto = safe(ans.getQuestion() != null ? ans.getQuestion().getText() : null);
                String resposta = safe(ans.getResponse());

                csv.append(alunoNome).append(";")
                   .append(questaoTexto).append(";")
                   .append(resposta).append("\n");
            }
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"avaliacao-" + avaliacaoId + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private static String safe(String s) {
        if (s == null) return "";
        // CSV com ;: precisamos evitar quebrar linha e ; no conteúdo
        return s.replace("\n", " ")
                .replace("\r", " ")
                .replace(";", ",")
                .trim();
    }
}
