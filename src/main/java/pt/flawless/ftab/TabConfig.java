package pt.flawless.ftab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

/** Configuração do fTab. {@code header}/{@code footer}: uma linha por entrada, enviadas à lista de jogadores. */
public record TabConfig(TeamPlan plan, int updateSeconds, List<String> header, List<String> footer) {

    /** Lê a config, convertendo {@code &} em {@code §}; {@code update-seconds} mínimo 1, por omissão 5. */
    public static TabConfig from(ConfigurationSection c) {
        List<GroupTag> rules = new ArrayList<>();
        for (Map<?, ?> m : c.getMapList("tags")) {
            Object g = m.get("group"), t = m.get("tag");
            if (g != null && t != null) rules.add(new GroupTag(String.valueOf(g), color(String.valueOf(t))));
        }
        String def = color(c.getString("default-tag", ""));
        int secs = Math.max(1, c.getInt("update-seconds", 5));
        return new TabConfig(new TeamPlan(rules, def), secs,
                colors(c.getStringList("header")), colors(c.getStringList("footer")));
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static List<String> colors(List<String> l) {
        return l.stream().map(TabConfig::color).toList();
    }
}
