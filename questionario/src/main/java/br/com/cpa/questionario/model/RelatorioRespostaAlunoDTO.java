package br.com.cpa.questionario.model;

public class RelatorioRespostaAlunoDTO {

    private Long perguntaId;
    private String perguntaTexto;
    private QuestionType tipo;
    private ItemAvaliacao item;

    private String resposta;        // bruto (Answer.response)
    private Integer respostaNumero; // 0..4 (para marcar checkbox)

    // ✅ importância por QUESTÃO (BAIXA/MEDIA/ALTA)
    private GrauImportancia importancia;

    // labels das alternativas (somente quantitativa)
    private String option1Label;
    private String option2Label;
    private String option3Label;
    private String option4Label;
    private String option5Label;

    private Double mediaQuestaoAvaliacao;

    public Long getPerguntaId() { return perguntaId; }
    public void setPerguntaId(Long perguntaId) { this.perguntaId = perguntaId; }

    public String getPerguntaTexto() { return perguntaTexto; }
    public void setPerguntaTexto(String perguntaTexto) { this.perguntaTexto = perguntaTexto; }

    public QuestionType getTipo() { return tipo; }
    public void setTipo(QuestionType tipo) { this.tipo = tipo; }

    public ItemAvaliacao getItem() { return item; }
    public void setItem(ItemAvaliacao item) { this.item = item; }

    public String getResposta() { return resposta; }
    public void setResposta(String resposta) { this.resposta = resposta; }

    public Integer getRespostaNumero() { return respostaNumero; }
    public void setRespostaNumero(Integer respostaNumero) { this.respostaNumero = respostaNumero; }

    public GrauImportancia getImportancia() { return importancia; }
    public void setImportancia(GrauImportancia importancia) { this.importancia = importancia; }

    public String getOption1Label() { return option1Label; }
    public void setOption1Label(String option1Label) { this.option1Label = option1Label; }

    public String getOption2Label() { return option2Label; }
    public void setOption2Label(String option2Label) { this.option2Label = option2Label; }

    public String getOption3Label() { return option3Label; }
    public void setOption3Label(String option3Label) { this.option3Label = option3Label; }

    public String getOption4Label() { return option4Label; }
    public void setOption4Label(String option4Label) { this.option4Label = option4Label; }

    public String getOption5Label() { return option5Label; }
    public void setOption5Label(String option5Label) { this.option5Label = option5Label; }

    public Double getMediaQuestaoAvaliacao() { return mediaQuestaoAvaliacao; }
    public void setMediaQuestaoAvaliacao(Double mediaQuestaoAvaliacao) { this.mediaQuestaoAvaliacao = mediaQuestaoAvaliacao; }

    // ==========================================================
    // ✅ PONTE PARA O THYMELEAF: BAIXA/MEDIA/ALTA -> 1/2/3
    // Use no HTML como: r.importanciaValor
    // ==========================================================
    public Integer getImportanciaValor() {
        if (importancia == null) return null;
        switch (importancia) {
            case BAIXA: return 1;
            case MEDIA: return 2;
            case ALTA:  return 3;
            default:    return null;
        }
    }

    public void setImportanciaValor(Integer v) {
        if (v == null) { this.importancia = null; return; }
        switch (v) {
            case 1: this.importancia = GrauImportancia.BAIXA; break;
            case 2: this.importancia = GrauImportancia.MEDIA; break;
            case 3: this.importancia = GrauImportancia.ALTA;  break;
            default: this.importancia = null;
        }
    }
}
