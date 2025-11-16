package br.com.cpa.questionario.service;

import br.com.cpa.questionario.model.AvaliacaoAplicada;
import br.com.cpa.questionario.model.StatusAluno;
import br.com.cpa.questionario.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AvaliacaoEmailService {

    // Aqui você pode injetar JavaMailSender, configs de e-mail e JWT de verdade

    public void enviarConvites(AvaliacaoAplicada avaliacaoAplicada) {
        if (avaliacaoAplicada.getTurma() == null ||
            avaliacaoAplicada.getTurma().getAlunos() == null) {
            return;
        }

        for (User aluno : avaliacaoAplicada.getTurma().getAlunos()) {
            if (aluno.getStatus() == StatusAluno.ATIVO) {
                String token = gerarToken(aluno, avaliacaoAplicada);
                String link = "https://sua-aplicacao.com/avaliacoes/"
                        + avaliacaoAplicada.getId() + "/responder?token=" + token;

                // TODO: enviar e-mail real (JavaMailSender)
                // assunto: "Avaliação CPA - " + avaliacaoAplicada.getQuestionario().getName()
                // corpo: "Olá, acesse sua avaliação em: " + link
            }
        }
    }

    private String gerarToken(User aluno, AvaliacaoAplicada avaliacaoAplicada) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(7, ChronoUnit.DAYS);

        // Aqui seria gerado um JWT de verdade;
        // por enquanto retornamos uma string simbólica
        return "token-" + aluno.getUsername() + "-" + avaliacaoAplicada.getId() + "-" + expira.toEpochMilli();
    }
}
