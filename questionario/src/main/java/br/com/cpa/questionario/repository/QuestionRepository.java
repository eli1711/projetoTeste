package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuestionnaireId(Long questionnaireId);
}
