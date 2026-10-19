package pt.flawless.ftab;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.server.v1_8_R1.ChatComponentText;
import net.minecraft.server.v1_8_R1.PacketPlayOutPlayerListHeaderFooter;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

/** Envia o cabeçalho e o rodapé da lista de jogadores (o cliente guarda-os até serem substituídos). */
public final class TabHeaderFooter {
    private final ChatComponentText header;
    private final ChatComponentText footer;
    private final Field footerField;

    public TabHeaderFooter(List<String> header, List<String> footer) {
        this.header = new ChatComponentText(String.join("\n", header));
        this.footer = new ChatComponentText(String.join("\n", footer));
        try {
            // o construtor do 1.8 R1 só aceita o cabeçalho; o rodapé é o campo "b"
            footerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("b");
            footerField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("pacote de cabeçalho/rodapé incompatível com esta versão", e);
        }
    }

    public boolean isEmpty() {
        return header.getText().isEmpty() && footer.getText().isEmpty();
    }

    public void send(Player p) {
        if (isEmpty()) return;
        try {
            PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(header);
            footerField.set(packet, footer);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
