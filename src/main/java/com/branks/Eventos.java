package com.branks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Eventos implements Listener {
    private final BRanks pl;

    public Eventos(BRanks pl) {
        this.pl = pl;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void aoEntrar(PlayerJoinEvent e) {
        pl.dados().registrarEntrada(e.getPlayer());
        pl.aplicador().aplicar(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoSair(PlayerQuitEvent e) {
        pl.aplicador().remover(e.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFalar(AsyncPlayerChatEvent e) {
        if (!pl.cfg().bool("chat.ativar")) {
            return;
        }
        Player p = e.getPlayer();
        Rank r = pl.dados().rankDe(p.getUniqueId());

        String formato = pl.aplicador().renderizar(pl.cfg().texto("chat.formato"), p, true);
        if (!formato.contains("{mensagem}")) {
            formato = formato + " {mensagem}";
        }
        formato = Cores.traduzir(formato).replace("{mensagem}", "%2$s");
        e.setFormat(formato);

        String msg = e.getMessage();
        if (pl.cfg().bool("chat.cores-na-mensagem") && p.hasPermission("branks.chat.cores")) {
            msg = Cores.traduzir(msg);
        }
        if (r != null && !r.corChat.isEmpty()) {
            msg = Cores.traduzir(Cores.codigo(r.corChat)) + msg;
        }
        e.setMessage(msg);
    }
}
