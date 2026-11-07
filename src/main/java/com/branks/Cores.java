package com.branks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilitarios de cor: &-codes, nomes (pt/en) e hex, com fallback para versoes antigas. */
public final class Cores {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PURO = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final Pattern CODIGOS = Pattern.compile("(&[0-9a-fA-Fk-oK-OrR])+");
    private static final Map<String, String> NOMES = new HashMap<String, String>();
    private static final int[][] PALETA = {
            {0, 0, 0}, {0, 0, 170}, {0, 170, 0}, {0, 170, 170},
            {170, 0, 0}, {170, 0, 170}, {255, 170, 0}, {170, 170, 170},
            {85, 85, 85}, {85, 85, 255}, {85, 255, 85}, {85, 255, 255},
            {255, 85, 85}, {255, 85, 255}, {255, 255, 85}, {255, 255, 255}};
    private static final String PALETA_COD = "0123456789abcdef";
    private static final boolean MODERNO = detectarModerno();

    static {
        nome("&0", "preto", "black");
        nome("&1", "azul-escuro", "dark-blue", "azulescuro");
        nome("&2", "verde-escuro", "dark-green", "verdeescuro");
        nome("&3", "ciano", "dark-aqua", "azul-petroleo");
        nome("&4", "vermelho-escuro", "dark-red", "vermelhoescuro");
        nome("&5", "roxo", "dark-purple", "purple");
        nome("&6", "dourado", "gold", "laranja", "orange");
        nome("&7", "cinza", "gray", "grey");
        nome("&8", "cinza-escuro", "dark-gray", "dark-grey", "cinzaescuro");
        nome("&9", "azul", "blue");
        nome("&a", "verde", "green", "verde-claro", "lime");
        nome("&b", "azul-claro", "aqua", "azulclaro", "ciano-claro");
        nome("&c", "vermelho", "red", "vermelho-claro");
        nome("&d", "rosa", "light-purple", "magenta", "pink");
        nome("&e", "amarelo", "yellow");
        nome("&f", "branco", "white");
    }

    private Cores() {
    }

    private static void nome(String codigo, String... nomes) {
        for (String n : nomes) {
            NOMES.put(n, codigo);
        }
    }

    private static boolean detectarModerno() {
        try {
            String[] p = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            int maior = Integer.parseInt(p[0]);
            if (maior != 1) {
                return maior > 1;
            }
            int menor = p.length > 1 ? Integer.parseInt(p[1]) : 0;
            return menor >= 16;
        } catch (Exception e) {
            return false;
        }
    }

    /** Traduz &-codes (e &#RRGGBB) para o formato do Minecraft. */
    public static String traduzir(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            String rep = MODERNO ? hexModerno(hex) : "§" + maisProximo(hex);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /** Converte nome/codigo/hex em um codigo no formato "&c" ou "&#RRGGBB" (vazio se invalido). */
    public static String codigo(String cor) {
        if (cor == null) {
            return "";
        }
        String c = cor.trim();
        if (c.isEmpty()) {
            return "";
        }
        if (c.startsWith("&")) {
            return c;
        }
        Matcher m = HEX_PURO.matcher(c);
        if (m.matches()) {
            return "&#" + m.group(1);
        }
        String cod = NOMES.get(normalizar(c));
        return cod == null ? "" : cod;
    }

    public static boolean valida(String cor) {
        if (cor == null) {
            return false;
        }
        String c = cor.trim();
        if (c.isEmpty()) {
            return false;
        }
        if (HEX_PURO.matcher(c).matches()) {
            return true;
        }
        if (c.startsWith("&")) {
            return HEX.matcher(c).matches() || CODIGOS.matcher(c).matches();
        }
        return NOMES.containsKey(normalizar(c));
    }

    public static java.util.Set<String> nomesDisponiveis() {
        return NOMES.keySet();
    }

    /** Corta o texto no limite sem deixar um codigo de cor pela metade. */
    public static String limitar(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        String r = s.substring(0, max);
        if (r.endsWith("§")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }

    private static String normalizar(String c) {
        return c.toLowerCase().replace('_', '-').replace(' ', '-');
    }

    private static String hexModerno(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char ch : hex.toCharArray()) {
            sb.append('§').append(ch);
        }
        return sb.toString();
    }

    private static char maisProximo(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int melhor = 0;
        long menor = Long.MAX_VALUE;
        for (int i = 0; i < PALETA.length; i++) {
            long dr = r - PALETA[i][0], dg = g - PALETA[i][1], db = b - PALETA[i][2];
            long d = dr * dr + dg * dg + db * db;
            if (d < menor) {
                menor = d;
                melhor = i;
            }
        }
        return PALETA_COD.charAt(melhor);
    }
}
