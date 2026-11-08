package com.branks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RankCommand implements CommandExecutor, TabCompleter {
    private final BRanks pl;

    private static final List<String> PUBLIC_SUBS = Arrays.asList("help", "list", "info");
    private static final List<String> ADMIN_SUBS = Arrays.asList("set", "create", "delete", "name",
            "prefix", "suffix", "color", "chatcolor", "permission", "inherit", "default", "move", "reload");

    public RankCommand(BRanks pl) {
        this.pl = pl;
    }

    private static String sub(String s) {
        s = s.toLowerCase();
        switch (s) {
            case "?": return "help";
            case "ls": return "list";
            case "new": return "create";
            case "remove": case "del": return "delete";
            case "chat-color": return "chatcolor";
            case "perm": case "perms": case "permissions": return "permission";
            case "inherits": return "inherit";
            case "rl": return "reload";
            default: return s;
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("setrank")) {
            String[] n = new String[a.length + 1];
            n[0] = "set";
            System.arraycopy(a, 0, n, 1, a.length);
            a = n;
        }
        if (a.length == 0) {
            help(s);
            return true;
        }
        String sub = sub(a[0]);
        String[] r = Util.rest(a, 1);

        if (sub.equals("help")) {
            help(s);
            return true;
        }
        if (sub.equals("list")) {
            list(s);
            return true;
        }
        if (sub.equals("info")) {
            info(s, r);
            return true;
        }
        if (!ADMIN_SUBS.contains(sub)) {
            pl.send(s, "unknown-subcommand", "command", "/rank help");
            return true;
        }
        if (!s.hasPermission("branks.admin")) {
            pl.send(s, "no-permission");
            return true;
        }
        switch (sub) {
            case "set": set(s, r); break;
            case "create": create(s, r); break;
            case "delete": delete(s, r); break;
            case "name": text(s, r, "name"); break;
            case "prefix": text(s, r, "prefix"); break;
            case "suffix": text(s, r, "suffix"); break;
            case "color": color(s, r, false); break;
            case "chatcolor": color(s, r, true); break;
            case "permission": permission(s, r); break;
            case "inherit": inherit(s, r); break;
            case "default": makeDefault(s, r); break;
            case "move": move(s, r); break;
            default:
                pl.reload();
                pl.send(s, "reloaded");
                break;
        }
        return true;
    }

    private void help(CommandSender s) {
        pl.sendList(s, "help-rank");
        if (s.hasPermission("branks.admin")) {
            pl.sendList(s, "help-rank-admin");
        }
    }

    private void usage(CommandSender s, String usage) {
        pl.send(s, "usage", "usage", usage);
    }

    private Rank rankOrError(CommandSender s, String id) {
        Rank r = pl.data().rank(id);
        if (r == null) {
            pl.send(s, "rank-not-found", "rank", id);
        }
        return r;
    }

    // ------------------------------------------------------------------ public

    private void list(CommandSender s) {
        Map<String, Rank> ranks = pl.data().ranks();
        if (ranks.isEmpty()) {
            pl.send(s, "rank-list-empty");
            return;
        }
        pl.send(s, "rank-list-title");
        int position = 1;
        for (Rank r : ranks.values()) {
            pl.send(s, "rank-list-line",
                    "position", String.valueOf(position++),
                    "id", r.id,
                    "prefix", r.prefix.isEmpty() ? "" : Applier.withSpace(r.prefix),
                    "color", Colors.code(r.color),
                    "name", r.name,
                    "default", r.isDefault ? pl.msg("rank-list-default") : "");
        }
    }

    private void info(CommandSender s, String[] r) {
        Util.Target target;
        if (r.length == 0) {
            if (!(s instanceof Player)) {
                usage(s, "/rank info <player>");
                return;
            }
            Player p = (Player) s;
            target = new Util.Target(p.getUniqueId(), p.getName(), p);
        } else {
            target = Util.resolve(pl, r[0]);
            if (target == null) {
                pl.send(s, "player-not-found", "player", r[0]);
                return;
            }
        }
        Rank rank = pl.data().rankOf(target.uuid);
        Tag tag = pl.data().tag(pl.data().chosenTag(target.uuid));
        pl.sendList(s, "info-lines",
                "player", target.name,
                "rank", rank == null ? pl.msg("info-no-rank") : rank.coloredName(),
                "tag", tag == null ? pl.msg("tag-none") : tag.coloredText(),
                "count", String.valueOf(rank == null ? 0 : pl.data().permissions(rank).size()));
    }

    // ------------------------------------------------------------------ admin

    private void set(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/rank set <player> <rank|default>");
            return;
        }
        Util.Target target = Util.resolve(pl, r[0]);
        if (target == null) {
            pl.send(s, "player-not-found", "player", r[0]);
            return;
        }
        Rank rank = pl.data().rank(r[1]);
        if (rank == null) {
            String t = r[1].toLowerCase();
            if (t.equals("default") || Util.isEmptyOrNone(t) || t.equals("reset")) {
                pl.data().setRank(target.uuid, target.name, null);
                if (target.online != null) {
                    pl.applier().apply(target.online);
                }
                pl.send(s, "rank-reset", "player", target.name);
                return;
            }
            pl.send(s, "rank-not-found", "rank", r[1]);
            return;
        }
        pl.data().setRank(target.uuid, target.name, rank.id);
        if (target.online != null) {
            pl.applier().apply(target.online);
            pl.send(target.online, "rank-received", "rank", rank.coloredName());
        }
        pl.send(s, "rank-set", "player", target.name, "rank", rank.coloredName());
    }

    private void create(CommandSender s, String[] r) {
        if (r.length < 1) {
            usage(s, "/rank create <name>");
            return;
        }
        if (!Util.isValidId(r[0])) {
            pl.send(s, "invalid-name");
            return;
        }
        String low = r[0].toLowerCase();
        if (ADMIN_SUBS.contains(low) || PUBLIC_SUBS.contains(low) || low.equals("none") || low.equals("default")) {
            pl.send(s, "reserved-name");
            return;
        }
        if (pl.data().rank(r[0]) != null) {
            pl.send(s, "rank-exists", "rank", r[0]);
            return;
        }
        Rank rank = pl.data().createRank(low, r[0]);
        pl.send(s, "rank-created", "rank", rank.coloredName());
    }

    private void delete(CommandSender s, String[] r) {
        if (r.length < 1) {
            usage(s, "/rank delete <rank>");
            return;
        }
        Rank rank = rankOrError(s, r[0]);
        if (rank == null) {
            return;
        }
        pl.data().deleteRank(rank.id);
        pl.applier().applyAll();
        pl.send(s, "rank-deleted", "rank", rank.id);
    }

    private void text(CommandSender s, String[] r, String field) {
        if (r.length < 2) {
            usage(s, "/rank " + field + " <rank> <text" + (field.equals("name") ? "" : "|none") + ">");
            return;
        }
        Rank rank = rankOrError(s, r[0]);
        if (rank == null) {
            return;
        }
        String value = Util.join(r, 1);
        if (!field.equals("name") && Util.isEmptyOrNone(value)) {
            value = "";
        }
        switch (field) {
            case "name": rank.name = value; break;
            case "prefix": rank.prefix = value; break;
            default: rank.suffix = value; break;
        }
        edited(s, rank, field, value);
    }

    private void color(CommandSender s, String[] r, boolean chat) {
        String field = chat ? "chatcolor" : "color";
        if (r.length < 2) {
            usage(s, "/rank " + field + " <rank> <color|none>");
            return;
        }
        Rank rank = rankOrError(s, r[0]);
        if (rank == null) {
            return;
        }
        String value = Util.join(r, 1).trim();
        if (Util.isEmptyOrNone(value)) {
            value = "";
        } else if (!Colors.isValid(value)) {
            pl.send(s, "invalid-color");
            return;
        }
        if (chat) {
            rank.chatColor = value;
        } else {
            rank.color = value;
        }
        edited(s, rank, field, value.isEmpty() ? "(none)" : Colors.translate(Colors.code(value)) + value);
    }

    private void edited(CommandSender s, Rank rank, String field, String value) {
        pl.data().saveRanks();
        pl.applier().applyAll();
        pl.send(s, "rank-edited", "rank", rank.coloredName(), "field", field,
                "value", value.isEmpty() ? "(empty)" : Colors.translate(value));
    }

    private void permission(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/rank permission <rank> <add|remove|list> [permission]");
            return;
        }
        Rank rank = rankOrError(s, r[0]);
        if (rank == null) {
            return;
        }
        String action = r[1].toLowerCase();
        if (action.equals("list")) {
            pl.send(s, "perm-list", "rank", rank.coloredName(),
                    "permissions", rank.permissions.isEmpty() ? "(none)" : joinList(rank.permissions));
            return;
        }
        if (r.length < 3) {
            usage(s, "/rank permission <rank> <add|remove> <permission>");
            return;
        }
        String perm = r[2].toLowerCase();
        if (action.equals("add")) {
            if (rank.permissions.contains(perm)) {
                pl.send(s, "perm-already", "rank", rank.coloredName());
                return;
            }
            rank.permissions.add(perm);
            pl.send(s, "perm-added", "rank", rank.coloredName(), "permission", perm);
        } else if (action.equals("remove") || action.equals("del")) {
            if (!rank.permissions.remove(perm)) {
                pl.send(s, "perm-missing", "rank", rank.coloredName());
                return;
            }
            pl.send(s, "perm-removed", "rank", rank.coloredName(), "permission", perm);
        } else {
            usage(s, "/rank permission <rank> <add|remove|list> [permission]");
            return;
        }
        pl.data().saveRanks();
        pl.applier().applyAll();
    }

    private void inherit(CommandSender s, String[] r) {
        if (r.length < 3) {
            usage(s, "/rank inherit <rank> <add|remove> <other-rank>");
            return;
        }
        Rank rank = rankOrError(s, r[0]);
        if (rank == null) {
            return;
        }
        Rank other = rankOrError(s, r[2]);
        if (other == null) {
            return;
        }
        String action = r[1].toLowerCase();
        if (action.equals("add")) {
            if (other == rank || rank.inherits.contains(other.id) || pl.data().inheritsFrom(other, rank)) {
                pl.send(s, "inherit-invalid");
                return;
            }
            rank.inherits.add(other.id);
            pl.send(s, "inherit-added", "rank", rank.coloredName(), "other", other.coloredName());
        } else if (action.equals("remove") || action.equals("del")) {
            if (!rank.inherits.remove(other.id)) {
                pl.send(s, "inherit-missing", "rank", rank.coloredName(), "other", other.coloredName());
                return;
            }
            pl.send(s, "inherit-removed", "rank", rank.coloredName(), "other", other.coloredName());
        } else {
            usage(s, "/rank inherit <rank> <add|remove> <other-rank>");
            return;
        }
        pl.data().saveRanks();
        pl.applier().applyAll();
    }

    private void makeDefault(CommandSender s, String[] r) {
        if (r.length < 1) {
            usage(s, "/rank default <rank>");
            return;
        }
        Rank rank = rankOrError(s, r[0]);
        if (rank == null) {
            return;
        }
        pl.data().setDefault(rank);
        pl.applier().applyAll();
        pl.send(s, "default-set", "rank", rank.coloredName());
    }

    private void move(CommandSender s, String[] r) {
        if (r.length < 2) {
            usage(s, "/rank move <rank> <up|down>");
            return;
        }
        Rank rank = rankOrError(s, r[0]);
        if (rank == null) {
            return;
        }
        String d = r[1].toLowerCase();
        boolean up = d.equals("up");
        boolean down = d.equals("down");
        if (!up && !down) {
            usage(s, "/rank move <rank> <up|down>");
            return;
        }
        if (!pl.data().move(rank.id, up)) {
            pl.send(s, "move-limit", "rank", rank.coloredName());
            return;
        }
        pl.applier().applyAll();
        pl.send(s, "move-ok", "rank", rank.coloredName());
    }

    private static String joinList(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String x : l) {
            if (sb.length() > 0) {
                sb.append("&7, &f");
            }
            sb.append(x);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ tab

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        List<String> none = new ArrayList<String>();
        boolean admin = s.hasPermission("branks.admin");
        if (c.getName().equalsIgnoreCase("setrank")) {
            if (!admin) {
                return none;
            }
            if (a.length == 1) {
                return Util.filter(pl.onlineNames(), a[0]);
            }
            if (a.length == 2) {
                return Util.filter(rankIds(true), a[1]);
            }
            return none;
        }
        if (a.length == 1) {
            List<String> subs = new ArrayList<String>(PUBLIC_SUBS);
            if (admin) {
                subs.addAll(ADMIN_SUBS);
            }
            return Util.filter(subs, a[0]);
        }
        String sub = sub(a[0]);
        if (sub.equals("info") && a.length == 2) {
            return Util.filter(pl.onlineNames(), a[1]);
        }
        if (!admin) {
            return none;
        }
        if (sub.equals("set")) {
            if (a.length == 2) {
                return Util.filter(pl.onlineNames(), a[1]);
            }
            if (a.length == 3) {
                return Util.filter(rankIds(true), a[2]);
            }
            return none;
        }
        if (a.length == 2 && !sub.equals("create") && !sub.equals("reload") && ADMIN_SUBS.contains(sub)) {
            return Util.filter(rankIds(false), a[1]);
        }
        if (a.length == 3) {
            if (sub.equals("color") || sub.equals("chatcolor")) {
                List<String> names = new ArrayList<String>(Colors.availableNames());
                names.add("none");
                return Util.filter(names, a[2]);
            }
            if (sub.equals("permission")) {
                return Util.filter(Arrays.asList("add", "remove", "list"), a[2]);
            }
            if (sub.equals("inherit")) {
                return Util.filter(Arrays.asList("add", "remove"), a[2]);
            }
            if (sub.equals("move")) {
                return Util.filter(Arrays.asList("up", "down"), a[2]);
            }
            if (sub.equals("prefix") || sub.equals("suffix")) {
                return Util.filter(Arrays.asList("none"), a[2]);
            }
        }
        if (a.length == 4 && sub.equals("inherit")) {
            return Util.filter(rankIds(false), a[3]);
        }
        return none;
    }

    private List<String> rankIds(boolean withDefault) {
        List<String> l = new ArrayList<String>(pl.data().ranks().keySet());
        if (withDefault) {
            l.add("default");
        }
        return l;
    }
}
