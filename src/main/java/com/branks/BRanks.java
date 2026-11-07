package com.branks;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BRanks extends JavaPlugin {
    private Config config;
    private Dados dados;
    private Aplicador aplicador;

    @Override
    public void onEnable() {
        config = new Config(this);
        config.carregar();
        dados = new Dados(this);
        dados.carregar();
        aplicador = new Aplicador(this);

        ComandoRank rank = new ComandoRank(this);
        ComandoTag tag = new ComandoTag(this);
        registrar("rank", rank, rank);
        registrar("setrank", rank, rank);
        registrar("tag", tag, tag);
        registrar("settag", tag, tag);

        Bukkit.getPluginManager().registerEvents(new Eventos(this), this);

        aplicador.limparTimes();
        aplicador.aplicarTodos();
        getLogger().info("BRanks ativado: " + dados.ranks().size() + " ranks, " + dados.tags().size() + " tags.");
    }

    @Override
    public void onDisable() {
        if (aplicador != null) {
            aplicador.removerTudo();
        }
        if (dados != null) {
            dados.salvarJogadores();
        }
    }

    private void registrar(String nome, org.bukkit.command.CommandExecutor ex, org.bukkit.command.TabCompleter tc) {
        PluginCommand c = getCommand(nome);
        if (c != null) {
            c.setExecutor(ex);
            c.setTabCompleter(tc);
        }
    }

    public void recarregar() {
        aplicador.removerTudo();
        config.carregar();
        dados.carregar();
        aplicador.aplicarTodos();
    }

    public Config cfg() {
        return config;
    }

    public Dados dados() {
        return dados;
    }

    public Aplicador aplicador() {
        return aplicador;
    }

    // ------------------------------------------------------------------ mensagens

    /** Mensagem do config.yml com variaveis: msg("chave", "jogador", "Steve", "rank", "VIP"). */
    public String msg(String chave, String... pares) {
        String t = config.texto("mensagens." + chave);
        if (t == null) {
            t = "&c[mensagem ausente: " + chave + "]";
        }
        return aplicarVars(t, pares);
    }

    public static String aplicarVars(String t, String... pares) {
        for (int i = 0; i + 1 < pares.length; i += 2) {
            t = t.replace("{" + pares[i] + "}", pares[i + 1] == null ? "" : pares[i + 1]);
        }
        return Cores.traduzir(t);
    }

    public void enviar(CommandSender s, String chave, String... pares) {
        s.sendMessage(msg(chave, pares));
    }

    public void enviarLista(CommandSender s, String chave, String... pares) {
        for (String linha : config.lista("mensagens." + chave)) {
            s.sendMessage(aplicarVars(linha, pares));
        }
    }

    public List<String> nomesOnline() {
        List<String> l = new ArrayList<String>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            l.add(p.getName());
        }
        return l;
    }
}
