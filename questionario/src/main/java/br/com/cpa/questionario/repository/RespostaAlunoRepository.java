package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.Aluno;
import br.com.cpa.questionario.model.AvaliacaoAplicada;
import br.com.cpa.questionario.model.RespostaAluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RespostaAlunoRepository extends JpaRepository<RespostaAluno, Long> {

    boolean existsByAlunoAndAvaliacaoAplicada(Aluno aluno, AvaliacaoAplicada avaliacaoAplicada);

    // Para buscar todas as respostas de uma avaliação
    List<RespostaAluno> findByAvaliacaoAplicadaId(Long avaliacaoAplicadaId);

    // Para apagar em cascata a partir da avaliação
    void deleteByAvaliacaoAplicada(AvaliacaoAplicada avaliacaoAplicada);
}
