package br.com.cpa.questionario.model;

import jakarta.persistence.*;

@Entity
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String response;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // username do usuário que respondeu (serve tanto para questionário direto quanto para avaliação aplicada)
    @Column(name = "user_username", nullable = false)
    private String userUsername;

    // se a resposta veio de uma AvaliacaoAplicada, este campo terá valor
    @ManyToOne
    @JoinColumn(name = "resposta_aluno_id")
    private RespostaAluno respostaAluno;

    // GETTERS / SETTERS ...

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }

    public RespostaAluno getRespostaAluno() { return respostaAluno; }
    public void setRespostaAluno(RespostaAluno respostaAluno) { this.respostaAluno = respostaAluno; }
}
