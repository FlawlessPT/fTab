package pt.flawless.ftab;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class TabConfigTest {
    private static TabConfig parse(String yaml) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(yaml);
        return TabConfig.from(y);
    }

    @Test void convertePrefixosComECommercial() throws Exception {
        TabConfig c = parse("tags:\n  - group: admin\n    tag: \"&c[ADMIN]\"\ndefault-tag: \"&7[Membro]\"\nupdate-seconds: 8\nheader: [\"&aTopo\"]\n");
        assertEquals("§c[ADMIN] ", c.plan().select(Set.of("admin")).prefix());
        assertEquals("§7[Membro] ", c.plan().select(Set.of()).prefix());
        assertEquals(8, c.updateSeconds());
        assertEquals(List.of("§aTopo"), c.header());
    }

    @Test void updateSecondsOmissaoEMinimo() throws Exception {
        assertEquals(5, parse("default-tag: x\n").updateSeconds());
        assertEquals(1, parse("update-seconds: 0\n").updateSeconds());
    }

    @Test void headerFooterAusentesDaoListasVazias() throws Exception {
        TabConfig c = parse("default-tag: x\n");
        assertTrue(c.header().isEmpty());
        assertTrue(c.footer().isEmpty());
    }
}
