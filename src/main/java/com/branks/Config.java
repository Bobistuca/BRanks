package com.branks;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** config.yml (somente leitura: o plugin nunca reescreve, entao seus comentarios ficam). */
public class Config {
    private final BRanks pl;
    private YamlConfiguration cfg = new YamlConfiguration();

    public Config(BRanks pl) {
        this.pl = pl;
    }

    public void carregar() {
        File f = new File(pl.getDataFolder(), "config.yml");
        if (!f.exists()) {
            pl.saveResource("config.yml", false);
        }
        YamlConfiguration lido = Arquivos.ler(pl, f);
        if (lido == null) {
            if (cfg.getKeys(false).isEmpty()) {
                lido = new YamlConfiguration(); // so usa os padroes embutidos
            } else {
                return;
            }
        }
        lido.setDefaults(Arquivos.lerRecurso(pl.getResource("config.yml")));
        cfg = lido;
    }

    public boolean bool(String caminho) {
        return cfg.getBoolean(caminho);
    }

    public String texto(String caminho) {
        return cfg.getString(caminho);
    }

    public List<String> lista(String caminho) {
        List<String> l = cfg.getStringList(caminho);
        return l == null ? new ArrayList<String>() : l;
    }
}
