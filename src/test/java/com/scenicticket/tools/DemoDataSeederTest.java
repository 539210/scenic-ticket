package com.scenicticket.tools;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataSeederTest {
    @Test
    void buildsFiftyUniqueUsersAndScenicsInReservedRanges() {
        var users = DemoDataSeeder.buildUsers(50);
        var scenics = DemoDataSeeder.buildScenics(50);

        assertEquals(50, users.size());
        assertEquals(50, scenics.size());
        assertEquals(50, new HashSet<>(users.stream().map(DemoDataSeeder.DemoUser::username).toList()).size());
        assertEquals(50, new HashSet<>(scenics.stream().map(DemoDataSeeder.DemoScenic::title).toList()).size());
        assertEquals(20_001L, users.getFirst().userId());
        assertEquals(20_050L, scenics.getLast().itemId());
        assertTrue(scenics.stream().allMatch(scenic -> scenic.price().signum() > 0));
    }

    @Test
    void rejectsCountsOutsideReservedRange() {
        assertThrows(IllegalArgumentException.class, () -> DemoDataSeeder.buildUsers(0));
        assertThrows(IllegalArgumentException.class, () -> DemoDataSeeder.buildScenics(51));
    }
}
