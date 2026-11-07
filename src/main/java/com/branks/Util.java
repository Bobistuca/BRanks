package com.branks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

final class Util {
    private static final Pattern ID_VALIDO = Pattern.compile("^[A-Za-z0-9_-]{1,16}$");

    private Util() {
    }

    static boolean idValido(String s) {
        return ID_VALIDO.matcher(s).matches();
    }

    static String juntar(String[] a, int de) {
        StringBuilder sb = new StringBuilder();
        for (int i = de; i < a.length; i++) {
            if (i > de) {
                sb.append(' ');
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    static String[] resto(String[] a, int de) {
        return de >= a.length ? new String[0] : Arrays.copyOfRange(a, de, a.length);
    }

    static boolean vazioOuNenhum(String s) {
        String t = s.trim().toLowerCase();
        return t.isEmpty() || t.equals("nenhum") || t.equals("nenhuma") || t.equals("none")
                || t.equals("limpar") || t.equals("-");
    }

    static List<String> filtrar(List<String> opcoes, String inicio) {
        List<String> r = new ArrayList<String>();
        String i = inicio.toLowerCase();
        for (String o : opcoes) {
            if (o.toLowerCase().startsWith(i)) {
                r.add(o);
            }
        }
        return r;
    }

    /** Jogador alvo de um comando (online ou que ja entrou alguma vez no servidor). */
    static final class Alvo {
        final UUID uuid;
        final String nome;
        final Player online;

        Alvo(UUID uuid, String nome, Player online) {
            this.uuid = uuid;
            this.nome = nome;
            this.online = online;
        }
    }

    @SuppressWarnings("deprecation")
    static Alvo resolver(BRanks pl, String nome) {
        Player on = Bukkit.getPlayerExact(nome);
        if (on != null) {
            return new Alvo(on.getUniqueId(), on.getName(), on);
        }
        for (Map.Entry<UUID, Jogador> e : pl.dados().jogadores().entrySet()) {
            if (nome.equalsIgnoreCase(e.getValue().nome)) {
                return new Alvo(e.getKey(), e.getValue().nome, null);
            }
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(nome);
        if (op != null && op.hasPlayedBefore()) {
            return new Alvo(op.getUniqueId(), op.getName() == null ? nome : op.getName(), null);
        }
        return null;
    }
}
