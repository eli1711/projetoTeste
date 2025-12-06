package br.com.cpa.questionario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
 
@Entity
public class ImportanciaQuestaoResposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrauImportancia grauImportancia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resposta_aluno_id")
    private RespostaAluno respostaAluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GrauImportancia getGrauImportancia() { return grauImportancia; }
    public void setGrauImportancia(GrauImportancia grauImportancia) { this.grauImportancia = grauImportancia; }

    public RespostaAluno getRespostaAluno() { return respostaAluno; }
    public void setRespostaAluno(RespostaAluno respostaAluno) { this.respostaAluno = respostaAluno; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}