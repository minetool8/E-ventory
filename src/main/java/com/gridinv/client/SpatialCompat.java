package com.gridinv.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Reflection bridge to Spatial GUI's public API (no build dependency; does nothing if it is absent). */
public final class SpatialCompat {
    private static boolean initialised;
    private static Method isActive, shouldHideHud, shouldHideHotbar;

    private SpatialCompat() {}

    private static void init() {
        if (initialised) return;
        initialised = true;
        if (!ModList.get().isLoaded("spatial_gui")) return;
        try {
            Class<?> c = Class.forName("org.tastytrash.spatialGUI.api.SpatialGUIAPI");
            isActive = c.getMethod("isActive");
            shouldHideHud = c.getMethod("shouldHideHud");
            shouldHideHotbar = c.getMethod("shouldHideHotbar");
        } catch (Throwable t) {
            isActive = shouldHideHud = shouldHideHotbar = null;
        }
    }

    private static boolean call(Method m) {
        if (m == null) return false;
        try { return (Boolean) m.invoke(null); } catch (Throwable t) { return false; }
    }

    /** Spatial GUI is currently presenting a screen as a 3D plane. */
    public static boolean active() {
        init();
        return call(isActive);
    }

    /** Spatial GUI is presenting this particular screen. */
    public static boolean activeFor(Screen screen) {
        return active() && Minecraft.getInstance().screen == screen;
    }

    /** Spatial GUI wants the 2D HUD (including our ring) out of the way. */
    public static boolean hidesHud() {
        init();
        return call(shouldHideHud) || call(shouldHideHotbar);
    }
}
