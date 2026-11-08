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

/** Applies rank/tag to players: permissions, tab list, nametag and formatting. */
public class Applier {
    private static final Pattern BRANKS_TEAM = Pattern.compile("^br\\d\\d.*");

    private final BRanks pl;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<UUID, PermissionAttachment>();
    private final Map<UUID, String> teams = new HashMap<UUID, String>();

    public Applier(BRanks pl) {
        this.pl = pl;
    }

    public void applyAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            apply(p);
        }
    }

    public void apply(Player p) {
        permissions(p);
        tab(p);
        nametag(p);
    }

    /** Removes everything the plugin put on players (used on reload/disable). */
    public void removeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            remove(p, true);
        }
        clearTeams();
    }

    public void remove(Player p, boolean resetTab) {
        removeAttachment(p);
        removeFromTeam(p);
        if (resetTab) {
            try {
                p.setPlayerListName(p.getName());
            } catch (Exception ignored) {
            }
        }
        attachments.remove(p.getUniqueId());
        teams.remove(p.getUniqueId());
    }

    // ------------------------------------------------------------------ permissions

    private void removeAttachment(Player p) {
        PermissionAttachment a = attachments.remove(p.getUniqueId());
        if (a != null) {
            try {
                p.removeAttachment(a);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void permissions(Player p) {
        removeAttachment(p);
        if (!pl.cfg().bool("permissions.enabled")) {
            return;
        }
        Rank r = pl.data().rankOf(p.getUniqueId());
        if (r == null) {
            return;
        }
        Map<String, Boolean> map = pl.data().permissions(r);
        if (map.isEmpty()) {
            return;
        }
        PermissionAttachment a = p.addAttachment(pl);
        // "*" first, so explicit negations (-perm) still win
        for (Map.Entry<String, Boolean> e : map.entrySet()) {
            if (e.getKey().equals("*") && e.getValue()) {
                for (Permission perm : Bukkit.getPluginManager().getPermissions()) {
                    a.setPermission(perm.getName(), true);
                }
                a.setPermission("*", true);
            }
        }
        for (Map.Entry<String, Boolean> e : map.entrySet()) {
            if (!e.getKey().equals("*")) {
                a.setPermission(e.getKey(), e.getValue());
            }
        }
        attachments.put(p.getUniqueId(), a);
    }

    // ------------------------------------------------------------------ tab list

    private void tab(Player p) {
        if (!pl.cfg().bool("tablist.enabled")) {
            return;
        }
        String full = Colors.translate(render(pl.cfg().text("tablist.format"), p, false));
        Rank r = pl.data().rankOf(p.getUniqueId());
        String colorOnly = Colors.translate(r == null ? "" : Colors.code(r.color)) + p.getName();
        // old versions limit the tab name to 16 characters: try from the fullest to the simplest
        List<String> candidates = new ArrayList<String>();
        candidates.add(full);
        candidates.add(colorOnly);
        candidates.add(p.getName());
        for (String c : candidates) {
            try {
                p.setPlayerListName(c);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ nametag

    private void nametag(Player p) {
        removeFromTeam(p);
        if (!pl.cfg().bool("nametag.enabled")) {
            return;
        }
        Rank r = pl.data().rankOf(p.getUniqueId());
        if (r == null) {
            return;
        }
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            String name = teamName(r);
            Team t = sb.getTeam(name);
            if (t == null) {
                t = sb.registerNewTeam(name);
            }
            String prefix = Colors.translate(withSpace(r.prefix) + Colors.code(r.color));
            String suffix = r.suffix.isEmpty() ? "" : Colors.translate("&r " + r.suffix.trim());
            setPrefix(t, prefix);
            setSuffix(t, suffix);
            t.addEntry(p.getName());
            teams.put(p.getUniqueId(), name);
        } catch (Exception e) {
            pl.getLogger().warning("Could not update the nametag of " + p.getName() + ": " + e.getMessage());
        }
    }

    private void setPrefix(Team t, String v) {
        try {
            t.setPrefix(v);
        } catch (IllegalArgumentException e) {
            t.setPrefix(Colors.limit(v, 16));
        }
    }

    private void setSuffix(Team t, String v) {
        try {
            t.setSuffix(v);
        } catch (IllegalArgumentException e) {
            t.setSuffix(Colors.limit(v, 16));
        }
    }

    private String teamName(Rank r) {
        String id = r.id.length() > 12 ? r.id.substring(0, 12) : r.id;
        return String.format("br%02d%s", Math.min(pl.data().indexOf(r), 99), id);
    }

    private void removeFromTeam(Player p) {
        String name = teams.remove(p.getUniqueId());
        if (name == null) {
            return;
        }
        try {
            Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name);
            if (t != null) {
                t.removeEntry(p.getName());
            }
        } catch (Exception ignored) {
        }
    }

    /** Deletes the teams created by BRanks (including leftovers from earlier runs). */
    public void clearTeams() {
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            List<Team> toRemove = new ArrayList<Team>();
            for (Team t : sb.getTeams()) {
                if (matchesTeam(t.getName())) {
                    toRemove.add(t);
                }
            }
            for (Team t : toRemove) {
                t.unregister();
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean matchesTeam(String name) {
        return BRANKS_TEAM.matcher(name).matches();
    }

    // ------------------------------------------------------------------ format

    public static String withSpace(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.endsWith(" ") ? s : s + " ";
    }

    private static String esc(String s, boolean chat) {
        return chat ? s.replace("%", "%%") : s;
    }

    /**
     * Replaces placeholders ({player}, {tag}, ...) with values. The result still has &-codes.
     * In chat mode the text comes out ready for String.format (%1$s = player, %2$s = message).
     */
    public String render(String format, Player p, boolean chat) {
        if (format == null) {
            return "";
        }
        DataStore d = pl.data();
        Rank r = d.rankOf(p.getUniqueId());
        Tag t = d.activeTag(p);
        String s = esc(format, chat);
        s = s.replace("{tag}", t == null ? "" : esc(withSpace(t.text), chat));
        s = s.replace("{prefix}", r == null ? "" : esc(withSpace(r.prefix), chat));
        s = s.replace("{suffix}", r == null || r.suffix.isEmpty() ? "" : esc(" " + r.suffix.trim(), chat));
        s = s.replace("{rank}", r == null ? "" : esc(r.name, chat));
        s = s.replace("{color}", r == null ? "" : esc(Colors.code(r.color), chat));
        s = s.replace("{chatcolor}", r == null ? "" : esc(Colors.code(r.chatColor), chat));
        s = s.replace("{player}", chat ? "%1$s" : p.getName());
        return s;
    }
}
