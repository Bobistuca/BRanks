package com.branks;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Holds ranks, tags and players, and manages ranks.yml, tags.yml and players.yml. */
public class DataStore {

    private static final String HEADER_RANKS =
            "BRanks - ranks\n"
                    + "level (1 = lowest, 2556 = highest) is the hierarchy. Ranks are listed from highest to lowest.\n"
                    + "Fields: level, name, prefix, suffix, color (player name color), chat-color (message color),\n"
                    + "default (true = rank of new players), inherits (other ranks), permissions.\n"
                    + "A permission starting with - removes that permission (e.g. -essentials.fly). \"*\" = all.\n"
                    + "Run /rank reload after editing. Comments are erased whenever a command saves the file.\n"
                    + "Example entry:\n"
                    + "  vip:\n"
                    + "    level: 100\n"
                    + "    name: VIP\n"
                    + "    prefix: \"&6[VIP]\"\n"
                    + "    color: gold\n"
                    + "    inherits: [member]\n"
                    + "    permissions: [some.permission]";
    private static final String HEADER_TAGS =
            "BRanks - tags\n"
                    + "text: how the tag looks (supports colors).\n"
                    + "permission: players with this permission can use the tag.\n"
                    + "rank: players with this rank OR ANY HIGHER RANK can use the tag.\n"
                    + "A tag is unlocked by the permission OR by the rank. With neither set, everyone can use it.\n"
                    + "Nothing is preset: create your own with /tag create <name> <text>. Example entry:\n"
                    + "  vip:\n"
                    + "    text: \"&6[VIP]\"\n"
                    + "    rank: vip\n"
                    + "Run /rank reload after editing.";
    private static final String HEADER_PLAYERS =
            "BRanks - player data (generated automatically)";

    private final BRanks pl;
    private final File fRanks, fTags, fPlayers;
    private volatile Map<String, Rank> ranks = new LinkedHashMap<String, Rank>();
    private volatile Map<String, Tag> tags = new LinkedHashMap<String, Tag>();
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<UUID, PlayerData>();

    public DataStore(BRanks pl) {
        this.pl = pl;
        File folder = pl.getDataFolder();
        fRanks = new File(folder, "ranks.yml");
        fTags = new File(folder, "tags.yml");
        fPlayers = new File(folder, "players.yml");
    }

    // ------------------------------------------------------------------ load

    public void load() {
        loadRanks();
        loadTags();
        loadPlayers();
    }

    private void loadRanks() {
        if (!fRanks.exists()) {
            ranks = defaultRanks(); // first run only: edit, delete or add ranks freely
            saveRanks();
            return;
        }
        YamlConfiguration y = FileUtil.read(pl, fRanks);
        if (y == null) {
            return; // broken file: keep what is already in memory
        }
        Map<String, Rank> loaded = new LinkedHashMap<String, Rank>();
        for (String key : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            String id = key.toLowerCase();
            Rank r = new Rank(id);
            r.level = clampLevel(s.getInt("level", Rank.MIN_LEVEL));
            r.name = s.getString("name", key);
            r.prefix = s.getString("prefix", "");
            r.suffix = s.getString("suffix", "");
            r.color = s.getString("color", "");
            r.chatColor = s.getString("chat-color", "");
            r.isDefault = s.getBoolean("default", false);
            for (String h : s.getStringList("inherits")) {
                r.inherits.add(h.toLowerCase());
            }
            r.permissions.addAll(s.getStringList("permissions"));
            loaded.put(id, r);
        }
        ranks = sorted(loaded);
    }

    private void loadTags() {
        if (!fTags.exists()) {
            tags = new LinkedHashMap<String, Tag>(); // starts empty: you create your own tags
            saveTags();
            return;
        }
        YamlConfiguration y = FileUtil.read(pl, fTags);
        if (y == null) {
            return;
        }
        Map<String, Tag> loaded = new LinkedHashMap<String, Tag>();
        for (String key : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            String id = key.toLowerCase();
            Tag t = new Tag(id, s.getString("text", key));
            t.permission = s.getString("permission", "");
            t.rank = s.getString("rank", "").toLowerCase();
            loaded.put(id, t);
        }
        tags = loaded;
    }

    private void loadPlayers() {
        YamlConfiguration y = FileUtil.read(pl, fPlayers);
        if (y == null) {
            return;
        }
        players.clear();
        for (String key : y.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection s = y.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                PlayerData d = new PlayerData(s.getString("name", ""));
                String r = s.getString("rank");
                String t = s.getString("tag");
                d.rank = (r == null || r.isEmpty()) ? null : r.toLowerCase();
                d.tag = (t == null || t.isEmpty()) ? null : t.toLowerCase();
                players.put(uuid, d);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ save

    public synchronized void saveRanks() {
        YamlConfiguration y = new YamlConfiguration();
        for (Rank r : ranks.values()) {
            String p = r.id + ".";
            y.set(p + "level", r.level);
            y.set(p + "name", r.name);
            y.set(p + "prefix", r.prefix);
            y.set(p + "suffix", r.suffix);
            y.set(p + "color", r.color);
            y.set(p + "chat-color", r.chatColor);
            y.set(p + "default", r.isDefault);
            y.set(p + "inherits", new ArrayList<String>(r.inherits));
            y.set(p + "permissions", new ArrayList<String>(r.permissions));
        }
        FileUtil.save(pl, fRanks, y, HEADER_RANKS);
    }

    public synchronized void saveTags() {
        YamlConfiguration y = new YamlConfiguration();
        for (Tag t : tags.values()) {
            y.set(t.id + ".text", t.text);
            y.set(t.id + ".permission", t.permission);
            y.set(t.id + ".rank", t.rank);
        }
        FileUtil.save(pl, fTags, y, HEADER_TAGS);
    }

    public synchronized void savePlayers() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> e : players.entrySet()) {
            PlayerData d = e.getValue();
            if (d.rank == null && d.tag == null && (d.name == null || d.name.isEmpty())) {
                continue;
            }
            String p = e.getKey().toString() + ".";
            y.set(p + "name", d.name);
            if (d.rank != null) {
                y.set(p + "rank", d.rank);
            }
            if (d.tag != null) {
                y.set(p + "tag", d.tag);
            }
        }
        FileUtil.save(pl, fPlayers, y, HEADER_PLAYERS);
    }

    // ------------------------------------------------------------------ ranks

    private static int clampLevel(int level) {
        return Math.max(Rank.MIN_LEVEL, Math.min(Rank.MAX_LEVEL, level));
    }

    /** Orders ranks from the highest level to the lowest (ties keep their current order). */
    private static Map<String, Rank> sorted(Map<String, Rank> in) {
        List<Rank> list = new ArrayList<Rank>(in.values());
        java.util.Collections.sort(list, new java.util.Comparator<Rank>() {
            @Override
            public int compare(Rank a, Rank b) {
                return Integer.compare(b.level, a.level);
            }
        });
        Map<String, Rank> out = new LinkedHashMap<String, Rank>();
        for (Rank r : list) {
            out.put(r.id, r);
        }
        return out;
    }

    private Map<String, Rank> defaultRanks() {
        Map<String, Rank> m = new LinkedHashMap<String, Rank>();
        addRank(m, "member", "MEMBER", 1, "&7[MEMBER]", "gray", true, null);
        addRank(m, "vip", "VIP", 100, "&a[VIP]", "green", false, "member");
        addRank(m, "helper", "HELPER", 800, "&#3E5A8A[HELPER]", "#3E5A8A", false, "vip");
        addRank(m, "mod", "MOD", 1200, "&d[MOD]", "light-purple", false, "helper");
        addRank(m, "modplus", "MOD+", 1600, "&5[MOD+]", "dark-purple", false, "mod");
        addRank(m, "admin", "ADMIN", 2000, "&c[ADMIN]", "red", false, "modplus", "branks.admin", "branks.chat.colors");
        addRank(m, "owner", "OWNER", Rank.MAX_LEVEL, "&4[OWNER]", "dark-red", false, "admin", "*");
        return sorted(m);
    }

    private void addRank(Map<String, Rank> m, String id, String name, int level, String prefix,
                         String color, boolean isDefault, String inherits, String... perms) {
        Rank r = new Rank(id);
        r.name = name;
        r.level = level;
        r.prefix = prefix;
        r.color = color;
        r.isDefault = isDefault;
        if (inherits != null) {
            r.inherits.add(inherits);
        }
        for (String p : perms) {
            r.permissions.add(p);
        }
        m.put(id, r);
    }

    public Map<String, Rank> ranks() {
        return ranks;
    }

    public Rank rank(String id) {
        return id == null ? null : ranks.get(id.toLowerCase());
    }

    public Rank defaultRank() {
        for (Rank r : ranks.values()) {
            if (r.isDefault) {
                return r;
            }
        }
        return null;
    }

    /** Position of the rank in the hierarchy (0 = highest). */
    public int indexOf(Rank r) {
        int i = 0;
        for (String id : ranks.keySet()) {
            if (id.equals(r.id)) {
                return i;
            }
            i++;
        }
        return i;
    }

    public Rank rankOf(UUID uuid) {
        PlayerData d = players.get(uuid);
        if (d != null && d.rank != null) {
            Rank r = ranks.get(d.rank);
            if (r != null) {
                return r;
            }
        }
        return defaultRank();
    }

    public Rank createRank(String id, String name, int level) {
        Rank r = new Rank(id);
        r.name = name;
        r.level = clampLevel(level);
        r.prefix = "&7[" + name + "]";
        r.color = "gray";
        Map<String, Rank> copy = new LinkedHashMap<String, Rank>(ranks);
        copy.put(id, r);
        ranks = sorted(copy);
        saveRanks();
        return r;
    }

    public void deleteRank(String id) {
        Map<String, Rank> copy = new LinkedHashMap<String, Rank>(ranks);
        copy.remove(id);
        ranks = copy;
        for (Rank r : copy.values()) {
            r.inherits.remove(id);
        }
        boolean playersChanged = false;
        for (PlayerData d : players.values()) {
            if (id.equals(d.rank)) {
                d.rank = null;
                playersChanged = true;
            }
        }
        saveRanks();
        if (playersChanged) {
            savePlayers();
        }
    }

    public void setDefault(Rank chosen) {
        for (Rank r : ranks.values()) {
            r.isDefault = r == chosen;
        }
        saveRanks();
    }

    /** Changes the level of a rank and re-sorts the hierarchy. */
    public void setLevel(Rank r, int level) {
        r.level = clampLevel(level);
        ranks = sorted(ranks);
        saveRanks();
    }

    /** Final permissions of a rank (including inherited ones). A false value = denied. */
    public Map<String, Boolean> permissions(Rank r) {
        Map<String, Boolean> out = new LinkedHashMap<String, Boolean>();
        collect(r, out, new HashSet<String>());
        return out;
    }

    private void collect(Rank r, Map<String, Boolean> out, Set<String> seen) {
        if (!seen.add(r.id)) {
            return;
        }
        for (String h : r.inherits) {
            Rank hr = ranks.get(h);
            if (hr != null) {
                collect(hr, out, seen);
            }
        }
        for (String p : r.permissions) {
            String n = p.trim();
            boolean value = true;
            if (n.startsWith("-") || n.startsWith("!")) {
                value = false;
                n = n.substring(1).trim();
            }
            if (!n.isEmpty()) {
                out.put(n.toLowerCase(), value);
            }
        }
    }

    /** True if "target" is already in the inheritance chain of "base" (prevents cycles). */
    public boolean inheritsFrom(Rank base, Rank target) {
        return inheritsFrom(base, target, new HashSet<String>());
    }

    private boolean inheritsFrom(Rank base, Rank target, Set<String> seen) {
        if (!seen.add(base.id)) {
            return false;
        }
        for (String h : base.inherits) {
            if (h.equals(target.id)) {
                return true;
            }
            Rank hr = ranks.get(h);
            if (hr != null && inheritsFrom(hr, target, seen)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ tags

    public Map<String, Tag> tags() {
        return tags;
    }

    public Tag tag(String id) {
        return id == null ? null : tags.get(id.toLowerCase());
    }

    public Tag createTag(String id, String text) {
        Tag t = new Tag(id, text);
        Map<String, Tag> copy = new LinkedHashMap<String, Tag>(tags);
        copy.put(id, t);
        tags = copy;
        saveTags();
        return t;
    }

    public void deleteTag(String id) {
        Map<String, Tag> copy = new LinkedHashMap<String, Tag>(tags);
        copy.remove(id);
        tags = copy;
        boolean changed = false;
        for (PlayerData d : players.values()) {
            if (id.equals(d.tag)) {
                d.tag = null;
                changed = true;
            }
        }
        saveTags();
        if (changed) {
            savePlayers();
        }
    }

    /**
     * A tag is unlocked by its permission OR by its rank requirement. The rank requirement
     * counts the required rank and every rank with an equal or higher level, so a player with a
     * higher rank (e.g. HELPER) can also pick the tags of the lower ones (e.g. VIP).
     */
    public boolean canUse(Player p, Tag t) {
        if (t.isFree()) {
            return true;
        }
        if (!t.permission.isEmpty() && p.hasPermission(t.permission)) {
            return true;
        }
        if (!t.rank.isEmpty()) {
            Rank required = ranks.get(t.rank);
            Rank mine = rankOf(p.getUniqueId());
            if (required != null && mine != null && mine.level >= required.level) {
                return true;
            }
        }
        return false;
    }

    /** The tag the player is really showing (null if none, or if they lost access to it). */
    public Tag activeTag(Player p) {
        PlayerData d = players.get(p.getUniqueId());
        if (d == null || d.tag == null) {
            return null;
        }
        Tag t = tags.get(d.tag);
        if (t == null || !canUse(p, t)) {
            return null;
        }
        return t;
    }

    public String chosenTag(UUID uuid) {
        PlayerData d = players.get(uuid);
        return d == null ? null : d.tag;
    }

    // ------------------------------------------------------------------ players

    public Map<UUID, PlayerData> players() {
        return players;
    }

    private PlayerData entry(UUID uuid, String name) {
        PlayerData d = players.get(uuid);
        if (d == null) {
            d = new PlayerData(name);
            players.put(uuid, d);
        } else if (name != null) {
            d.name = name;
        }
        return d;
    }

    public void registerJoin(Player p) {
        PlayerData d = players.get(p.getUniqueId());
        if (d == null || !p.getName().equals(d.name)) {
            entry(p.getUniqueId(), p.getName());
            savePlayers();
        }
    }

    /** rankId null = back to the default rank. */
    public void setRank(UUID uuid, String name, String rankId) {
        entry(uuid, name).rank = rankId;
        savePlayers();
    }

    public void setTag(UUID uuid, String name, String tagId) {
        entry(uuid, name).tag = tagId;
        savePlayers();
    }
}
