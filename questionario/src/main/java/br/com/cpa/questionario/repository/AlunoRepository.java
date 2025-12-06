package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByUserUsername(String username);

    Optional<Aluno> findByRa(String ra);
}
