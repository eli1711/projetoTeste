package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    List<Turma> findByAnoAndSemestre(int ano, int semestre);

    Optional<Turma> findByNome(String nome);
}
