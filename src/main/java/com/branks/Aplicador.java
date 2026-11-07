package com.branks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Aplica o rank/tag nos jogadores: permissoes, tab, nametag e formatacao. */
public class Aplicador {
    private static final Pattern TIME_BRANKS = Pattern.compile("^br\\d\\d.*");

    private final BRanks pl;
    private final Map<UUID, PermissionAttachment> anexos = new HashMap<UUID, PermissionAttachment>();
    private final Map<UUID, String> times = new HashMap<UUID, String>();

    public Aplicador(BRanks pl) {
        this.pl = pl;
    }

    public void aplicarTodos() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            aplicar(p);
        }
    }

    public void aplicar(Player p) {
        permissoes(p);
        tab(p);
        nametag(p);
    }

    /** Remove tudo que o plugin colocou nos jogadores (usado ao recarregar/desligar). */
    public void removerTudo() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            remover(p, true);
        }
        limparTimes();
    }

    public void remover(Player p, boolean resetarTab) {
        removerAnexo(p);
        removerDoTime(p);
        if (resetarTab) {
            try {
                p.setPlayerListName(p.getName());
            } catch (Exception ignored) {
            }
        }
        anexos.remove(p.getUniqueId());
        times.remove(p.getUniqueId());
    }

    // ------------------------------------------------------------------ permissoes

    private void removerAnexo(Player p) {
        PermissionAttachment a = anexos.remove(p.getUniqueId());
        if (a != null) {
            try {
                p.removeAttachment(a);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void permissoes(Player p) {
        removerAnexo(p);
        if (!pl.cfg().bool("permissoes.ativar")) {
            return;
        }
        Rank r = pl.dados().rankDe(p.getUniqueId());
        if (r == null) {
            return;
        }
        Map<String, Boolean> mapa = pl.dados().permissoes(r);
        if (mapa.isEmpty()) {
            return;
        }
        PermissionAttachment a = p.addAttachment(pl);
        // "*" primeiro, para que negacoes explicitas (-perm) ainda vençam
        for (Map.Entry<String, Boolean> e : mapa.entrySet()) {
            if (e.getKey().equals("*") && e.getValue()) {
                for (Permission perm : Bukkit.getPluginManager().getPermissions()) {
                    a.setPermission(perm.getName(), true);
                }
                a.setPermission("*", true);
            }
        }
        for (Map.Entry<String, Boolean> e : mapa.entrySet()) {
            if (!e.getKey().equals("*")) {
                a.setPermission(e.getKey(), e.getValue());
            }
        }
        anexos.put(p.getUniqueId(), a);
    }

    // ------------------------------------------------------------------ tab

    private void tab(Player p) {
        if (!pl.cfg().bool("tablist.ativar")) {
            return;
        }
        String completo = Cores.traduzir(renderizar(pl.cfg().texto("tablist.formato"), p, false));
        Rank r = pl.dados().rankDe(p.getUniqueId());
        String soCor = Cores.traduzir(r == null ? "" : Cores.codigo(r.cor)) + p.getName();
        // versoes antigas limitam o nome da tab a 16 caracteres: tenta do mais completo ao mais simples
        List<String> candidatos = new ArrayList<String>();
        candidatos.add(completo);
        candidatos.add(soCor);
        candidatos.add(p.getName());
        for (String c : candidatos) {
            try {
                p.setPlayerListName(c);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ nametag

    private void nametag(Player p) {
        removerDoTime(p);
        if (!pl.cfg().bool("nametag.ativar")) {
            return;
        }
        Rank r = pl.dados().rankDe(p.getUniqueId());
        if (r == null) {
            return;
        }
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            String nome = nomeTime(r);
            Team t = sb.getTeam(nome);
            if (t == null) {
                t = sb.registerNewTeam(nome);
            }
            String prefixo = Cores.traduzir(comEspaco(r.prefixo) + Cores.codigo(r.cor));
            String sufixo = r.sufixo.isEmpty() ? "" : Cores.traduzir("&r " + r.sufixo.trim());
            definirPrefixo(t, prefixo);
            definirSufixo(t, sufixo);
            t.addEntry(p.getName());
            times.put(p.getUniqueId(), nome);
        } catch (Exception e) {
            pl.getLogger().warning("Nao foi possivel atualizar a nametag de " + p.getName() + ": " + e.getMessage());
        }
    }

    private void definirPrefixo(Team t, String v) {
        try {
            t.setPrefix(v);
        } catch (IllegalArgumentException e) {
            t.setPrefix(Cores.limitar(v, 16));
        }
    }

    private void definirSufixo(Team t, String v) {
        try {
            t.setSuffix(v);
        } catch (IllegalArgumentException e) {
            t.setSuffix(Cores.limitar(v, 16));
        }
    }

    private String nomeTime(Rank r) {
        String id = r.id.length() > 12 ? r.id.substring(0, 12) : r.id;
        return String.format("br%02d%s", Math.min(pl.dados().indice(r), 99), id);
    }

    private void removerDoTime(Player p) {
        String nome = times.remove(p.getUniqueId());
        if (nome == null) {
            return;
        }
        try {
            Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(nome);
            if (t != null) {
                t.removeEntry(p.getName());
            }
        } catch (Exception ignored) {
        }
    }

    /** Apaga os times criados pelo BRanks (inclusive restos de execucoes anteriores). */
    public void limparTimes() {
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            List<Team> remover = new ArrayList<Team>();
            for (Team t : sb.getTeams()) {
                if (TIME_BRANKS.matcher(t.getName()).matches()) {
                    remover.add(t);
                }
            }
            for (Team t : remover) {
                t.unregister();
            }
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ formato

    public static String comEspaco(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.endsWith(" ") ? s : s + " ";
    }

    private static String esc(String s, boolean chat) {
        return chat ? s.replace("%", "%%") : s;
    }

    /**
     * Troca os placeholders ({jogador}, {tag}, ...) por valores. O resultado ainda tem &-codes.
     * No modo chat o texto ja sai pronto para String.format (%1$s = jogador, %2$s = mensagem).
     */
    public String renderizar(String formato, Player p, boolean chat) {
        if (formato == null) {
            return "";
        }
        Dados d = pl.dados();
        Rank r = d.rankDe(p.getUniqueId());
        Tag t = d.tagAtiva(p);
        String s = esc(formato, chat);
        s = s.replace("{tag}", t == null ? "" : esc(comEspaco(t.texto), chat));
        s = s.replace("{prefixo}", r == null ? "" : esc(comEspaco(r.prefixo), chat));
        s = s.replace("{sufixo}", r == null || r.sufixo.isEmpty() ? "" : esc(" " + r.sufixo.trim(), chat));
        s = s.replace("{rank}", r == null ? "" : esc(r.nome, chat));
        s = s.replace("{cor}", r == null ? "" : esc(Cores.codigo(r.cor), chat));
        s = s.replace("{corchat}", r == null ? "" : esc(Cores.codigo(r.corChat), chat));
        s = s.replace("{jogador}", chat ? "%1$s" : p.getName());
        return s;
    }
}
