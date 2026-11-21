package com.gridinv.client;

import com.gridinv.HudConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Petite Inventory keeps a per-screen on/off switch for its grid layout. Our screen replaces the
 * vanilla inventory, so Petite does not know about it; switch its grid layout on for our screen
 * (once per session) so multi-slot items work in the inventory and the Backpacked backpack.
 * Locked backpack slots are already recognised by Petite through Backpacked's UnlockableSlot.
 */
public final class PetiteCompat {
    private static final Logger LOGGER = LogManager.getLogger("gridinventory");
    private static boolean done;

    private PetiteCompat() {}

    public static void enableFor(Screen screen) {
        if (done || !HudConfig.CLIENT.petiteLayout.get()) return;
        if (!ModList.get().isLoaded("petiteinventory")) return;
        done = true;
        try {
            Class<?> c = Class.forName("com.sighs.petiteinventory.client.ScreenLayoutSettings");
            c.getMethod("setEnabled", Screen.class, boolean.class).invoke(null, screen, true);
            LOGGER.info("Enabled Petite Inventory grid layout for {}", screen.getClass().getName());
        } catch (Throwable t) {
            LOGGER.warn("Could not enable Petite Inventory layout for the grid inventory screen", t);
        }
    }
}
