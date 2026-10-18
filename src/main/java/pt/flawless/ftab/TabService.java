package pt.flawless.ftab;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
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

    private Scoreboard board() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    /** Cria (ou atualiza o prefixo de) todas as equipas do plano. Thread principal. */
    public void createTeams() {
        plan.allTeams().forEach(this::ensure);
    }

    private Team ensure(TeamSpec spec) {
        Team t = board().getTeam(spec.name());
        if (t == null) t = board().registerNewTeam(spec.name());
        t.setPrefix(spec.prefix());
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
        TeamSpec spec = plan.select(groups);
        Team target = ensure(spec);
        String name = player.getName();
        for (Team t : board().getTeams())
            if (t != target && owned(t) && t.hasEntry(name)) t.removeEntry(name);
        if (!target.hasEntry(name)) target.addEntry(name);
    }

    /** Tira o jogador das equipas do fTab. */
    public void remove(Player player) {
        String name = player.getName();
        for (Team t : board().getTeams())
            if (owned(t) && t.hasEntry(name)) t.removeEntry(name);
    }

    /** Desregista as equipas do fTab. */
    public void shutdown() {
        for (Team t : new HashSet<>(board().getTeams()))
            if (owned(t)) t.unregister();
    }
}
