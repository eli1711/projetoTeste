package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.AvaliacaoAplicada;
import br.com.cpa.questionario.model.RespostaAluno;
import br.com.cpa.questionario.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RespostaAlunoRepository extends JpaRepository<RespostaAluno, Long> {

    boolean existsByAlunoAndAvaliacaoAplicada(User aluno, AvaliacaoAplicada avaliacaoAplicada);

    List<RespostaAluno> findByAvaliacaoAplicadaId(Long avaliacaoId);
}
