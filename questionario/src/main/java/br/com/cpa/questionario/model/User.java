package br.com.cpa.questionario.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String username; // login (no nosso caso, RA)

    private String password;

    private String name;

    @Column(nullable = false)
    private String email;    // e-mail institucional

    @Column(unique = true)
    private String ra;       // RA / matrícula

    private String role;     // ROLE_ADMIN, ROLE_PROFESSOR, ROLE_ALUNO

    @Enumerated(EnumType.STRING)
    private StatusAluno status = StatusAluno.ATIVO;

    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRa() { return ra; }
    public void setRa(String ra) { this.ra = ra; }

    public String getRole() { return role; }
    public void setRole(String role) {
        if (role == null) {
            this.role = "ROLE_USER";
            return;
        }
        this.role = role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    public StatusAluno getStatus() { return status; }
    public void setStatus(StatusAluno status) { this.status = status; }

    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }
}
