package pt.flawless.ftab;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TeamPlanTest {
    private static TeamPlan plan() {
        return new TeamPlan(List.of(new GroupTag("admin", "§c[ADMIN]"), new GroupTag("vip", "§2[VIP]")), "§7[Membro]");
    }

    @Test void escolhePrimeiroGrupoDaListaNaoDoConjunto() {
        assertEquals("00_admin", plan().select(Set.of("vip", "admin")).name());
    }

    @Test void grupoDesconhecidoDaEquipaPorOmissao() {
        TeamSpec s = plan().select(Set.of("xpto"));
        assertEquals("99_default", s.name());
        assertEquals("§7[Membro] ", s.prefix());
    }

    @Test void comparacaoSemMaiusculas() {
        assertEquals("01_vip", plan().select(Set.of("VIP")).name());
    }

    @Test void prefixoTerminaEmEspacoOuCorta() {
        String p = plan().select(Set.of("admin")).prefix();
        assertEquals("§c[ADMIN] ", p);
        assertEquals(10, p.length());
    }

    @Test void clipCortaA16() {
        assertEquals("abcdefghijklmnop", TeamPlan.clip("abcdefghijklmnopqrs"));
        assertEquals("abc", TeamPlan.clip("abc"));
    }

    @Test void clipNaoDeixaSecaoSolta() {
        assertEquals("abcdefghijklmno", TeamPlan.clip("abcdefghijklmno§c"));
    }

    @Test void nomesDeEquipaAte16() {
        TeamPlan p = new TeamPlan(List.of(new GroupTag("muitolongonomedegrupo", "§a[X]")), "");
        assertEquals(16, p.select(Set.of("muitolongonomedegrupo")).name().length());
    }

    @Test void allTeamsPelaOrdemDeNome() {
        assertEquals(List.of("00_admin", "01_vip", "99_default"),
                plan().allTeams().stream().map(TeamSpec::name).toList());
    }

    @Test void clippedWarningsListaTagsCortadas() {
        TeamPlan p = new TeamPlan(List.of(new GroupTag("a", "§c[MUITOLONGAAAAAA]")), "§7[M]");
        assertEquals(List.of("§c[MUITOLONGAAAAAA]"), p.clippedWarnings());
        assertTrue(plan().clippedWarnings().isEmpty());
    }
}
