package com.branks;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

/** Leitura/escrita de YAML sempre em UTF-8 (acentos funcionam em qualquer versao). */
public final class Arquivos {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    private Arquivos() {
    }

    /** Le um YAML. Retorna null se o arquivo estiver com erro (e guarda uma copia .erro). */
    public static YamlConfiguration ler(BRanks pl, File f) {
        YamlConfiguration y = new YamlConfiguration();
        if (!f.exists()) {
            return y;
        }
        Reader r = null;
        try {
            r = new InputStreamReader(new FileInputStream(f), UTF8);
            y.load(r);
            return y;
        } catch (IOException e) {
            pl.getLogger().log(Level.SEVERE, "Nao foi possivel ler " + f.getName(), e);
        } catch (InvalidConfigurationException e) {
            pl.getLogger().severe("O arquivo " + f.getName() + " tem erro de formatacao: " + e.getMessage());
            pl.getLogger().severe("Uma copia foi salva em " + f.getName() + ".erro. Corrija o arquivo e use /rank recarregar.");
            try {
                Files.copy(f.toPath(), new File(f.getParentFile(), f.getName() + ".erro").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public static YamlConfiguration lerRecurso(InputStream in) {
        YamlConfiguration y = new YamlConfiguration();
        if (in == null) {
            return y;
        }
        try {
            Reader r = new InputStreamReader(in, UTF8);
            y.load(r);
            r.close();
        } catch (Exception ignored) {
        }
        return y;
    }

    /** Salva o YAML com um cabecalho explicativo (comentarios antigos sao descartados). */
    public static void salvar(BRanks pl, File f, YamlConfiguration y, String cabecalho) {
        StringBuilder sb = new StringBuilder();
        for (String l : cabecalho.split("\n")) {
            sb.append("# ").append(l).append('\n');
        }
        sb.append('\n');
        for (String linha : y.saveToString().split("\n")) {
            if (linha.trim().startsWith("#")) {
                continue;
            }
            sb.append(linha).append('\n');
        }
        OutputStream out = null;
        try {
            File pai = f.getParentFile();
            if (pai != null && !pai.exists()) {
                pai.mkdirs();
            }
            File tmp = new File(pai, f.getName() + ".tmp");
            out = new FileOutputStream(tmp);
            out.write(sb.toString().getBytes(UTF8));
            out.close();
            out = null;
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            pl.getLogger().log(Level.SEVERE, "Nao foi possivel salvar " + f.getName(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
