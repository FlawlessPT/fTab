package pt.flawless.ftab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.flawless.ftab.TabService.Placement.*;

import org.junit.jupiter.api.Test;

class TabServiceTest {
    @Test void semEquipaColoca() {
        assertEquals(PLACE, TabService.decide(null, false, "00_admin"));
    }

    @Test void equipaDeOutroPluginDeixaEmPaz() {
        assertEquals(SKIP_FOREIGN, TabService.decide("staff", false, "00_admin"));
    }

    @Test void equipaDoFTabDiferenteTroca() {
        assertEquals(PLACE, TabService.decide("01_vip", true, "00_admin"));
    }

    @Test void jaNaEquipaCerta() {
        assertEquals(ALREADY_IN_PLACE, TabService.decide("00_admin", true, "00_admin"));
    }
}
