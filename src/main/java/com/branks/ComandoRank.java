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

public class ComandoRank implements CommandExecutor, TabCompleter {
    private final BRanks pl;

    private static final List<String> SUBS_ADMIN = Arrays.asList("definir", "criar", "apagar", "nome",
            "prefixo", "sufixo", "cor", "corchat", "permissao", "herda", "padrao", "mover", "recarregar");

    public ComandoRank(BRanks pl) {
        this.pl = pl;
    }

    private static String sub(String s) {
        s = s.toLowerCase();
        switch (s) {
            case "list": case "listar": return "lista";
            case "set": case "setar": case "setrank": return "definir";
            case "create": case "novo": case "criar": return "criar";
            case "delete": case "deletar": case "excluir": case "remove": return "apagar";
            case "name": return "nome";
            case "prefix": return "prefixo";
            case "suffix": return "sufixo";
            case "color": return "cor";
            case "chatcolor": case "cor-chat": return "corchat";
            case "perm": case "permissoes": case "permission": case "perms": return "permissao";
            case "inherit": case "herdar": case "heranca": return "herda";
            case "default": return "padrao";
            case "move": return "mover";
            case "reload": case "rl": return "recarregar";
            default: return s;
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("setrank")) {
            String[] n = new String[a.length + 1];
            n[0] = "definir";
            System.arraycopy(a, 0, n, 1, a.length);
            a = n;
        }
        if (a.length == 0) {
            ajuda(s);
            return true;
        }
        String sub = sub(a[0]);
        String[] r = Util.resto(a, 1);

        if (sub.equals("lista")) {
            lista(s);
            return true;
        }
        if (sub.equals("info")) {
            info(s, r);
            return true;
        }
        if (!SUBS_ADMIN.contains(sub)) {
            ajuda(s);
            return true;
        }
        if (!s.hasPermission("branks.admin")) {
            pl.enviar(s, "sem-permissao");
            return true;
        }
        switch (sub) {
            case "definir": definir(s, r); break;
            case "criar": criar(s, r); break;
            case "apagar": apagar(s, r); break;
            case "nome": texto(s, r, "nome"); break;
            case "prefixo": texto(s, r, "prefixo"); break;
            case "sufixo": texto(s, r, "sufixo"); break;
            case "cor": cor(s, r, false); break;
            case "corchat": cor(s, r, true); break;
            case "permissao": permissao(s, r); break;
            case "herda": herda(s, r); break;
            case "padrao": padrao(s, r); break;
            case "mover": mover(s, r); break;
            case "recarregar":
                pl.recarregar();
                pl.enviar(s, "recarregado");
                break;
            default: ajuda(s);
        }
        return true;
    }

    private void ajuda(CommandSender s) {
        pl.enviarLista(s, "ajuda-rank");
        if (s.hasPermission("branks.admin")) {
            pl.enviarLista(s, "ajuda-rank-admin");
        }
    }

    private void uso(CommandSender s, String uso) {
        pl.enviar(s, "uso", "uso", uso);
    }

    private Rank rankOuErro(CommandSender s, String id) {
        Rank r = pl.dados().rank(id);
        if (r == null) {
            pl.enviar(s, "rank-nao-existe", "rank", id);
        }
        return r;
    }

    // ------------------------------------------------------------------ publicos

    private void lista(CommandSender s) {
        Map<String, Rank> ranks = pl.dados().ranks();
        if (ranks.isEmpty()) {
            pl.enviar(s, "rank-lista-vazia");
            return;
        }
        pl.enviar(s, "rank-lista-titulo");
        for (Rank r : ranks.values()) {
            pl.enviar(s, "rank-lista-linha",
                    "id", r.id,
                    "prefixo", r.prefixo.isEmpty() ? "" : Aplicador.comEspaco(r.prefixo),
                    "cor", Cores.codigo(r.cor),
                    "nome", r.nome,
                    "padrao", r.padrao ? pl.msg("rank-lista-padrao") : "");
        }
    }

    private void info(CommandSender s, String[] r) {
        Util.Alvo alvo;
        if (r.length == 0) {
            if (!(s instanceof Player)) {
                uso(s, "/rank info <jogador>");
                return;
            }
            Player p = (Player) s;
            alvo = new Util.Alvo(p.getUniqueId(), p.getName(), p);
        } else {
            alvo = Util.resolver(pl, r[0]);
            if (alvo == null) {
                pl.enviar(s, "jogador-nao-encontrado", "jogador", r[0]);
                return;
            }
        }
        Rank rank = pl.dados().rankDe(alvo.uuid);
        Tag tag = pl.dados().tag(pl.dados().tagEscolhida(alvo.uuid));
        pl.enviarLista(s, "info-linhas",
                "jogador", alvo.nome,
                "rank", rank == null ? pl.msg("info-sem-rank") : rank.nomeColorido(),
                "tag", tag == null ? pl.msg("tag-nenhuma") : tag.textoColorido(),
                "quantidade", String.valueOf(rank == null ? 0 : pl.dados().permissoes(rank).size()));
    }

    // ------------------------------------------------------------------ admin

    private void definir(CommandSender s, String[] r) {
        if (r.length < 2) {
            uso(s, "/rank definir <jogador> <rank>");
            return;
        }
        Util.Alvo alvo = Util.resolver(pl, r[0]);
        if (alvo == null) {
            pl.enviar(s, "jogador-nao-encontrado", "jogador", r[0]);
            return;
        }
        Rank rank = pl.dados().rank(r[1]);
        if (rank == null) {
            String t = r[1].toLowerCase();
            if (t.equals("padrao") || t.equals("nenhum") || t.equals("remover") || t.equals("resetar")) {
                pl.dados().definirRank(alvo.uuid, alvo.nome, null);
                if (alvo.online != null) {
                    pl.aplicador().aplicar(alvo.online);
                }
                pl.enviar(s, "rank-limpo", "jogador", alvo.nome);
                return;
            }
            pl.enviar(s, "rank-nao-existe", "rank", r[1]);
            return;
        }
        pl.dados().definirRank(alvo.uuid, alvo.nome, rank.id);
        if (alvo.online != null) {
            pl.aplicador().aplicar(alvo.online);
            pl.enviar(alvo.online, "rank-recebido", "rank", rank.nomeColorido());
        }
        pl.enviar(s, "rank-definido", "jogador", alvo.nome, "rank", rank.nomeColorido());
    }

    private void criar(CommandSender s, String[] r) {
        if (r.length < 1) {
            uso(s, "/rank criar <nome>");
            return;
        }
        if (!Util.idValido(r[0])) {
            pl.enviar(s, "nome-invalido");
            return;
        }
        if (SUBS_ADMIN.contains(r[0].toLowerCase()) || r[0].equalsIgnoreCase("lista")
                || r[0].equalsIgnoreCase("info") || r[0].equalsIgnoreCase("nenhum")) {
            pl.enviar(s, "nome-reservado");
            return;
        }
        if (pl.dados().rank(r[0]) != null) {
            pl.enviar(s, "rank-ja-existe", "rank", r[0]);
            return;
        }
        Rank rank = pl.dados().criarRank(r[0].toLowerCase(), r[0]);
        pl.enviar(s, "rank-criado", "rank", rank.nomeColorido());
    }

    private void apagar(CommandSender s, String[] r) {
        if (r.length < 1) {
            uso(s, "/rank apagar <rank>");
            return;
        }
        Rank rank = rankOuErro(s, r[0]);
        if (rank == null) {
            return;
        }
        pl.dados().apagarRank(rank.id);
        pl.aplicador().aplicarTodos();
        pl.enviar(s, "rank-apagado", "rank", rank.id);
    }

    private void texto(CommandSender s, String[] r, String campo) {
        if (r.length < 2) {
            uso(s, "/rank " + campo + " <rank> <texto" + (campo.equals("nome") ? "" : "|nenhum") + ">");
            return;
        }
        Rank rank = rankOuErro(s, r[0]);
        if (rank == null) {
            return;
        }
        String valor = Util.juntar(r, 1);
        if (!campo.equals("nome") && Util.vazioOuNenhum(valor)) {
            valor = "";
        }
        switch (campo) {
            case "nome": rank.nome = valor; break;
            case "prefixo": rank.prefixo = valor; break;
            default: rank.sufixo = valor; break;
        }
        editado(s, rank, campo, valor);
    }

    private void cor(CommandSender s, String[] r, boolean chat) {
        String campo = chat ? "corchat" : "cor";
        if (r.length < 2) {
            uso(s, "/rank " + campo + " <rank> <cor|nenhuma>");
            return;
        }
        Rank rank = rankOuErro(s, r[0]);
        if (rank == null) {
            return;
        }
        String valor = Util.juntar(r, 1).trim();
        if (Util.vazioOuNenhum(valor)) {
            valor = "";
        } else if (!Cores.valida(valor)) {
            pl.enviar(s, "cor-invalida");
            return;
        }
        if (chat) {
            rank.corChat = valor;
        } else {
            rank.cor = valor;
        }
        editado(s, rank, campo, valor.isEmpty() ? "(nenhuma)" : Cores.traduzir(Cores.codigo(valor)) + valor);
    }

    private void editado(CommandSender s, Rank rank, String campo, String valor) {
        pl.dados().salvarRanks();
        pl.aplicador().aplicarTodos();
        pl.enviar(s, "rank-editado", "rank", rank.nomeColorido(), "campo", campo,
                "valor", valor.isEmpty() ? "(vazio)" : Cores.traduzir(valor));
    }

    private void permissao(CommandSender s, String[] r) {
        if (r.length < 2) {
            uso(s, "/rank permissao <rank> <add|remover|lista> [permissão]");
            return;
        }
        Rank rank = rankOuErro(s, r[0]);
        if (rank == null) {
            return;
        }
        String acao = r[1].toLowerCase();
        if (acao.equals("lista") || acao.equals("list")) {
            pl.enviar(s, "perm-lista", "rank", rank.nomeColorido(),
                    "permissoes", rank.permissoes.isEmpty() ? "(nenhuma)" : join(rank.permissoes));
            return;
        }
        if (r.length < 3) {
            uso(s, "/rank permissao <rank> <add|remover> <permissão>");
            return;
        }
        String perm = r[2].toLowerCase();
        boolean add = acao.equals("add") || acao.equals("adicionar") || acao.equals("dar");
        boolean rem = acao.equals("remover") || acao.equals("remove") || acao.equals("del") || acao.equals("tirar");
        if (add) {
            if (rank.permissoes.contains(perm)) {
                pl.enviar(s, "perm-ja-tem", "rank", rank.nomeColorido());
                return;
            }
            rank.permissoes.add(perm);
            pl.enviar(s, "perm-adicionada", "rank", rank.nomeColorido(), "permissao", perm);
        } else if (rem) {
            if (!rank.permissoes.remove(perm)) {
                pl.enviar(s, "perm-nao-tem", "rank", rank.nomeColorido());
                return;
            }
            pl.enviar(s, "perm-removida", "rank", rank.nomeColorido(), "permissao", perm);
        } else {
            uso(s, "/rank permissao <rank> <add|remover|lista> [permissão]");
            return;
        }
        pl.dados().salvarRanks();
        pl.aplicador().aplicarTodos();
    }

    private void herda(CommandSender s, String[] r) {
        if (r.length < 3) {
            uso(s, "/rank herda <rank> <add|remover> <outro-rank>");
            return;
        }
        Rank rank = rankOuErro(s, r[0]);
        if (rank == null) {
            return;
        }
        Rank outro = rankOuErro(s, r[2]);
        if (outro == null) {
            return;
        }
        String acao = r[1].toLowerCase();
        if (acao.equals("add") || acao.equals("adicionar")) {
            if (outro == rank || rank.herda.contains(outro.id) || pl.dados().herdaDe(outro, rank)) {
                pl.enviar(s, "herda-invalido");
                return;
            }
            rank.herda.add(outro.id);
            pl.enviar(s, "herda-adicionado", "rank", rank.nomeColorido(), "outro", outro.nomeColorido());
        } else if (acao.equals("remover") || acao.equals("remove") || acao.equals("del")) {
            if (!rank.herda.remove(outro.id)) {
                pl.enviar(s, "herda-nao-tem", "rank", rank.nomeColorido(), "outro", outro.nomeColorido());
                return;
            }
            pl.enviar(s, "herda-removido", "rank", rank.nomeColorido(), "outro", outro.nomeColorido());
        } else {
            uso(s, "/rank herda <rank> <add|remover> <outro-rank>");
            return;
        }
        pl.dados().salvarRanks();
        pl.aplicador().aplicarTodos();
    }

    private void padrao(CommandSender s, String[] r) {
        if (r.length < 1) {
            uso(s, "/rank padrao <rank>");
            return;
        }
        Rank rank = rankOuErro(s, r[0]);
        if (rank == null) {
            return;
        }
        pl.dados().definirPadrao(rank);
        pl.aplicador().aplicarTodos();
        pl.enviar(s, "padrao-definido", "rank", rank.nomeColorido());
    }

    private void mover(CommandSender s, String[] r) {
        if (r.length < 2) {
            uso(s, "/rank mover <rank> <cima|baixo>");
            return;
        }
        Rank rank = rankOuErro(s, r[0]);
        if (rank == null) {
            return;
        }
        String d = r[1].toLowerCase();
        boolean cima = d.equals("cima") || d.equals("up") || d.equals("subir");
        boolean baixo = d.equals("baixo") || d.equals("down") || d.equals("descer");
        if (!cima && !baixo) {
            uso(s, "/rank mover <rank> <cima|baixo>");
            return;
        }
        if (!pl.dados().mover(rank.id, cima)) {
            pl.enviar(s, "mover-limite", "rank", rank.nomeColorido());
            return;
        }
        pl.aplicador().aplicarTodos();
        pl.enviar(s, "mover-ok", "rank", rank.nomeColorido());
    }

    private static String join(List<String> l) {
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
        List<String> vazio = new ArrayList<String>();
        boolean admin = s.hasPermission("branks.admin");
        boolean setrank = c.getName().equalsIgnoreCase("setrank");
        if (setrank) {
            if (!admin) {
                return vazio;
            }
            if (a.length == 1) {
                return Util.filtrar(pl.nomesOnline(), a[0]);
            }
            if (a.length == 2) {
                return Util.filtrar(rankIds(true), a[1]);
            }
            return vazio;
        }
        if (a.length == 1) {
            List<String> subs = new ArrayList<String>(Arrays.asList("lista", "info"));
            if (admin) {
                subs.addAll(SUBS_ADMIN);
            }
            return Util.filtrar(subs, a[0]);
        }
        String sub = sub(a[0]);
        if (sub.equals("info") && a.length == 2) {
            return Util.filtrar(pl.nomesOnline(), a[1]);
        }
        if (!admin) {
            return vazio;
        }
        if (sub.equals("definir")) {
            if (a.length == 2) {
                return Util.filtrar(pl.nomesOnline(), a[1]);
            }
            if (a.length == 3) {
                return Util.filtrar(rankIds(true), a[2]);
            }
            return vazio;
        }
        if (a.length == 2 && !sub.equals("criar") && !sub.equals("recarregar") && SUBS_ADMIN.contains(sub)) {
            return Util.filtrar(rankIds(false), a[1]);
        }
        if (a.length == 3) {
            if (sub.equals("cor") || sub.equals("corchat")) {
                List<String> nomes = new ArrayList<String>(Cores.nomesDisponiveis());
                nomes.add("nenhuma");
                return Util.filtrar(nomes, a[2]);
            }
            if (sub.equals("permissao")) {
                return Util.filtrar(Arrays.asList("add", "remover", "lista"), a[2]);
            }
            if (sub.equals("herda")) {
                return Util.filtrar(Arrays.asList("add", "remover"), a[2]);
            }
            if (sub.equals("mover")) {
                return Util.filtrar(Arrays.asList("cima", "baixo"), a[2]);
            }
            if (sub.equals("prefixo") || sub.equals("sufixo")) {
                return Util.filtrar(Arrays.asList("nenhum"), a[2]);
            }
        }
        if (a.length == 4 && sub.equals("herda")) {
            return Util.filtrar(rankIds(false), a[3]);
        }
        return vazio;
    }

    private List<String> rankIds(boolean comPadrao) {
        List<String> l = new ArrayList<String>(pl.dados().ranks().keySet());
        if (comPadrao) {
            l.add("padrao");
        }
        return l;
    }
}
