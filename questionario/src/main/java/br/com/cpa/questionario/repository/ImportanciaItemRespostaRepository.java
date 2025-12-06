package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.ImportanciaItemResposta;
import br.com.cpa.questionario.model.RespostaAluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportanciaItemRespostaRepository extends JpaRepository<ImportanciaItemResposta, Long> {

    List<ImportanciaItemResposta> findByRespostaAluno(RespostaAluno respostaAluno);
      List<ImportanciaItemResposta> findByRespostaAlunoId(Long respostaAlunoId);
}
