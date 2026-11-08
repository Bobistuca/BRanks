package com.branks;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BRanks extends JavaPlugin {
    private Config config;
    private DataStore data;
    private Applier applier;

    @Override
    public void onEnable() {
        config = new Config(this);
        config.load();
        data = new DataStore(this);
        data.load();
        applier = new Applier(this);

        RankCommand rank = new RankCommand(this);
        TagCommand tag = new TagCommand(this);
        register("rank", rank, rank);
        register("setrank", rank, rank);
        register("tag", tag, tag);
        register("settag", tag, tag);

        Bukkit.getPluginManager().registerEvents(new Events(this), this);

        applier.clearTeams();
        applier.applyAll();
        getLogger().info("BRanks enabled: " + data.ranks().size() + " ranks, " + data.tags().size() + " tags.");
    }

    @Override
    public void onDisable() {
        if (applier != null) {
            applier.removeAll();
        }
        if (data != null) {
            data.savePlayers();
        }
    }

    private void register(String name, CommandExecutor ex, TabCompleter tc) {
        PluginCommand c = getCommand(name);
        if (c != null) {
            c.setExecutor(ex);
            c.setTabCompleter(tc);
        }
    }

    public void reload() {
        applier.removeAll();
        config.load();
        data.load();
        applier.applyAll();
    }

    public Config cfg() {
        return config;
    }

    public DataStore data() {
        return data;
    }

    public Applier applier() {
        return applier;
    }

    // ------------------------------------------------------------------ messages

    /** Message from config.yml with variables: msg("key", "player", "Steve", "rank", "VIP"). */
    public String msg(String key, String... pairs) {
        String t = config.text("messages." + key);
        if (t == null) {
            t = "&c[missing message: " + key + "]";
        }
        return applyVars(t, pairs);
    }

    public static String applyVars(String t, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            t = t.replace("{" + pairs[i] + "}", pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return Colors.translate(t);
    }

    public void send(CommandSender s, String key, String... pairs) {
        s.sendMessage(msg(key, pairs));
    }

    public void sendList(CommandSender s, String key, String... pairs) {
        for (String line : config.list("messages." + key)) {
            s.sendMessage(applyVars(line, pairs));
        }
    }

    public List<String> onlineNames() {
        List<String> l = new ArrayList<String>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            l.add(p.getName());
        }
        return l;
    }
}
