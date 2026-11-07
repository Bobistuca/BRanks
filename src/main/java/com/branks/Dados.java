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

/** Guarda ranks, tags e jogadores e cuida dos arquivos ranks.yml, tags.yml e jogadores.yml. */
public class Dados {

    private static final String CAB_RANKS =
            "BRanks - ranks\n"
                    + "A ORDEM abaixo e a hierarquia: o primeiro rank e o mais alto.\n"
                    + "Campos: nome, prefixo, sufixo, cor (nome do rank no chat), cor-chat (cor da mensagem),\n"
                    + "padrao (true = rank dos novos jogadores), herda (outros ranks), permissoes.\n"
                    + "Permissao com - na frente remove a permissao (ex: -essentials.fly). \"*\" = todas.\n"
                    + "Use /rank recarregar depois de editar. Comentarios sao apagados quando um comando salva o arquivo.";
    private static final String CAB_TAGS =
            "BRanks - tags\n"
                    + "texto: como a tag aparece (aceita cores).\n"
                    + "permissao: quem tiver essa permissao pode usar a tag. Vazio = todos podem usar.\n"
                    + "Use /rank recarregar depois de editar.";
    private static final String CAB_JOGADORES =
            "BRanks - dados dos jogadores (gerado automaticamente)";

    private final BRanks pl;
    private final File fRanks, fTags, fJogadores;
    private volatile Map<String, Rank> ranks = new LinkedHashMap<String, Rank>();
    private volatile Map<String, Tag> tags = new LinkedHashMap<String, Tag>();
    private final Map<UUID, Jogador> jogadores = new ConcurrentHashMap<UUID, Jogador>();

    public Dados(BRanks pl) {
        this.pl = pl;
        File pasta = pl.getDataFolder();
        fRanks = new File(pasta, "ranks.yml");
        fTags = new File(pasta, "tags.yml");
        fJogadores = new File(pasta, "jogadores.yml");
    }

    // ------------------------------------------------------------------ carregar

    public void carregar() {
        carregarRanks();
        carregarTags();
        carregarJogadores();
    }

    private void carregarRanks() {
        if (!fRanks.exists()) {
            ranks = ranksPadrao();
            salvarRanks();
            return;
        }
        YamlConfiguration y = Arquivos.ler(pl, fRanks);
        if (y == null) {
            return; // arquivo com erro: mantem o que ja estava em memoria
        }
        Map<String, Rank> novo = new LinkedHashMap<String, Rank>();
        for (String chave : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(chave);
            if (s == null) {
                continue;
            }
            String id = chave.toLowerCase();
            Rank r = new Rank(id);
            r.nome = s.getString("nome", chave);
            r.prefixo = s.getString("prefixo", "");
            r.sufixo = s.getString("sufixo", "");
            r.cor = s.getString("cor", "");
            r.corChat = s.getString("cor-chat", "");
            r.padrao = s.getBoolean("padrao", false);
            for (String h : s.getStringList("herda")) {
                r.herda.add(h.toLowerCase());
            }
            r.permissoes.addAll(s.getStringList("permissoes"));
            novo.put(id, r);
        }
        ranks = novo;
    }

    private void carregarTags() {
        if (!fTags.exists()) {
            tags = tagsPadrao();
            salvarTags();
            return;
        }
        YamlConfiguration y = Arquivos.ler(pl, fTags);
        if (y == null) {
            return;
        }
        Map<String, Tag> novo = new LinkedHashMap<String, Tag>();
        for (String chave : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(chave);
            if (s == null) {
                continue;
            }
            String id = chave.toLowerCase();
            Tag t = new Tag(id, s.getString("texto", chave));
            t.permissao = s.getString("permissao", "");
            novo.put(id, t);
        }
        tags = novo;
    }

    private void carregarJogadores() {
        YamlConfiguration y = Arquivos.ler(pl, fJogadores);
        if (y == null) {
            return;
        }
        jogadores.clear();
        for (String chave : y.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(chave);
                ConfigurationSection s = y.getConfigurationSection(chave);
                if (s == null) {
                    continue;
                }
                Jogador j = new Jogador(s.getString("nome", ""));
                String r = s.getString("rank");
                String t = s.getString("tag");
                j.rank = (r == null || r.isEmpty()) ? null : r.toLowerCase();
                j.tag = (t == null || t.isEmpty()) ? null : t.toLowerCase();
                jogadores.put(uuid, j);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ salvar

    public synchronized void salvarRanks() {
        YamlConfiguration y = new YamlConfiguration();
        for (Rank r : ranks.values()) {
            String p = r.id + ".";
            y.set(p + "nome", r.nome);
            y.set(p + "prefixo", r.prefixo);
            y.set(p + "sufixo", r.sufixo);
            y.set(p + "cor", r.cor);
            y.set(p + "cor-chat", r.corChat);
            y.set(p + "padrao", r.padrao);
            y.set(p + "herda", new ArrayList<String>(r.herda));
            y.set(p + "permissoes", new ArrayList<String>(r.permissoes));
        }
        Arquivos.salvar(pl, fRanks, y, CAB_RANKS);
    }

    public synchronized void salvarTags() {
        YamlConfiguration y = new YamlConfiguration();
        for (Tag t : tags.values()) {
            y.set(t.id + ".texto", t.texto);
            y.set(t.id + ".permissao", t.permissao);
        }
        Arquivos.salvar(pl, fTags, y, CAB_TAGS);
    }

    public synchronized void salvarJogadores() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Jogador> e : jogadores.entrySet()) {
            Jogador j = e.getValue();
            if (j.rank == null && j.tag == null && (j.nome == null || j.nome.isEmpty())) {
                continue;
            }
            String p = e.getKey().toString() + ".";
            y.set(p + "nome", j.nome);
            if (j.rank != null) {
                y.set(p + "rank", j.rank);
            }
            if (j.tag != null) {
                y.set(p + "tag", j.tag);
            }
        }
        Arquivos.salvar(pl, fJogadores, y, CAB_JOGADORES);
    }

    // ------------------------------------------------------------------ padroes

    private Map<String, Rank> ranksPadrao() {
        Map<String, Rank> m = new LinkedHashMap<String, Rank>();
        add(m, "dono", "Dono", "&4[Dono]", "vermelho-escuro", "branco", false, "*");
        add(m, "admin", "Admin", "&c[Admin]", "vermelho", "branco", false,
                "branks.admin", "branks.chat.cores");
        add(m, "moderador", "Moderador", "&2[Mod]", "verde-escuro", "", false,
                "branks.chat.cores");
        add(m, "vip", "VIP", "&6[VIP]", "dourado", "", false, "branks.tag.vip");
        add(m, "membro", "Membro", "&7[Membro]", "cinza", "", true);
        return m;
    }

    private void add(Map<String, Rank> m, String id, String nome, String prefixo, String cor,
                     String corChat, boolean padrao, String... perms) {
        Rank r = new Rank(id);
        r.nome = nome;
        r.prefixo = prefixo;
        r.cor = cor;
        r.corChat = corChat;
        r.padrao = padrao;
        for (String p : perms) {
            r.permissoes.add(p);
        }
        m.put(id, r);
    }

    private Map<String, Tag> tagsPadrao() {
        Map<String, Tag> m = new LinkedHashMap<String, Tag>();
        Tag vip = new Tag("vip", "&6[VIP]");
        vip.permissao = "branks.tag.vip";
        Tag lenda = new Tag("lenda", "&d[Lenda]");
        lenda.permissao = "branks.tag.lenda";
        Tag gamer = new Tag("gamer", "&a[Gamer]");
        m.put(vip.id, vip);
        m.put(lenda.id, lenda);
        m.put(gamer.id, gamer);
        return m;
    }

    // ------------------------------------------------------------------ ranks

    public Map<String, Rank> ranks() {
        return ranks;
    }

    public Rank rank(String id) {
        return id == null ? null : ranks.get(id.toLowerCase());
    }

    public Rank rankPadrao() {
        for (Rank r : ranks.values()) {
            if (r.padrao) {
                return r;
            }
        }
        return null;
    }

    /** Indice do rank na hierarquia (0 = mais alto). */
    public int indice(Rank r) {
        int i = 0;
        for (String id : ranks.keySet()) {
            if (id.equals(r.id)) {
                return i;
            }
            i++;
        }
        return i;
    }

    public Rank rankDe(UUID uuid) {
        Jogador j = jogadores.get(uuid);
        if (j != null && j.rank != null) {
            Rank r = ranks.get(j.rank);
            if (r != null) {
                return r;
            }
        }
        return rankPadrao();
    }

    public Rank criarRank(String id, String nome) {
        Rank r = new Rank(id);
        r.nome = nome;
        r.prefixo = "&7[" + nome + "]";
        r.cor = "cinza";
        Map<String, Rank> copia = new LinkedHashMap<String, Rank>(ranks);
        copia.put(id, r);
        ranks = copia;
        salvarRanks();
        return r;
    }

    public void apagarRank(String id) {
        Map<String, Rank> copia = new LinkedHashMap<String, Rank>(ranks);
        copia.remove(id);
        ranks = copia;
        for (Rank r : copia.values()) {
            r.herda.remove(id);
        }
        boolean mudouJog = false;
        for (Jogador j : jogadores.values()) {
            if (id.equals(j.rank)) {
                j.rank = null;
                mudouJog = true;
            }
        }
        salvarRanks();
        if (mudouJog) {
            salvarJogadores();
        }
    }

    public void definirPadrao(Rank novo) {
        for (Rank r : ranks.values()) {
            r.padrao = r == novo;
        }
        salvarRanks();
    }

    /** Move o rank na hierarquia. Retorna false se ja estiver no limite. */
    public boolean mover(String id, boolean paraCima) {
        List<String> ids = new ArrayList<String>(ranks.keySet());
        int i = ids.indexOf(id);
        int j = paraCima ? i - 1 : i + 1;
        if (i < 0 || j < 0 || j >= ids.size()) {
            return false;
        }
        String tmp = ids.get(i);
        ids.set(i, ids.get(j));
        ids.set(j, tmp);
        Map<String, Rank> novo = new LinkedHashMap<String, Rank>();
        for (String k : ids) {
            novo.put(k, ranks.get(k));
        }
        ranks = novo;
        salvarRanks();
        return true;
    }

    /** Permissoes finais de um rank (incluindo as herdadas). Valor false = negada. */
    public Map<String, Boolean> permissoes(Rank r) {
        Map<String, Boolean> out = new LinkedHashMap<String, Boolean>();
        coletar(r, out, new HashSet<String>());
        return out;
    }

    private void coletar(Rank r, Map<String, Boolean> out, Set<String> vistos) {
        if (!vistos.add(r.id)) {
            return;
        }
        for (String h : r.herda) {
            Rank hr = ranks.get(h);
            if (hr != null) {
                coletar(hr, out, vistos);
            }
        }
        for (String p : r.permissoes) {
            String n = p.trim();
            boolean valor = true;
            if (n.startsWith("-") || n.startsWith("!")) {
                valor = false;
                n = n.substring(1).trim();
            }
            if (!n.isEmpty()) {
                out.put(n.toLowerCase(), valor);
            }
        }
    }

    /** Verdadeiro se "alvo" ja esta na cadeia de heranca de "base" (evita ciclos). */
    public boolean herdaDe(Rank base, Rank alvo) {
        return herdaDe(base, alvo, new HashSet<String>());
    }

    private boolean herdaDe(Rank base, Rank alvo, Set<String> vistos) {
        if (!vistos.add(base.id)) {
            return false;
        }
        for (String h : base.herda) {
            if (h.equals(alvo.id)) {
                return true;
            }
            Rank hr = ranks.get(h);
            if (hr != null && herdaDe(hr, alvo, vistos)) {
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

    public Tag criarTag(String id, String texto) {
        Tag t = new Tag(id, texto);
        Map<String, Tag> copia = new LinkedHashMap<String, Tag>(tags);
        copia.put(id, t);
        tags = copia;
        salvarTags();
        return t;
    }

    public void apagarTag(String id) {
        Map<String, Tag> copia = new LinkedHashMap<String, Tag>(tags);
        copia.remove(id);
        tags = copia;
        boolean mudou = false;
        for (Jogador j : jogadores.values()) {
            if (id.equals(j.tag)) {
                j.tag = null;
                mudou = true;
            }
        }
        salvarTags();
        if (mudou) {
            salvarJogadores();
        }
    }

    public boolean permitida(Player p, Tag t) {
        return t.permissao.isEmpty() || p.hasPermission(t.permissao);
    }

    /** Tag que o jogador esta realmente exibindo (null se nao tem ou perdeu a permissao). */
    public Tag tagAtiva(Player p) {
        Jogador j = jogadores.get(p.getUniqueId());
        if (j == null || j.tag == null) {
            return null;
        }
        Tag t = tags.get(j.tag);
        if (t == null || !permitida(p, t)) {
            return null;
        }
        return t;
    }

    public String tagEscolhida(UUID uuid) {
        Jogador j = jogadores.get(uuid);
        return j == null ? null : j.tag;
    }

    // ------------------------------------------------------------------ jogadores

    public Map<UUID, Jogador> jogadores() {
        return jogadores;
    }

    private Jogador entrada(UUID uuid, String nome) {
        Jogador j = jogadores.get(uuid);
        if (j == null) {
            j = new Jogador(nome);
            jogadores.put(uuid, j);
        } else if (nome != null) {
            j.nome = nome;
        }
        return j;
    }

    public void registrarEntrada(Player p) {
        Jogador j = jogadores.get(p.getUniqueId());
        if (j == null || !p.getName().equals(j.nome)) {
            entrada(p.getUniqueId(), p.getName());
            salvarJogadores();
        }
    }

    /** rankId null = volta ao padrao. */
    public void definirRank(UUID uuid, String nome, String rankId) {
        entrada(uuid, nome).rank = rankId;
        salvarJogadores();
    }

    public void definirTag(UUID uuid, String nome, String tagId) {
        entrada(uuid, nome).tag = tagId;
        salvarJogadores();
    }
}
