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
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9_-]{1,16}$");

    private Util() {
    }

    static boolean isValidId(String s) {
        return VALID_ID.matcher(s).matches();
    }

    static String join(String[] a, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < a.length; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    static String[] rest(String[] a, int from) {
        return from >= a.length ? new String[0] : Arrays.copyOfRange(a, from, a.length);
    }

    static boolean isEmptyOrNone(String s) {
        String t = s.trim().toLowerCase();
        return t.isEmpty() || t.equals("none") || t.equals("nothing") || t.equals("clear") || t.equals("-");
    }

    static List<String> filter(List<String> options, String start) {
        List<String> r = new ArrayList<String>();
        String s = start.toLowerCase();
        for (String o : options) {
            if (o.toLowerCase().startsWith(s)) {
                r.add(o);
            }
        }
        return r;
    }

    /** Target player of a command (online, or someone who joined the server before). */
    static final class Target {
        final UUID uuid;
        final String name;
        final Player online;

        Target(UUID uuid, String name, Player online) {
            this.uuid = uuid;
            this.name = name;
            this.online = online;
        }
    }

    @SuppressWarnings("deprecation")
    static Target resolve(BRanks pl, String name) {
        Player on = Bukkit.getPlayerExact(name);
        if (on != null) {
            return new Target(on.getUniqueId(), on.getName(), on);
        }
        for (Map.Entry<UUID, PlayerData> e : pl.data().players().entrySet()) {
            if (name.equalsIgnoreCase(e.getValue().name)) {
                return new Target(e.getKey(), e.getValue().name, null);
            }
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op != null && op.hasPlayedBefore()) {
            return new Target(op.getUniqueId(), op.getName() == null ? name : op.getName(), null);
        }
        return null;
    }
}
