package com.branks;

public class Jogador {
    public volatile String nome;
    /** id do rank definido manualmente (null = usa o rank padrao). */
    public volatile String rank;
    public volatile String tag;

    public Jogador(String nome) {
        this.nome = nome;
    }
}
