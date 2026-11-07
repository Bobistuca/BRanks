package com.branks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComandoTag implements CommandExecutor, TabCompleter {
    private final BRanks pl;

    private static final List<String> SUBS_ADMIN = Arrays.asList("definir", "criar", "apagar", "texto", "permissao");
    private static final List<String> RESERVADAS = Arrays.asList("lista", "remover", "definir", "criar", "apagar",
            "texto", "permissao", "nenhuma", "nenhum");

    public ComandoTag(BRanks pl) {
        this.pl = pl;
    }

    private static String sub(String s) {
        s = s.toLowerCase();
        switch (s) {
            case "list": case "listar": return "lista";
            case "off": case "tirar": case "limpar": case "clear": return "remover";
            case "set": case "setar": case "settag": return "definir";
            case "create": case "novo": return "criar";
            case "delete": case "deletar": case "excluir": return "apagar";
            case "text": return "texto";
            case "perm": case "permission": return "permissao";
            default: return s;
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("settag")) {
            String[] n = new String[a.length + 1];
            n[0] = "definir";
            System.arraycopy(a, 0, n, 1, a.length);
            a = n;
        }
        if (a.length == 0) {
            lista(s);
            pl.enviarLista(s, "ajuda-tag");
            if (s.hasPermission("branks.admin")) {
                pl.enviarLista(s, "ajuda-tag-admin");
            }
            return true;
        }
        String sub = sub(a[0]);
        String[] r = Util.resto(a, 1);

        if (sub.equals("lista")) {
            lista(s);
            return true;
        }
        if (sub.equals("remover")) {
            removerPropria(s);
            return true;
        }
        if (SUBS_ADMIN.contains(sub)) {
            if (!s.hasPermission("branks.admin")) {
                pl.enviar(s, "sem-permissao");
                return true;
            }
            switch (sub) {
                case "definir": definir(s, r); break;
                case "criar": criar(s, r); break;
                case "apagar": apagar(s, r); break;
                case "texto": texto(s, r); break;
                default: permissao(s, r); break;
            }
            return true;
        }
        usar(s, a[0]);
        return true;
    }

    private void uso(CommandSender s, String uso) {
        pl.enviar(s, "uso", "uso", uso);
    }

    private Tag tagOuErro(CommandSender s, String id) {
        Tag t = pl.dados().tag(id);
        if (t == null) {
            pl.enviar(s, "tag-nao-existe", "tag", id);
        }
        return t;
    }

    // ------------------------------------------------------------------ jogadores

    private void lista(CommandSender s) {
        if (pl.dados().tags().isEmpty()) {
            pl.enviar(s, "tag-lista-vazia");
            return;
        }
        pl.enviar(s, "tag-lista-titulo");
        for (Tag t : pl.dados().tags().values()) {
            boolean livre = !(s instanceof Player) || pl.dados().permitida((Player) s, t);
            pl.enviar(s, livre ? "tag-lista-livre" : "tag-lista-bloqueada",
                    "id", t.id, "texto", t.textoColorido());
        }
    }

    private void usar(CommandSender s, String id) {
        if (!(s instanceof Player)) {
            pl.enviar(s, "apenas-jogador");
            return;
        }
        Player p = (Player) s;
        if (!p.hasPermission("branks.tag.usar")) {
            pl.enviar(s, "sem-permissao");
            return;
        }
        Tag t = tagOuErro(s, id);
        if (t == null) {
            return;
        }
        if (!pl.dados().permitida(p, t)) {
            pl.enviar(s, "tag-sem-permissao", "texto", t.textoColorido());
            return;
        }
        pl.dados().definirTag(p.getUniqueId(), p.getName(), t.id);
        pl.aplicador().aplicar(p);
        pl.enviar(s, "tag-selecionada", "texto", t.textoColorido());
    }

    private void removerPropria(CommandSender s) {
        if (!(s instanceof Player)) {
            pl.enviar(s, "apenas-jogador");
            return;
        }
        Player p = (Player) s;
        if (pl.dados().tagEscolhida(p.getUniqueId()) == null) {
            pl.enviar(s, "tag-sem-tag");
            return;
        }
        pl.dados().definirTag(p.getUniqueId(), p.getName(), null);
        pl.aplicador().aplicar(p);
        pl.enviar(s, "tag-removida");
    }

    // ------------------------------------------------------------------ admin

    private void definir(CommandSender s, String[] r) {
        if (r.length < 2) {
            uso(s, "/tag definir <jogador> <tag|nenhuma>");
            return;
        }
        Util.Alvo alvo = Util.resolver(pl, r[0]);
        if (alvo == null) {
            pl.enviar(s, "jogador-nao-encontrado", "jogador", r[0]);
            return;
        }
        Tag t = pl.dados().tag(r[1]);
        if (t == null) {
            if (Util.vazioOuNenhum(r[1]) || r[1].equalsIgnoreCase("remover")) {
                pl.dados().definirTag(alvo.uuid, alvo.nome, null);
                if (alvo.online != null) {
                    pl.aplicador().aplicar(alvo.online);
                }
                pl.enviar(s, "tag-limpa", "jogador", alvo.nome);
                return;
            }
            pl.enviar(s, "tag-nao-existe", "tag", r[1]);
            return;
        }
        pl.dados().definirTag(alvo.uuid, alvo.nome, t.id);
        if (alvo.online != null) {
            pl.aplicador().aplicar(alvo.online);
        }
        String aviso = (alvo.online != null && !pl.dados().permitida(alvo.online, t))
                ? " &7(ele só verá a tag quando tiver a permissão " + t.permissao + ")" : "";
        pl.enviar(s, "tag-definida", "jogador", alvo.nome, "texto", t.textoColorido());
        if (!aviso.isEmpty()) {
            s.sendMessage(Cores.traduzir(aviso.trim()));
        }
    }

    private void criar(CommandSender s, String[] r) {
        if (r.length < 2) {
            uso(s, "/tag criar <nome> <texto>");
            return;
        }
        if (!Util.idValido(r[0])) {
            pl.enviar(s, "nome-invalido");
            return;
        }
        if (RESERVADAS.contains(r[0].toLowerCase())) {
            pl.enviar(s, "nome-reservado");
            return;
        }
        if (pl.dados().tag(r[0]) != null) {
            pl.enviar(s, "tag-ja-existe", "tag", r[0]);
            return;
        }
        Tag t = pl.dados().criarTag(r[0].toLowerCase(), Util.juntar(r, 1));
        pl.enviar(s, "tag-criada", "tag", t.id, "texto", t.textoColorido());
    }

    private void apagar(CommandSender s, String[] r) {
        if (r.length < 1) {
            uso(s, "/tag apagar <tag>");
            return;
        }
        Tag t = tagOuErro(s, r[0]);
        if (t == null) {
            return;
        }
        pl.dados().apagarTag(t.id);
        pl.aplicador().aplicarTodos();
        pl.enviar(s, "tag-apagada", "tag", t.id);
    }

    private void texto(CommandSender s, String[] r) {
        if (r.length < 2) {
            uso(s, "/tag texto <tag> <texto>");
            return;
        }
        Tag t = tagOuErro(s, r[0]);
        if (t == null) {
            return;
        }
        t.texto = Util.juntar(r, 1);
        pl.dados().salvarTags();
        pl.aplicador().aplicarTodos();
        pl.enviar(s, "tag-editada", "tag", t.id, "campo", "texto", "valor", t.textoColorido());
    }

    private void permissao(CommandSender s, String[] r) {
        if (r.length < 2) {
            uso(s, "/tag permissao <tag> <permissão|nenhuma>");
            return;
        }
        Tag t = tagOuErro(s, r[0]);
        if (t == null) {
            return;
        }
        t.permissao = Util.vazioOuNenhum(r[1]) ? "" : r[1].toLowerCase();
        pl.dados().salvarTags();
        pl.aplicador().aplicarTodos();
        pl.enviar(s, "tag-editada", "tag", t.id, "campo", "permissao",
                "valor", t.permissao.isEmpty() ? "(todos podem usar)" : t.permissao);
    }

    // ------------------------------------------------------------------ tab

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        List<String> vazio = new ArrayList<String>();
        boolean admin = s.hasPermission("branks.admin");
        if (c.getName().equalsIgnoreCase("settag")) {
            if (!admin) {
                return vazio;
            }
            if (a.length == 1) {
                return Util.filtrar(pl.nomesOnline(), a[0]);
            }
            if (a.length == 2) {
                return Util.filtrar(tagIds(true), a[1]);
            }
            return vazio;
        }
        if (a.length == 1) {
            List<String> l = new ArrayList<String>(tagIds(false));
            l.add("lista");
            l.add("remover");
            if (admin) {
                l.addAll(SUBS_ADMIN);
            }
            return Util.filtrar(l, a[0]);
        }
        if (!admin) {
            return vazio;
        }
        String sub = sub(a[0]);
        if (sub.equals("definir")) {
            if (a.length == 2) {
                return Util.filtrar(pl.nomesOnline(), a[1]);
            }
            if (a.length == 3) {
                return Util.filtrar(tagIds(true), a[2]);
            }
        } else if ((sub.equals("apagar") || sub.equals("texto") || sub.equals("permissao")) && a.length == 2) {
            return Util.filtrar(tagIds(false), a[1]);
        } else if (sub.equals("permissao") && a.length == 3) {
            return Util.filtrar(Arrays.asList("nenhuma"), a[2]);
        }
        return vazio;
    }

    private List<String> tagIds(boolean comNenhuma) {
        List<String> l = new ArrayList<String>(pl.dados().tags().keySet());
        if (comNenhuma) {
            l.add("nenhuma");
        }
        return l;
    }
}
