package com.gridinv.client;

import com.gridinv.GridInventoryMod;
import com.gridinv.HudConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Left: ring hotbar (selected slot centred and enlarged, the other eight on a ring).
 * Right: vanilla + Legendary Survival Overhaul bars, moved by translating the pose stack around
 * each overlay (no mixins, no calls into LSO internals).
 * Selection state is vanilla's Inventory.selected; this class only draws.
 */
public final class UnifiedHud {
    private static final String LSO = "legendarysurvivaloverhaul";

    private static final ResourceLocation PARCOOL_STAMINA = new ResourceLocation("parcool", "hud.stamina.host");

    private static final Logger LOGGER = LogManager.getLogger("gridinventory");
    private static final ResourceLocation RING_ID = new ResourceLocation(GridInventoryMod.MODID, "ring");
    private static final Set<ResourceLocation> SEEN = new HashSet<>();

    private static final Set<ResourceLocation> PUSHED = new HashSet<>();
    /** ForgeGui leftHeight/rightHeight values to restore after each moved bar. */
    private static final Map<ResourceLocation, int[]> SAVED = new HashMap<>();
    private static float visualSel = -1f;
    private static long lastMillis;

    private UnifiedHud() {}

    private static boolean enabled() {
        return HudConfig.CLIENT.hudEnabled.get();
    }

    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "ring", RING);
    }

    private static boolean isLso(ResourceLocation id, String path) {
        return id.getNamespace().equals(LSO) && id.getPath().equals(path);
    }

    public static void onOverlayPre(RenderGuiOverlayEvent.Pre e) {
        ResourceLocation id = e.getOverlay().id();
        if (HudConfig.CLIENT.logOverlayIds.get() && SEEN.add(id)) LOGGER.info("HUD overlay: {}", id);

        // Active effects are never shown: vanilla icons plus any other mod's potion/effect overlay.
        if (HudConfig.CLIENT.hideEffects.get()
                && (id.equals(VanillaGuiOverlay.POTION_ICONS.id())
                    || (!id.equals(RING_ID) && (id.getPath().contains("potion") || id.getPath().contains("effect"))))) {
            e.setCanceled(true);
            return;
        }

        // ParCool stamina: same place as before, just pulled down toward the bottom.
        if (id.equals(PARCOOL_STAMINA)) {
            int dy = HudConfig.CLIENT.staminaYOffset.get();
            if (dy != 0 && !e.isCanceled()) {
                PoseStack pose = e.getGuiGraphics().pose();
                pose.pushPose();
                pose.translate(0, dy, 0);
                PUSHED.add(id);
            }
            return;
        }

        if (!enabled()) return;
        // vanilla hotbar, plus any other mod's hotbar-style overlay (everything except our ring)
        if (id.equals(VanillaGuiOverlay.HOTBAR.id())
                || (!id.equals(RING_ID) && id.getPath().contains("hotbar"))) {
            e.setCanceled(true);
            return;
        }
        if (id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) && HudConfig.CLIENT.hideExperienceBar.get()) {
            e.setCanceled(true);
            return;
        }
        if (e.isCanceled() || !HudConfig.CLIENT.moveSurvivalRight.get()) return;

        int w = e.getWindow().getGuiScaledWidth();
        int h = e.getWindow().getGuiScaledHeight();
        int margin = HudConfig.CLIENT.hudMargin.get();
        int spacing = HudConfig.CLIENT.barRowSpacing.get();
        int reserve = HudConfig.CLIENT.taczReserveBottom.get();
        float rightEdge = w - margin;
        int baseBottom = h - margin - reserve;               // bottom edge of the lowest row

        // Row layout, bottom to top: hunger, thirst, hearts, armor, air. Temperature etc. sit to the left.
        int row = -1;
        boolean left = false;
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) || isLso(id, "health_overhaul")) { row = 2; left = true; }
        else if (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) { row = 3; left = true; }
        else if (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) || isLso(id, "cold_hunger")
                || id.equals(VanillaGuiOverlay.MOUNT_HEALTH.id())) row = 0;
        else if (isLso(id, "thirst")) row = 1;
        else if (id.equals(VanillaGuiOverlay.AIR_LEVEL.id())) row = 4;

        PoseStack pose = e.getGuiGraphics().pose();
        if (row >= 0) {
            if (!(Minecraft.getInstance().gui instanceof ForgeGui fg)) return;
            SAVED.put(id, new int[]{fg.leftHeight, fg.rightHeight});
            int heightValue = margin + reserve + 9 + row * spacing;   // bars draw at y = screenHeight - xHeight
            if (left) fg.leftHeight = heightValue; else fg.rightHeight = heightValue;
            // vanilla left bars are 81 wide from w/2-91; right bars end at w/2+91
            float dx = left ? rightEdge - (w / 2f - 91 + 81) : rightEdge - (w / 2f + 91);
            pose.pushPose();
            pose.translate(dx, 0, 0);
            PUSHED.add(id);
            return;
        }

        // Absolute-positioned LSO icons: natural positions read from LSO 2.4.7 (offsets default 0).
        float midY = baseBottom - (9 + spacing) / 2f;
        float tempX = rightEdge - 81 - 24;
        float dx, dy;
        if (isLso(id, "temperature")) {                  // 16px icon at (w/2-8, h-52)
            dx = tempX - (w / 2f - 8);
            dy = (midY - 8) - (h - 52);
        } else if (isLso(id, "wetness")) {               // 10px icon at (w/2-20, h-61)
            dx = (tempX - 14) - (w / 2f - 20);
            dy = (midY - 5) - (h - 61);
        } else if (isLso(id, "body_damage")) {           // ~16px figure at (w/2+92, h-33)
            dx = (tempX - 14 - 22) - (w / 2f + 92);
            dy = (midY - 8) - (h - 33);
        } else {
            return;
        }
        pose.pushPose();
        pose.translate(dx, dy, 0);
        PUSHED.add(id);
    }

    public static void onOverlayPost(RenderGuiOverlayEvent.Post e) {
        ResourceLocation id = e.getOverlay().id();
        if (PUSHED.remove(id)) e.getGuiGraphics().pose().popPose();
        int[] saved = SAVED.remove(id);
        if (saved != null && Minecraft.getInstance().gui instanceof ForgeGui fg) {
            fg.leftHeight = saved[0];
            fg.rightHeight = saved[1];
        }
    }

    private static final IGuiOverlay RING = (gui, g, partialTick, width, height) -> {
        Minecraft mc = gui.getMinecraft();
        if (!enabled() || mc.options.hideGui || mc.player == null || mc.gameMode == null
                || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        Inventory inv = mc.player.getInventory();
        int sel = inv.selected;

        long now = Util.getMillis();
        float dt = lastMillis == 0 ? 16f : Math.min(100f, now - lastMillis);
        lastMillis = now;
        if (visualSel < 0) visualSel = sel;
        float diff = sel - visualSel;
        diff -= 9f * Math.round(diff / 9f); // shortest cyclic path
        float s = HudConfig.CLIENT.scrollSmoothing.get().floatValue();
        float k = 1f - (float) Math.pow(1f - s, dt * 60f / 1000f);
        visualSel += diff * k;
        if (Math.abs(sel - visualSel) < 0.002f && Math.abs(diff) < 0.002f) visualSel = sel;
        visualSel = ((visualSel % 9f) + 9f) % 9f;

        int margin = HudConfig.CLIENT.hudMargin.get();
        float radius = HudConfig.CLIENT.ringRadius.get();
        float centerScale = HudConfig.CLIENT.centerScale.get().floatValue();
        float cx = margin + radius + 8f * centerScale + 2f;
        float cy = height - margin - radius - 8f * centerScale - 2f;

        // draw smaller (ring) items first so the centre item ends up on top
        Integer[] order = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        float[] rels = new float[9];
        for (int i = 0; i < 9; i++) rels[i] = (((i - visualSel) % 9f) + 9f) % 9f;
        java.util.Arrays.sort(order, (a, b) -> Float.compare(closeness(rels[a]), closeness(rels[b])));

        for (int slot : order) {
            float rel = rels[slot];
            float near = closeness(rel); // 1 at centre, 0 on the ring
            float px;
            float py;
            if (rel <= 1f) {
                px = Mth.lerp(rel, 0f, ringX(1f, radius));
                py = Mth.lerp(rel, 0f, ringY(1f, radius));
            } else if (rel >= 8f) {
                float t = rel - 8f;
                px = Mth.lerp(t, ringX(8f, radius), 0f);
                py = Mth.lerp(t, ringY(8f, radius), 0f);
            } else {
                px = ringX(rel, radius);
                py = ringY(rel, radius);
            }
            float sc = Mth.lerp(near, 1f, centerScale);
            drawSlot(g, mc, inv.items.get(slot), cx + px, cy + py, sc, near, slot);
        }

        if (HudConfig.CLIENT.showOffhand.get()) {
            ItemStack off = mc.player.getOffhandItem();
            if (!off.isEmpty()) {
                drawSlot(g, mc, off, cx + radius + 8f * centerScale + 20f, cy, 1f, 0f, 99);
            }
        }
    };

    /** 1 when the slot is at the centre, falling to 0 one step away in either direction. */
    private static float closeness(float rel) {
        return Mth.clamp(1f - Math.min(rel, 9f - rel), 0f, 1f);
    }

    private static float ringX(float k, float r) {
        return (float) Math.cos(Math.toRadians(-90.0 + (k - 1f) * 45.0)) * r;
    }

    private static float ringY(float k, float r) {
        return (float) Math.sin(Math.toRadians(-90.0 + (k - 1f) * 45.0)) * r;
    }

    private static void drawSlot(GuiGraphics g, Minecraft mc, ItemStack stack, float centerX, float centerY,
                                 float scale, float highlight, int seed) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(centerX - 8f * scale, centerY - 8f * scale, 0f);
        pose.scale(scale, scale, 1f);
        g.fill(-2, -2, 18, 18, highlight > 0.5f ? 0x66000000 : 0x33000000);
        int a = (int) (highlight * 0xCC);
        if (a > 0) {
            int c = (a << 24) | 0xFFFFFF;
            g.fill(-2, -2, 18, -1, c);
            g.fill(-2, 17, 18, 18, c);
            g.fill(-2, -1, -1, 17, c);
            g.fill(17, -1, 18, 17, c);
        }
        if (!stack.isEmpty()) {
            g.renderItem(mc.player, stack, 0, 0, seed);
            g.renderItemDecorations(mc.font, stack, 0, 0);
        }
        pose.popPose();
    }
}
