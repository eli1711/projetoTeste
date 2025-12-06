package br.com.cpa.questionario.model;

import jakarta.persistence.*;

@Entity
public class ImportanciaItemResposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "grau_importancia", nullable = false)
    private GrauImportancia grauImportancia;

    @Enumerated(EnumType.STRING)
    @Column(name = "item", nullable = false)
    private ItemAvaliacao item;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resposta_aluno_id")
    private RespostaAluno respostaAluno;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GrauImportancia getGrauImportancia() { return grauImportancia; }
    public void setGrauImportancia(GrauImportancia grauImportancia) { this.grauImportancia = grauImportancia; }

    public ItemAvaliacao getItem() { return item; }
    public void setItem(ItemAvaliacao item) { this.item = item; }

    public RespostaAluno getRespostaAluno() { return respostaAluno; }
    public void setRespostaAluno(RespostaAluno respostaAluno) { this.respostaAluno = respostaAluno; }
}
