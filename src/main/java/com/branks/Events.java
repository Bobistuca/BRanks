package com.branks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Events implements Listener {
    private final BRanks pl;

    public Events(BRanks pl) {
        this.pl = pl;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e) {
        pl.data().registerJoin(e.getPlayer());
        pl.applier().apply(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        pl.applier().remove(e.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!pl.cfg().bool("chat.enabled")) {
            return;
        }
        Player p = e.getPlayer();
        Rank r = pl.data().rankOf(p.getUniqueId());

        String format = pl.applier().render(pl.cfg().text("chat.format"), p, true);
        if (!format.contains("{message}")) {
            format = format + " {message}";
        }
        format = Colors.translate(format).replace("{message}", "%2$s");
        e.setFormat(format);

        String msg = e.getMessage();
        if (pl.cfg().bool("chat.colors-in-message") && p.hasPermission("branks.chat.colors")) {
            msg = Colors.translate(msg);
        }
        if (r != null && !r.chatColor.isEmpty()) {
            msg = Colors.translate(Colors.code(r.chatColor)) + msg;
        }
        e.setMessage(msg);
    }
}
