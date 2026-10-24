package pt.flawless.ftab;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/** Envia o cabeçalho e o rodapé da lista de jogadores (o cliente guarda-os até serem substituídos). */
public final class TabHeaderFooter {
    private final String headerText;
    private final String footerText;
    private final Component header;
    private final Component footer;

    public TabHeaderFooter(List<String> header, List<String> footer) {
        this.headerText = String.join("\n", header);
        this.footerText = String.join("\n", footer);
        this.header = LegacyComponentSerializer.legacySection().deserialize(headerText);
        this.footer = LegacyComponentSerializer.legacySection().deserialize(footerText);
    }

    public boolean isEmpty() {
        return headerText.isEmpty() && footerText.isEmpty();
    }

    public void send(Player p) {
        if (isEmpty()) return;
        p.sendPlayerListHeaderAndFooter(header, footer);
    }
}
