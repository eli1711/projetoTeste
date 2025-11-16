package br.com.cpa.questionario.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class AvaliacaoAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @ManyToOne(optional = false)
    @JoinColumn(name = "questionnaire_id")
    private Questionnaire questionario;

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    @Enumerated(EnumType.STRING)
    private StatusAvaliacao status = StatusAvaliacao.ABERTA;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }

    public Questionnaire getQuestionario() { return questionario; }
    public void setQuestionario(Questionnaire questionario) { this.questionario = questionario; }

    public LocalDateTime getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDateTime dataInicio) { this.dataInicio = dataInicio; }

    public LocalDateTime getDataFim() { return dataFim; }
    public void setDataFim(LocalDateTime dataFim) { this.dataFim = dataFim; }

    public StatusAvaliacao getStatus() { return status; }
    public void setStatus(StatusAvaliacao status) { this.status = status; }
}
