package br.com.cpa.questionario.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;    // ex: ADS 2º Semestre 2025
    private String curso;
    private int semestre;
    private int ano;

    @OneToMany(mappedBy = "turma")
    private List<User> alunos;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public List<User> getAlunos() { return alunos; }
    public void setAlunos(List<User> alunos) { this.alunos = alunos; }
}
