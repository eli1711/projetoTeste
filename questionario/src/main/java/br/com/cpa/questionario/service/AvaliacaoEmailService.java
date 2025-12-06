package br.com.cpa.questionario.service;

import br.com.cpa.questionario.model.Aluno;
import br.com.cpa.questionario.model.AvaliacaoAplicada;
import br.com.cpa.questionario.model.StatusAluno;
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

        // AGORA é Aluno, não User
        for (Aluno aluno : avaliacaoAplicada.getTurma().getAlunos()) {

            // segurança: checa se tem User vinculado
            if (aluno.getUser() == null) {
                continue;
            }

            // status fica no User
            if (aluno.getUser().getStatus() == StatusAluno.ATIVO) {

                String token = gerarToken(aluno, avaliacaoAplicada);

                String link = "https://sua-aplicacao.com/avaliacoes/"
                        + avaliacaoAplicada.getId() + "/responder?token=" + token;

                // TODO: enviar e-mail real (JavaMailSender)
                // assunto: "Avaliação CPA - " + avaliacaoAplicada.getQuestionario().getName()
                // corpo: "Olá " + aluno.getNome() + ", acesse sua avaliação em: " + link
            }
        }
    }

    private String gerarToken(Aluno aluno, AvaliacaoAplicada avaliacaoAplicada) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(7, ChronoUnit.DAYS);

        // usa o username do User; se der algum problema, cai pro RA
        String username = (aluno.getUser() != null
                ? aluno.getUser().getUsername()
                : aluno.getRa());

        // Aqui seria um JWT de verdade; por enquanto, simbólico
        return "token-" + username + "-" + avaliacaoAplicada.getId() + "-" + expira.toEpochMilli();
    }
}
