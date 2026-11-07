package com.branks;

public class Tag {
    public final String id;
    public volatile String texto;
    /** Permissao necessaria para usar a tag. Vazio = qualquer um pode usar. */
    public volatile String permissao = "";

    public Tag(String id, String texto) {
        this.id = id;
        this.texto = texto;
    }

    public String textoColorido() {
        return Cores.traduzir(texto);
    }
}
