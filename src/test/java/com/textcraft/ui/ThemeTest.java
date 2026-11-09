package com.textcraft.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import org.junit.jupiter.api.Test;

/** Theme colour mapping must match the spec palettes. */
class ThemeTest {
    @Test
    void lightPaletteMatchesSpec() {
        Palette p = Theme.LIGHT.palette();
        assertEquals(Color.decode("#F1F3F4"), p.workspace);
        assertEquals(Color.decode("#1A73E8"), p.accent);
        assertEquals(Color.decode("#202124"), p.text);
    }

    @Test
    void darkPaletteMatchesSpec() {
        Palette p = Theme.DARK.palette();
        assertEquals(Color.decode("#121316"), p.workspace);
        assertEquals(Color.decode("#8AB4F8"), p.accent);
        assertEquals(Color.decode("#E8EAED"), p.text);
    }

    @Test
    void systemHasNoPaletteOfItsOwn() {
        assertNull(Theme.SYSTEM.palette());
        assertNotNull(Theme.valueOf("SYSTEM"));
    }
}
