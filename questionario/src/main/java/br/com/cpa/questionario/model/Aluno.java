package br.com.cpa.questionario.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aluno")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome completo do aluno
    @Column(nullable = false)
    private String nome;

    // RA do aluno (também é o username no User)
    @Column(nullable = false, unique = true)
    private String ra;

    // CPF (usado como base para a senha inicial)
    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String email;

    // Relacionamento 1-1 com User (tabela de login)
    @OneToOne(optional = false)
    @JoinColumn(name = "user_username", referencedColumnName = "username")
    private User user;

    // Turma (pode ser null até o aluno definir no primeiro login)
    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma;

    // ========= GETTERS / SETTERS =========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getRa() {
        return ra;
    }

    public void setRa(String ra) {
        this.ra = ra;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }
}
