package com.branks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Rank {
    public final String id;
    public volatile String nome;
    public volatile String prefixo = "";
    public volatile String sufixo = "";
    public volatile String cor = "";
    public volatile String corChat = "";
    public volatile boolean padrao;
    public final List<String> permissoes = new CopyOnWriteArrayList<String>();
    public final List<String> herda = new CopyOnWriteArrayList<String>();

    public Rank(String id) {
        this.id = id;
        this.nome = id;
    }

    /** Nome de exibicao ja colorido. */
    public String nomeColorido() {
        return Cores.traduzir(Cores.codigo(cor) + nome);
    }
}
