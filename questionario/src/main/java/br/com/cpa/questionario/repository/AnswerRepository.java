package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // ---- QUESTIONÁRIO "PURO" (sem AvaliaçãoAplicada) ----

    // usado ao responder /questionnaires/{id}/respond
    void deleteByQuestionQuestionnaireIdAndUserUsernameAndRespostaAlunoIsNull(Long questionnaireId, String userUsername);

    // respostas agrupadas por questionário (somente as que não vieram de AvaliacaoAplicada)
    List<Answer> findByQuestionQuestionnaireIdAndRespostaAlunoIsNull(Long questionnaireId);

    // respostas do usuário para aquele questionário "puro"
    List<Answer> findByQuestionQuestionnaireIdAndUserUsernameAndRespostaAlunoIsNull(Long questionnaireId, String userUsername);

    // ---- AVALIAÇÃO APLICADA (respostas ligadas a RespostaAluno) ----

    // todas as respostas de uma AvaliacaoAplicada específica
    List<Answer> findByRespostaAlunoAvaliacaoAplicadaId(Long avaliacaoId);
}
