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

/** YAML reading/writing, always in UTF-8 (accents work on every version). */
public final class FileUtil {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    private FileUtil() {
    }

    /** Reads a YAML file. Returns null if the file is broken (a .error copy is kept). */
    public static YamlConfiguration read(BRanks pl, File f) {
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
            pl.getLogger().log(Level.SEVERE, "Could not read " + f.getName(), e);
        } catch (InvalidConfigurationException e) {
            pl.getLogger().severe("File " + f.getName() + " has a formatting error: " + e.getMessage());
            pl.getLogger().severe("A copy was saved as " + f.getName() + ".error. Fix the file and run /rank reload.");
            try {
                Files.copy(f.toPath(), new File(f.getParentFile(), f.getName() + ".error").toPath(),
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

    public static YamlConfiguration readResource(InputStream in) {
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

    /** Saves the YAML with an explanatory header (old comments are discarded). */
    public static void save(BRanks pl, File f, YamlConfiguration y, String header) {
        StringBuilder sb = new StringBuilder();
        for (String l : header.split("\n")) {
            sb.append("# ").append(l).append('\n');
        }
        sb.append('\n');
        for (String line : y.saveToString().split("\n")) {
            if (line.trim().startsWith("#")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        OutputStream out = null;
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            File tmp = new File(parent, f.getName() + ".tmp");
            out = new FileOutputStream(tmp);
            out.write(sb.toString().getBytes(UTF8));
            out.close();
            out = null;
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            pl.getLogger().log(Level.SEVERE, "Could not save " + f.getName(), e);
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
