package pt.flawless.ftab;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Põe o jogador na equipa certa ao entrar e tira-o ao sair. */
public final class TabListener implements Listener {
    private final TabService service;

    public TabListener(TabService service) {
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        service.apply(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        service.remove(e.getPlayer());
    }
}
