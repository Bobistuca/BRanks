package com.branks;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** config.yml (read only: the plugin never rewrites it, so your comments are kept). */
public class Config {
    private final BRanks pl;
    private YamlConfiguration cfg = new YamlConfiguration();

    public Config(BRanks pl) {
        this.pl = pl;
    }

    public void load() {
        File f = new File(pl.getDataFolder(), "config.yml");
        if (!f.exists()) {
            pl.saveResource("config.yml", false);
        }
        YamlConfiguration read = FileUtil.read(pl, f);
        if (read == null) {
            if (cfg.getKeys(false).isEmpty()) {
                read = new YamlConfiguration(); // fall back to the built-in defaults only
            } else {
                return;
            }
        }
        read.setDefaults(FileUtil.readResource(pl.getResource("config.yml")));
        cfg = read;
    }

    public boolean bool(String path) {
        return cfg.getBoolean(path);
    }

    public String text(String path) {
        return cfg.getString(path);
    }

    public List<String> list(String path) {
        List<String> l = cfg.getStringList(path);
        return l == null ? new ArrayList<String>() : l;
    }
}
