package pt.flawless.ftab;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;
import pt.flawless.fapi.permissions.PermissionsApi;

/**
 * Mantém os jogadores nas equipas do scoreboard principal. Só mexe em equipas do fTab: nome
 * {@code NN_...} e presentes no {@link TeamPlan}; as de outros plugins nunca são tocadas.
 */
public final class TabService {
    private static final Pattern OWNED = Pattern.compile("\\d\\d_.+");

    private final JavaPlugin plugin;
    private final TeamPlan plan;
    private final PermissionsApi permissions;
    private final FConsoleLogger log;
    private final Set<String> ownedNames = new HashSet<>();

    public TabService(JavaPlugin plugin, TeamPlan plan, PermissionsApi permissions, FConsoleLogger log) {
        this.plugin = plugin;
        this.plan = plan;
        this.permissions = permissions;
        this.log = log;
        plan.allTeams().forEach(s -> ownedNames.add(s.name()));
    }

    /** Scoreboard principal, ou null se o gestor ainda não existe. */
    private Scoreboard board() {
        ScoreboardManager m = Bukkit.getScoreboardManager();
        return m == null ? null : m.getMainScoreboard();
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
        if (board() == null) return;
        plan.allTeams().forEach(this::ensure);
    }

    private Team ensure(TeamSpec spec) {
        Scoreboard b = board();
        if (b == null) return null;
        Team t = b.getTeam(spec.name());
        if (t == null) t = b.registerNewTeam(spec.name());
        // no 1.8 o setPrefix emite sempre um pacote de atualização da equipa: só quando muda
        if (!spec.prefix().equals(t.getPrefix())) t.setPrefix(spec.prefix());
        return t;
    }

    private boolean owned(Team t) {
        return OWNED.matcher(t.getName()).matches() && ownedNames.contains(t.getName());
    }

    /** Obtém os grupos e, na thread principal, põe o jogador na equipa certa. */
    public void apply(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            permissions.getGroups(uuid).whenComplete((groups, error) -> {
                if (error != null) {
                    log.sendMessage("falha ao obter os grupos de " + player.getName() + ": " + error.getMessage(),
                            EMessageTypeEnum.ERROR);
                    return;
                }
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> place(uuid, groups));
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

    private void place(UUID uuid, Set<String> groups) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        Scoreboard b = board();
        if (b == null) return;
        TeamSpec spec = plan.select(groups);
        // no 1.8 um jogador só está numa equipa por scoreboard e o addEntry tira-o da outra em silêncio:
        // se for de outro plugin, deixa-se em paz para não haver luta a cada atualização
        Team current = b.getPlayerTeam(player);
        Placement p = decide(current == null ? null : current.getName(), current != null && owned(current),
                spec.name());
        if (p == Placement.SKIP_FOREIGN) return;
        Team target = ensure(spec);
        if (p == Placement.PLACE) target.addEntry(player.getName());
    }

    /** Tira o jogador das equipas do fTab. */
    public void remove(Player player) {
        Scoreboard b = board();
        if (b == null) return;
        String name = player.getName();
        for (Team t : b.getTeams())
            if (owned(t) && t.hasEntry(name)) t.removeEntry(name);
    }

    /** Desregista as equipas do fTab. */
    public void shutdown() {
        Scoreboard b = board();
        if (b == null) return;
        for (Team t : new HashSet<>(b.getTeams()))
            if (owned(t)) t.unregister();
    }
}
