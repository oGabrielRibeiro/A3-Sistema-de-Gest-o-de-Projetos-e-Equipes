package br.com.gpro.model;
public enum ProjectStatus {
    PLANEJADO("Planejado"),
    ANDAMENTO("Em andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String pt;
    ProjectStatus(String pt){ this.pt = pt; }
    public String pt(){ return pt; }
}