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
        public final ForgeConfigSpec.IntValue survivalYOffset;

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
            moveSurvivalRight = b.define("moveSurvivalRight", true);
            survivalYOffset = b.comment("Pixels to shift the right-side survival cluster down (negative = up).")
                    .defineInRange("survivalYOffset", 16, -64, 64);
            b.pop();
        }
    }
}
