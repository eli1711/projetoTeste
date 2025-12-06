package br.com.cpa.questionario.model;

public class AnaliseItemPorAlunoDTO {

    private String alunoNome;
    private String alunoRa;
    private ItemAvaliacao item;
    private GrauImportancia importanciaAluno; // BAIXA / MEDIA / ALTA
    private Double mediaNotaItem;             // média 1..4
    private Integer classificacao;            // 1=Ruim, 2=Regular, 3=Bom, 4=Ótimo

    public String getAlunoNome() {
        return alunoNome;
    }

    public void setAlunoNome(String alunoNome) {
        this.alunoNome = alunoNome;
    }

    public String getAlunoRa() {
        return alunoRa;
    }

    public void setAlunoRa(String alunoRa) {
        this.alunoRa = alunoRa;
    }

    public ItemAvaliacao getItem() {
        return item;
    }

    public void setItem(ItemAvaliacao item) {
        this.item = item;
    }

    public GrauImportancia getImportanciaAluno() {
        return importanciaAluno;
    }

    public void setImportanciaAluno(GrauImportancia importanciaAluno) {
        this.importanciaAluno = importanciaAluno;
    }

    public Double getMediaNotaItem() {
        return mediaNotaItem;
    }

    public void setMediaNotaItem(Double mediaNotaItem) {
        this.mediaNotaItem = mediaNotaItem;
    }

    public Integer getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(Integer classificacao) {
        this.classificacao = classificacao;
    }

    // ==========================================================
    // ✅ "PONTE" PARA O THYMELEAF
    // HTML usa r.importancia e compara com 1/2/3.
    // ==========================================================
    public Integer getImportancia() {
        if (importanciaAluno == null) return null;

        switch (importanciaAluno) {
            case BAIXA:
                return 1;
            case MEDIA:
                return 2;
            case ALTA:
                return 3;
            default:
                return null;
        }
    }

    // Opcional: permitir setar por número
    public void setImportancia(Integer v) {
        if (v == null) {
            this.importanciaAluno = null;
            return;
        }

        switch (v) {
            case 1:
                this.importanciaAluno = GrauImportancia.BAIXA;
                break;
            case 2:
                this.importanciaAluno = GrauImportancia.MEDIA;
                break;
            case 3:
                this.importanciaAluno = GrauImportancia.ALTA;
                break;
            default:
                this.importanciaAluno = null;
        }
    }
}
