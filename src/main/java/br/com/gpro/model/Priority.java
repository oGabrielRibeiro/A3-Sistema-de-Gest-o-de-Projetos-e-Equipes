package br.com.gpro.model;
public enum Priority {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta"),
    CRITICA("Crítica");

    private final String pt;
    Priority(String pt){ this.pt = pt; }
    public String pt(){ return pt; }
}