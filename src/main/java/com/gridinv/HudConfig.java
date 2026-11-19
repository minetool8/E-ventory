package com.gridinv;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Client config for the ring hotbar / survival-bar HUD (config/gridinventory-client.toml). */
public final class HudConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Client CLIENT;

    static {
        Pair<Client, ForgeConfigSpec> p = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = p.getLeft();
        SPEC = p.getRight();
    }

    private HudConfig() {}

    public static class Client {
        public final ForgeConfigSpec.BooleanValue hudEnabled;
        public final ForgeConfigSpec.IntValue hudMargin;
        public final ForgeConfigSpec.IntValue ringRadius;
        public final ForgeConfigSpec.DoubleValue centerScale;
        public final ForgeConfigSpec.DoubleValue scrollSmoothing;
        public final ForgeConfigSpec.BooleanValue showOffhand;
        public final ForgeConfigSpec.BooleanValue moveSurvivalRight;
        public final ForgeConfigSpec.IntValue barRowSpacing;
        public final ForgeConfigSpec.IntValue taczReserveBottom;
        public final ForgeConfigSpec.BooleanValue hideExperienceBar;
        public final ForgeConfigSpec.BooleanValue logOverlayIds;
        public final ForgeConfigSpec.BooleanValue petiteAutoEnable;
        public final ForgeConfigSpec.BooleanValue hideEffects;
        public final ForgeConfigSpec.BooleanValue removeVanillaHotbar;
        public final ForgeConfigSpec.BooleanValue hideHeldItemName;
        public final ForgeConfigSpec.IntValue staminaYOffset;

        Client(ForgeConfigSpec.Builder b) {
            b.push("hud");
            hudEnabled = b.comment("Replace the vanilla hotbar with the ring display and move survival bars to the right.")
                    .define("hudEnabled", true);
            hudMargin = b.defineInRange("hudMargin", 6, 0, 64);
            ringRadius = b.defineInRange("ringRadius", 34, 20, 80);
            centerScale = b.defineInRange("centerScale", 2.0, 1.0, 3.0);
            scrollSmoothing = b.comment("0-1, higher = snappier ring rotation when scrolling.")
                    .defineInRange("scrollSmoothing", 0.35, 0.05, 1.0);
            showOffhand = b.define("showOffhand", true);
            moveSurvivalRight = b.comment("Stack hearts, armor, hunger, thirst, temperature etc. in the bottom-right corner.")
                    .define("moveSurvivalRight", true);
            barRowSpacing = b.comment("Vertical distance between the stacked bars (GUI pixels).")
                    .defineInRange("barRowSpacing", 14, 10, 40);
            taczReserveBottom = b.comment("Empty space kept at the bottom-right for the TACZ gun/ammo HUD (GUI pixels).",
                            "The bar stack sits above this. Raise it if the bars overlap the gun HUD.")
                    .defineInRange("taczReserveBottom", 44, 0, 200);
            hideExperienceBar = b.comment("Hide the experience bar and level number.")
                    .define("hideExperienceBar", true);
            staminaYOffset = b.comment("ParCool stamina bar: pixels to pull DOWN from where ParCool draws it (negative = up).",
                            "Keeps its horizontal position. 0 = untouched.")
                    .defineInRange("staminaYOffset", 20, -300, 300);
            removeVanillaHotbar = b.comment("Always remove the vanilla hotbar (and any other mod's hotbar overlay), even if hudEnabled is false.")
                    .define("removeVanillaHotbar", true);
            hideHeldItemName = b.comment("Hide the held item's name that pops up above the hotbar for a few seconds.")
                    .define("hideHeldItemName", true);
            hideEffects = b.comment("Hide active potion/status effect icons everywhere (HUD and inventory screens).")
                    .define("hideEffects", true);
            petiteAutoEnable = b.comment("If Petite Inventory is installed, switch its grid layout on for the grid inventory screen.")
                    .define("petiteAutoEnable", true);
            logOverlayIds = b.comment("Log every HUD overlay id once (helps find which mod draws something you want hidden).")
                    .define("logOverlayIds", true);
            b.pop();
        }
    }
}
