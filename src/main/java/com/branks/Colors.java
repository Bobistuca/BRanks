package com.branks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Color helpers: &-codes, color names and hex, with a fallback for old versions. */
public final class Colors {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern PLAIN_HEX = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final Pattern CODES = Pattern.compile("(&[0-9a-fA-Fk-oK-OrR])+");
    private static final Map<String, String> NAMES = new LinkedHashMap<String, String>();
    private static final int[][] PALETTE = {
            {0, 0, 0}, {0, 0, 170}, {0, 170, 0}, {0, 170, 170},
            {170, 0, 0}, {170, 0, 170}, {255, 170, 0}, {170, 170, 170},
            {85, 85, 85}, {85, 85, 255}, {85, 255, 85}, {85, 255, 255},
            {255, 85, 85}, {255, 85, 255}, {255, 255, 85}, {255, 255, 255}};
    private static final String PALETTE_CODES = "0123456789abcdef";
    private static final boolean MODERN = detectModern();

    static {
        name("&0", "black");
        name("&1", "dark-blue");
        name("&2", "dark-green");
        name("&3", "dark-aqua", "cyan");
        name("&4", "dark-red");
        name("&5", "dark-purple", "purple");
        name("&6", "gold", "orange");
        name("&7", "gray", "grey");
        name("&8", "dark-gray", "dark-grey");
        name("&9", "blue");
        name("&a", "green", "lime");
        name("&b", "aqua");
        name("&c", "red");
        name("&d", "light-purple", "pink", "magenta");
        name("&e", "yellow");
        name("&f", "white");
    }

    private Colors() {
    }

    private static void name(String code, String... names) {
        for (String n : names) {
            NAMES.put(n, code);
        }
    }

    private static boolean detectModern() {
        try {
            String[] p = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            int major = Integer.parseInt(p[0]);
            if (major != 1) {
                return major > 1;
            }
            int minor = p.length > 1 ? Integer.parseInt(p[1]) : 0;
            return minor >= 16;
        } catch (Exception e) {
            return false;
        }
    }

    /** Translates &-codes (and &#RRGGBB) into Minecraft's format. */
    public static String translate(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            String rep = MODERN ? modernHex(hex) : "§" + closest(hex);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /** Converts a name/code/hex into a "&c" or "&#RRGGBB" code (empty if invalid). */
    public static String code(String color) {
        if (color == null) {
            return "";
        }
        String c = color.trim();
        if (c.isEmpty()) {
            return "";
        }
        if (c.startsWith("&")) {
            return c;
        }
        Matcher m = PLAIN_HEX.matcher(c);
        if (m.matches()) {
            return "&#" + m.group(1);
        }
        String code = NAMES.get(normalize(c));
        return code == null ? "" : code;
    }

    public static boolean isValid(String color) {
        if (color == null) {
            return false;
        }
        String c = color.trim();
        if (c.isEmpty()) {
            return false;
        }
        if (PLAIN_HEX.matcher(c).matches()) {
            return true;
        }
        if (c.startsWith("&")) {
            return HEX.matcher(c).matches() || CODES.matcher(c).matches();
        }
        return NAMES.containsKey(normalize(c));
    }

    public static Set<String> availableNames() {
        return NAMES.keySet();
    }

    /** Cuts the text at the limit without leaving half a color code behind. */
    public static String limit(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        String r = s.substring(0, max);
        if (r.endsWith("§")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }

    private static String normalize(String c) {
        return c.toLowerCase().replace('_', '-').replace(' ', '-');
    }

    private static String modernHex(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char ch : hex.toCharArray()) {
            sb.append('§').append(ch);
        }
        return sb.toString();
    }

    private static char closest(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int best = 0;
        long smallest = Long.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            long dr = r - PALETTE[i][0], dg = g - PALETTE[i][1], db = b - PALETTE[i][2];
            long d = dr * dr + dg * dg + db * db;
            if (d < smallest) {
                smallest = d;
                best = i;
            }
        }
        return PALETTE_CODES.charAt(best);
    }
}
