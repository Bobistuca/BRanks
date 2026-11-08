package com.branks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagCommand implements CommandExecutor, TabCompleter {
    private final BRanks pl;

    private static final List<String> PUBLIC_SUBS = Arrays.asList("help", "list", "clear");
    private static final List<String> ADMIN_SUBS = Arrays.asList("set", "create", "delete", "text",
            "permission", "rank");
    private static final List<String> RESERVED = Arrays.asList("help", "list", "clear", "set", "create", "delete",
            "text", "permission", "rank", "none");

    public TagCommand(BRanks pl) {
        this.pl = pl;
    }

    private static String sub(String s) {
        s = s.toLowerCase();
        switch (s) {
            case "?": return "help";
            case "ls": return "list";
            case "off": case "remove": case "reset": return "clear";
            case "new": return "create";
            case "del": return "delete";
            case "perm": case "permissions": return "permission";
            default: return s;
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("settag")) {
            String[] n = new String[a.length + 1];
            n[0] = "set";
            System.arraycopy(a, 0, n, 1, a.length);
            a = n;
        }
        if (a.length == 0) {
            menu(s);
            return true;
        }
        String sub = sub(a[0]);
        String[] r = Util.rest(a, 1);

        if (sub.equals("help")) {
            help(s);
            return true;
        }
        if (sub.equals("list")) {
            menu(s);
            return true;
        }
        if (sub.equals("clear")) {
            clearOwn(s);
            return true;
        }
        if (ADMIN_SUBS.contains(sub)) {
            if (!s.hasPermission("branks.admin")) {
                pl.send(s, "no-permission");
                return true;
            }
            switch (sub) {
                case "set": set(s, r); break;
                case "create": create(s, r); break;
                case "delete": delete(s, r); break;
                case "text": text(s, r); break;
                case "permission": permission(s, r); break;
                default: rank(s, r); break;
            }
            return true;
        }
        use(s, a[0]);
        return true;
    }

    private void usage(CommandSender s, String usage) {
        pl.send(s, "usage", "usage", usage);
    }

    private Tag tagOrError(CommandSender s, String id) {
        Tag t = pl.data().tag(id);
        if (t == null) {
            pl.send(s, "tag-not-found", "tag", id);
        }
        return t;
    }

    // ------------------------------------------------------------------ players

    private void help(CommandSender s) {
        pl.sendList(s, "help-tag");
        if (s.hasPermission("branks.admin")) {
            pl.sendList(s, "help-tag-admin");
        }
    }

    /** Main menu: current tag, then every tag split into unlocked and locked. */
    private void menu(CommandSender s) {
        if (pl.data().tags().isEmpty()) {
            pl.send(s, "tag-list-empty");
            return;
        }
        pl.send(s, "tag-list-title");
        if (s instanceof Player) {
            Player p = (Player) s;
            Tag current = pl.data().activeTag(p);
            pl.send(s, "tag-current", "tag", current == null ? pl.msg("tag-none") : current.coloredText());
        }
        List<Tag> unlocked = new ArrayList<Tag>();
        List<Tag> locked = new ArrayList<Tag>();
        for (Tag t : pl.data().tags().values()) {
            boolean free = !(s instanceof Player) || pl.data().canUse((Player) s, t);
            (free ? unlocked : locked).add(t);
        }
        if (!unlocked.isEmpty()) {
            pl.send(s, "tag-section-available");
            for (Tag t : unlocked) {
                pl.send(s, "tag-line-available", "id", t.id, "text", t.coloredText());
            }
        }
        if (!locked.isEmpty()) {
            pl.send(s, "tag-section-locked");
            for (Tag t : locked) {
                pl.send(s, "tag-line-locked", "id", t.id, "text", t.coloredText(),
                        "requirement", requirement(t));
            }
        }
        pl.send(s, "tag-footer");
    }

    private String requirement(Tag t) {
        Rank required = pl.data().rank(t.rank);
        String rankPart = t.rank.isEmpty() ? null
                : pl.msg("tag-requires-rank", "rank", required == null ? t.rank : required.coloredName());
        String permPart = t.permission.isEmpty() ? null : pl.msg("tag-requires-permission");
        if (rankPart != null && permPart != null) {
            return rankPart + pl.msg("tag-requires-or") + permPart;
        }
        return rankPart != null ? rankPart : (permPart != null ? permPart : "");
    }

    private void use(CommandSender s, String id) {
        if (!(s instanceof Player)) {
            pl.send(s, "players-only");
            return;
        }
        Player p = (Player) s;
        if (!p.hasPermission("branks.tag.use")) {
            pl.send(s, "no-permission");
            return;
        }
        Tag t = pl.data().tag(id);
        if (t == null) {
            pl.send(s, "tag-not-found", "tag", id);
            return;
        }
        if (!pl.data().canUse(p, t)) {
            pl.send(s, "tag-locked", "text", t.coloredText(), "requirement", requirement(t));
            return;
        }
        pl.data().setTag(p.getUniqueId(), p.getName(), t.id);
        pl.applier().apply(p);
        pl.send(s, "tag-selected", "text", t.coloredText());
    }

    private void clearOwn(CommandSender s) {
        if (!(s instanceof Player)) {
            pl.send(s, "players-only");
            return;
        }
        Player p = (Player) s;
        if (pl.data().chosenTag(p.getUniqueId()) == null) {
            pl.send(s, "tag-not-using");
            return;
        }
        pl.data().setTag(p.getUniqueId(), p.getName(), null);
        pl.applier().apply(p);
        pl.send(s, "tag-cleared");
    }

    // ------------------------------------------------------------------ admin

    private void set(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/tag set <player> <tag|none>");
            return;
        }
        Util.Target target = Util.resolve(pl, r[0]);
        if (target == null) {
            pl.send(s, "player-not-found", "player", r[0]);
            return;
        }
        Tag t = pl.data().tag(r[1]);
        if (t == null) {
            if (Util.isEmptyOrNone(r[1])) {
                pl.data().setTag(target.uuid, target.name, null);
                if (target.online != null) {
                    pl.applier().apply(target.online);
                }
                pl.send(s, "tag-reset", "player", target.name);
                return;
            }
            pl.send(s, "tag-not-found", "tag", r[1]);
            return;
        }
        pl.data().setTag(target.uuid, target.name, t.id);
        if (target.online != null) {
            pl.applier().apply(target.online);
        }
        pl.send(s, "tag-set", "player", target.name, "text", t.coloredText());
        if (target.online != null && !pl.data().canUse(target.online, t)) {
            pl.send(s, "tag-set-warning", "requirement", requirement(t));
        }
    }

    private void create(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/tag create <name> <text>");
            return;
        }
        if (!Util.isValidId(r[0])) {
            pl.send(s, "invalid-name");
            return;
        }
        if (RESERVED.contains(r[0].toLowerCase())) {
            pl.send(s, "reserved-name");
            return;
        }
        if (pl.data().tag(r[0]) != null) {
            pl.send(s, "tag-exists", "tag", r[0]);
            return;
        }
        Tag t = pl.data().createTag(r[0].toLowerCase(), Util.join(r, 1));
        pl.send(s, "tag-created", "tag", t.id, "text", t.coloredText());
    }

    private void delete(CommandSender s, String[] r) {
        if (r.length < 1) {
            usage(s, "/tag delete <tag>");
            return;
        }
        Tag t = tagOrError(s, r[0]);
        if (t == null) {
            return;
        }
        pl.data().deleteTag(t.id);
        pl.applier().applyAll();
        pl.send(s, "tag-deleted", "tag", t.id);
    }

    private void text(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/tag text <tag> <text>");
            return;
        }
        Tag t = tagOrError(s, r[0]);
        if (t == null) {
            return;
        }
        t.text = Util.join(r, 1);
        pl.data().saveTags();
        pl.applier().applyAll();
        pl.send(s, "tag-edited", "tag", t.id, "field", "text", "value", t.coloredText());
    }

    private void permission(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/tag permission <tag> <permission|none>");
            return;
        }
        Tag t = tagOrError(s, r[0]);
        if (t == null) {
            return;
        }
        t.permission = Util.isEmptyOrNone(r[1]) ? "" : r[1].toLowerCase();
        pl.data().saveTags();
        pl.applier().applyAll();
        pl.send(s, "tag-edited", "tag", t.id, "field", "permission",
                "value", t.permission.isEmpty() ? "(none)" : t.permission);
    }

    private void rank(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/tag rank <tag> <rank|none>");
            return;
        }
        Tag t = tagOrError(s, r[0]);
        if (t == null) {
            return;
        }
        String value = "";
        if (!Util.isEmptyOrNone(r[1])) {
            Rank rank = pl.data().rank(r[1]);
            if (rank == null) {
                pl.send(s, "rank-not-found", "rank", r[1]);
                return;
            }
            value = rank.id;
        }
        t.rank = value;
        pl.data().saveTags();
        pl.applier().applyAll();
        Rank shown = pl.data().rank(value);
        pl.send(s, "tag-edited", "tag", t.id, "field", "rank",
                "value", shown == null ? "(none)" : pl.msg("tag-requires-rank", "rank", shown.coloredName()));
    }

    // ------------------------------------------------------------------ tab

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        List<String> none = new ArrayList<String>();
        boolean admin = s.hasPermission("branks.admin");
        if (c.getName().equalsIgnoreCase("settag")) {
            if (!admin) {
                return none;
            }
            if (a.length == 1) {
                return Util.filter(pl.onlineNames(), a[0]);
            }
            if (a.length == 2) {
                return Util.filter(tagIds(true), a[1]);
            }
            return none;
        }
        if (a.length == 1) {
            List<String> l = new ArrayList<String>();
            for (Tag t : pl.data().tags().values()) {
                if (!(s instanceof Player) || pl.data().canUse((Player) s, t)) {
                    l.add(t.id);
                }
            }
            l.addAll(PUBLIC_SUBS);
            if (admin) {
                l.addAll(ADMIN_SUBS);
            }
            return Util.filter(l, a[0]);
        }
        if (!admin) {
            return none;
        }
        String sub = sub(a[0]);
        if (sub.equals("set")) {
            if (a.length == 2) {
                return Util.filter(pl.onlineNames(), a[1]);
            }
            if (a.length == 3) {
                return Util.filter(tagIds(true), a[2]);
            }
        } else if ((sub.equals("delete") || sub.equals("text") || sub.equals("permission") || sub.equals("rank"))
                && a.length == 2) {
            return Util.filter(tagIds(false), a[1]);
        } else if (sub.equals("permission") && a.length == 3) {
            return Util.filter(Arrays.asList("none"), a[2]);
        } else if (sub.equals("rank") && a.length == 3) {
            List<String> l = new ArrayList<String>(pl.data().ranks().keySet());
            l.add("none");
            return Util.filter(l, a[2]);
        }
        return none;
    }

    private List<String> tagIds(boolean withNone) {
        List<String> l = new ArrayList<String>(pl.data().tags().keySet());
        if (withNone) {
            l.add("none");
        }
        return l;
    }
}
