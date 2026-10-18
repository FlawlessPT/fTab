package pt.flawless.ftab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Escolhe a equipa do tab de um jogador: a primeira regra da lista cujo grupo ele tem. */
public final class TeamPlan {
    private static final int MAX = 16;

    private final List<GroupTag> rules;
    private final List<TeamSpec> specs = new ArrayList<>();
    private final TeamSpec defaultSpec;
    private final List<String> warnings = new ArrayList<>();

    public TeamPlan(List<GroupTag> rules, String defaultTag) {
        this.rules = List.copyOf(rules);
        for (int i = 0; i < rules.size(); i++)
            specs.add(spec(String.format("%02d_%s", i, rules.get(i).group()), rules.get(i).tag()));
        defaultSpec = spec("99_default", defaultTag);
    }

    private TeamSpec spec(String name, String tag) {
        String full = tag + " ";
        String clipped = clip(full);
        if (!clipped.equals(full)) warnings.add(tag);
        return new TeamSpec(name.length() > MAX ? name.substring(0, MAX) : name, clipped);
    }

    /** Primeira regra da lista cujo grupo o jogador tem; senão a equipa por omissão. */
    public TeamSpec select(Set<String> groups) {
        for (int i = 0; i < rules.size(); i++)
            for (String g : groups)
                if (g.equalsIgnoreCase(rules.get(i).group())) return specs.get(i);
        return defaultSpec;
    }

    /** Todas as equipas possíveis (regras e omissão), pela ordem de nome. */
    public List<TeamSpec> allTeams() {
        List<TeamSpec> all = new ArrayList<>(specs);
        all.add(defaultSpec);
        all.sort(Comparator.comparing(TeamSpec::name));
        return all;
    }

    /** Corta a 16 caracteres sem deixar um {@code §} solto no fim. */
    public static String clip(String prefix) {
        if (prefix.length() <= MAX) return prefix;
        String s = prefix.substring(0, MAX);
        return s.endsWith("§") ? s.substring(0, MAX - 1) : s;
    }

    /** Tags que foram cortadas (para o log no arranque). */
    public List<String> clippedWarnings() {
        return List.copyOf(warnings);
    }
}
