package br.com.cpa.questionario.model;

public enum ItemAvaliacao {

    CONTEUDO_COERENCIA,
    CUMPRIMENTO_OBJETIVOS,
    CUMPRIMENTO_HORARIO,
    OBJETIVIDADE_DOCENTE,
    RELACIONAMENTO_INTERPESSOAL,
    ATUACAO_COORDENACAO,
    ATENDIMENTO_SECRETARIA,
    ADEQUACAO_MATERIAL,
    ATENDIMENTO_BIBLIOTECA,
    INFRAESTRUTURA_SALAS,
    ATENDIMENTO_CANTINA;

    public String getDescricao() {
        return switch (this) {
            case CONTEUDO_COERENCIA -> "Conteúdo ministrado e coerência com o plano de ensino do módulo";
            case CUMPRIMENTO_OBJETIVOS -> "Cumprimento dos objetivos propostos pelo módulo";
            case CUMPRIMENTO_HORARIO -> "Cumprimento do horário das aulas pelo docente";
            case OBJETIVIDADE_DOCENTE -> "Objetividade e clareza do docente na exposição do conteúdo e esclarecimento de dúvidas";
            case RELACIONAMENTO_INTERPESSOAL -> "Relacionamento interpessoal do docente com os alunos";
            case ATUACAO_COORDENACAO -> "Atuação e postura da coordenação na solução de problemas referentes ao módulo";
            case ATENDIMENTO_SECRETARIA -> "Atendimento na recepção e secretaria da escola";
            case ADEQUACAO_MATERIAL -> "Adequação dos livros e textos ao conteúdo do módulo";
            case ATENDIMENTO_BIBLIOTECA -> "Atendimento na biblioteca";
            case INFRAESTRUTURA_SALAS -> "Limpeza, conservação e infraestrutura das salas e laboratórios";
            case ATENDIMENTO_CANTINA -> "Atendimento da cantina/restaurante";
        };
    }
}
