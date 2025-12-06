package br.com.cpa.questionario.repository;

import br.com.cpa.questionario.model.ImportanciaQuestaoResposta;
import br.com.cpa.questionario.model.RespostaAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportanciaQuestaoRespostaRepository extends JpaRepository<ImportanciaQuestaoResposta, Long> {

    List<ImportanciaQuestaoResposta> findByRespostaAluno(RespostaAluno respostaAluno);

    // ✅ buscar por id sem carregar o objeto inteiro
    List<ImportanciaQuestaoResposta> findByRespostaAlunoId(Long respostaAlunoId);

    // ✅ (Opcional) Mais eficiente: traz questionId + grauImportancia direto
    @Query("""
        select i
        from ImportanciaQuestaoResposta i
        join fetch i.question q
        where i.respostaAluno.id = :respostaAlunoId
    """)
    List<ImportanciaQuestaoResposta> findByRespostaAlunoIdFetchQuestion(@Param("respostaAlunoId") Long respostaAlunoId);
}
