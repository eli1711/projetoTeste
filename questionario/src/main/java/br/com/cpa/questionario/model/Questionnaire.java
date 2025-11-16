package br.com.cpa.questionario.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Questionnaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // título do questionário

    @Column(columnDefinition = "TEXT")
    private String description; // descrição opcional

    private int semester;       // semestre de origem
    @Column(name = "\"year\"")
    private int year;           // ano de origem

    @Enumerated(EnumType.STRING)
    private StatusDisponibilidade status;

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public StatusDisponibilidade getStatus() { return status; }
    public void setStatus(StatusDisponibilidade status) { this.status = status; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
