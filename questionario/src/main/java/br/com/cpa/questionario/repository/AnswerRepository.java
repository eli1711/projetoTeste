package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // ---- QUESTIONÁRIO "PURO" (sem AvaliaçãoAplicada) ----
    void deleteByQuestionQuestionnaireIdAndUserUsernameAndRespostaAlunoIsNull(Long questionnaireId, String userUsername);

    void deleteByRespostaAlunoAvaliacaoAplicadaId(Long avaliacaoId);

    List<Answer> findByQuestionQuestionnaireIdAndRespostaAlunoIsNull(Long questionnaireId);

    List<Answer> findByQuestionQuestionnaireIdAndUserUsernameAndRespostaAlunoIsNull(Long questionnaireId, String userUsername);

    // ---- AVALIAÇÃO APLICADA (respostas ligadas a RespostaAluno) ----
    List<Answer> findByRespostaAlunoAvaliacaoAplicadaId(Long avaliacaoId);

    // ✅ NOVO: todas as respostas de um "envio" (RespostaAluno)
    List<Answer> findByRespostaAlunoId(Long respostaAlunoId);

    // ✅ NOVO: listar todas as respostas de um aluno específico (RA)
    List<Answer> findByRespostaAlunoAlunoRa(String ra);
}
