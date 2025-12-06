package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.AvaliacaoAplicada;
import br.com.cpa.questionario.model.StatusAvaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvaliacaoAplicadaRepository extends JpaRepository<AvaliacaoAplicada, Long> {

    List<AvaliacaoAplicada> findByTurmaIdAndStatus(Long turmaId, StatusAvaliacao status);
}
