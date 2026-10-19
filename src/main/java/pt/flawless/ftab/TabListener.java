package pt.flawless.ftab;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Põe o jogador na equipa certa e envia o cabeçalho/rodapé ao entrar; tira-o da equipa ao sair. */
public final class TabListener implements Listener {
    private final TabService service;
    private final TabHeaderFooter headerFooter;

    public TabListener(TabService service, TabHeaderFooter headerFooter) {
        this.service = service;
        this.headerFooter = headerFooter;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        service.apply(e.getPlayer());
        headerFooter.send(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        service.remove(e.getPlayer());
    }
}
