package pt.flawless.ftab;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;
import pt.flawless.fapi.permissions.PermissionsApi;
import pt.flawless.fapi.ranks.RankApi;
import java.util.concurrent.CompletableFuture;

/**
 * Mantém os jogadores nas equipas do scoreboard principal e dos scoreboards de cada jogador. Só mexe em equipas do fTab: nome
 * {@code NN_...} e presentes no {@link TeamPlan}; as de outros plugins nunca são tocadas.
 */
public final class TabService {
    private static final Pattern OWNED = Pattern.compile("\\d\\d_.+");
    private static final Pattern RANKED = Pattern.compile("\\d\\d_r[0-9a-f]{8}");

    private final JavaPlugin plugin;
    private final TeamPlan plan;
    private final PermissionsApi permissions;
    private final FConsoleLogger log;
    private final RankApi ranks; // opcional (fRankup)
    private final Set<String> ownedNames = new HashSet<>();

    public TabService(JavaPlugin plugin, TeamPlan plan, PermissionsApi permissions, FConsoleLogger log,
                      RankApi ranks) {
        this.ranks = ranks;
        this.plugin = plugin;
        this.plan = plan;
        this.permissions = permissions;
        this.log = log;
        plan.allTeams().forEach(s -> ownedNames.add(s.name()));
    }

    /** Scoreboard principal e o de cada jogador online, sem repetidos (vazio se o gestor ainda não existe). */
    private Set<Scoreboard> boards() {
        Set<Scoreboard> all = new LinkedHashSet<>();
        ScoreboardManager m = Bukkit.getScoreboardManager();
        if (m == null) return all;
        all.add(m.getMainScoreboard());
        for (Player p : Bukkit.getOnlinePlayers()) all.add(p.getScoreboard());
        return all;
    }

    public enum Placement { PLACE, SKIP_FOREIGN, ALREADY_IN_PLACE }

    /** Decide o que fazer com um jogador, dada a equipa atual (null = nenhuma) e a equipa alvo. */
    static Placement decide(String currentTeam, boolean currentOwned, String targetTeam) {
        if (currentTeam == null) return Placement.PLACE;
        if (!currentOwned) return Placement.SKIP_FOREIGN;
        return currentTeam.equals(targetTeam) ? Placement.ALREADY_IN_PLACE : Placement.PLACE;
    }

    /** Cria (ou atualiza o prefixo de) todas as equipas do plano. Thread principal. */
    public void createTeams() {
        for (Scoreboard b : boards()) plan.allTeams().forEach(s -> ensure(b, s));
    }

    private Team ensure(Scoreboard b, TeamSpec spec) {
        Team t = b.getTeam(spec.name());
        if (t == null) t = b.registerNewTeam(spec.name());
        // o setPrefix emite sempre um pacote de atualização da equipa: só quando muda
        if (!spec.prefix().equals(t.getPrefix())) t.setPrefix(spec.prefix());
        // nos clientes 1.13+ a cor do nome vem da cor da equipa, não do prefixo
        ChatColor color = lastColor(spec.prefix());
        if (t.getColor() != color) t.setColor(color);
        return t;
    }

    /** Última cor (não formato) do prefixo; branco se não houver. */
    static ChatColor lastColor(String prefix) {
        String codes = ChatColor.getLastColors(prefix);
        for (int i = codes.length() - 2; i >= 0; i -= 2) {
            ChatColor c = ChatColor.getByChar(codes.charAt(i + 1));
            if (c != null && c.isColor()) return c;
        }
        return ChatColor.WHITE;
    }

    private boolean owned(Team t) {
        String n = t.getName();
        return OWNED.matcher(n).matches() && (ownedNames.contains(n) || RANKED.matcher(n).matches());
    }

    /** Obtém os grupos e, na thread principal, põe o jogador na equipa certa. */
    public void apply(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            CompletableFuture<String> rank = ranks == null ? CompletableFuture.completedFuture("")
                    : ranks.getRankNumeral(uuid).exceptionally(e -> "");
            permissions.getGroups(uuid).thenCombine(rank, (g, r) -> new Object[]{g, r}).whenComplete((gr, error) -> {
                if (error != null) {
                    log.sendMessage("falha ao obter os grupos de " + player.getName() + ": " + error.getMessage(),
                            EMessageTypeEnum.ERROR);
                    return;
                }
                try {
                    @SuppressWarnings("unchecked") Set<String> groups = (Set<String>) gr[0];
                    Bukkit.getScheduler().runTask(plugin, () -> place(uuid, groups, (String) gr[1]));
                } catch (RuntimeException e) { // plugin desligado entretanto (IllegalPluginAccessException)
                    log.sendMessage("não foi possível agendar a atualização do tab: " + e.getMessage(),
                            EMessageTypeEnum.ERROR);
                }
            });
        } catch (RuntimeException e) {
            log.sendMessage("falha ao pedir os grupos de " + player.getName() + ": " + e.getMessage(),
                    EMessageTypeEnum.ERROR);
        }
    }

    private void place(UUID uuid, Set<String> groups, String rank) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        TeamSpec spec = plan.select(groups);
        if (!rank.isEmpty()) // uma equipa por grupo e rank: "[V] [ADMIN] nome"
            spec = new TeamSpec(spec.name().substring(0, 2) + "_r" + String.format("%08x", rank.hashCode()),
                    rank + " " + spec.prefix());
        for (Scoreboard b : boards()) {
            // um jogador só está numa equipa por scoreboard e o addEntry tira-o da outra em silêncio:
            // se for de outro plugin, deixa-se em paz para não haver luta a cada atualização
            Team current = b.getPlayerTeam(player);
            Placement p = decide(current == null ? null : current.getName(), current != null && owned(current),
                    spec.name());
            if (p == Placement.SKIP_FOREIGN) continue;
            Team target = ensure(b, spec);
            if (p == Placement.PLACE) {
                target.addEntry(player.getName());
                if (current != null && current.getEntries().isEmpty()) current.unregister(); // equipa de rank antiga
            }
        }
    }

    /** Tira o jogador das equipas do fTab. */
    public void remove(Player player) {
        String name = player.getName();
        for (Scoreboard b : boards())
            for (Team t : b.getTeams())
                if (owned(t) && t.hasEntry(name)) t.removeEntry(name);
    }

    /** Desregista as equipas do fTab. */
    public void shutdown() {
        for (Scoreboard b : boards())
            for (Team t : new HashSet<>(b.getTeams()))
                if (owned(t)) t.unregister();
    }
}
