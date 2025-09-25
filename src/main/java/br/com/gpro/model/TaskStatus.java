package br.com.gpro.model;

public enum TaskStatus {
    FAZER("A fazer"),
    ANDAMENTO("Em andamento"),
    BLOQUEADO("Bloqueado"),
    REVISANDO("Em revisão"),
    CONCLUIDO("Concluído");

    private final String pt;
    TaskStatus(String pt){ this.pt = pt; }
    public String pt(){ return pt; }

}