package com.gridinv.client;

import com.gridinv.EquipmentMenu;
import com.gridinv.GridInventoryMod;
import com.gridinv.GuiLayout;
import com.mrcrayfish.backpacked.inventory.container.slot.UnlockableSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioSlot;

/**
 * [ curios ] [ big player ] [ armor ] [ Backpacked backpack ]
 *                                     [ inventory + hotbar  ]
 */
public class EquipmentScreen extends AbstractContainerScreen<EquipmentMenu> {
    private static final ResourceLocation BP_TEX =
            new ResourceLocation("backpacked", "textures/gui/backpack_assets.png");
    private static final ResourceLocation[] ARMOR_ICONS = {
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_head.png"),
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_chest.png"),
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_legs.png"),
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_feet.png")};

    private final GuiLayout L;

    public EquipmentScreen(EquipmentMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.empty());
        this.L = menu.layout;
        this.imageWidth = L.width;
        this.imageHeight = L.height;
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) { /* no vanilla labels */ }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;

        drawCurios(g, x, y);
        drawPlayer(g, x, y, mx, my);
        drawArmor(g, x, y);
        drawBackpack(g, x, y);

        // inventory panel + slots
        panel(g, x + L.rightX, y + L.invY, L.rightW, L.invH);
        for (int i = menu.mainStart; i < menu.hotbarEnd; i++) {
            Slot s = menu.slots.get(i);
            slotBg(g, x + s.x - 1, y + s.y - 1);
        }
    }

    // ---------------------------------------------------------------- sections

    private void drawCurios(GuiGraphics g, int x, int y) {
        if (L.curioCount <= 0) return;
        panel(g, x + L.curioX, y + L.curioY, L.curioW, L.curioH);
        for (int i = menu.curioStart; i < menu.curioEnd; i++) {
            Slot s = menu.slots.get(i);
            int sx = x + s.x - 1, sy = y + s.y - 1;
            slotBg(g, sx, sy);
            if (s instanceof CurioSlot cs && !s.hasItem()) {
                ResourceLocation icon = CuriosApi.getSlotIcon(cs.getIdentifier());
                TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
                g.blit(sx + 1, sy + 1, 0, 16, 16, sprite);
            }
        }
    }

    private void drawPlayer(GuiGraphics g, int x, int y, int mx, int my) {
        int cx = x + L.playerX + GuiLayout.PLAYER_W / 2;
        int feetY = y + L.height / 2 + 68;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, cx, feetY, 72, (float) (cx - mx), (float) (feetY - 117 - my), minecraft.player);
    }

    private void drawArmor(GuiGraphics g, int x, int y) {
        for (int i = 0; i < 4; i++) {
            int cx = x + L.armorX;
            int cy = y + L.armorY + i * GuiLayout.ARMOR_CELL;
            int w = GuiLayout.ARMOR_CELL, h = GuiLayout.ARMOR_CELL;
            g.fill(cx, cy, cx + w, cy + h, 0xFF8B8B8B);
            g.fill(cx, cy, cx + 2, cy + h, 0xFF373737);          // dark left edge
            g.fill(cx, cy + h - 1, cx + w, cy + h, 0xFFFFFFFF);  // light divider
            Slot s = menu.slots.get(menu.armorStart + i);
            if (!s.hasItem()) {
                g.blit(ARMOR_ICONS[i], cx + (w - 32) / 2, cy + (h - 32) / 2, 0, 0, 32, 32, 32, 32);
            }
        }
    }

    private void drawBackpack(GuiGraphics g, int x, int y) {
        int fx = x + L.rightX, fy = y + L.frameY;

        // title tab above the frame
        int tabW = 110, tabX = fx + (L.rightW - tabW) / 2;
        g.fill(tabX, fy - GuiLayout.TAB_H, tabX + tabW, fy + 2, 0xFF2C1E0D);
        g.fill(tabX + 1, fy - GuiLayout.TAB_H + 1, tabX + tabW - 1, fy + 2, 0xFFEFDBC4);
        Component title = menu.hasBackpack
                ? menu.backpackTitle : Component.translatable("gridinventory.backpack");
        g.drawCenteredString(font, title, tabX + tabW / 2, fy - GuiLayout.TAB_H + 3, 0xFF4A3A2A);

        // parchment frame cut from Backpacked's own texture sheet (128x128, 8px border)
        nineSlice(g, fx, fy, L.rightW, L.frameH, 128, 0, 128, 128, 8);

        if (!menu.hasBackpack) {
            Component msg = Component.translatable("gridinventory.no_backpack");
            g.drawCenteredString(font, msg, fx + L.rightW / 2, fy + L.frameH / 2 - 4, 0xFF4A3A2A);
            return;
        }

        for (int i = menu.backpackStart; i < menu.backpackEnd; i++) {
            Slot s = menu.slots.get(i);
            int sx = x + s.x - 1, sy = y + s.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B7B63);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFFB5A58B);
            if (s instanceof UnlockableSlot us && !us.isUnlocked()) {
                g.blit(BP_TEX, sx + 3, sy + 2, 245, 146, 11, 13, 256, 256); // padlock
            }
        }
    }

    // ---------------------------------------------------------------- tooltips

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        super.renderTooltip(g, mx, my);
        if (hoveredSlot instanceof CurioSlot cs && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            g.renderTooltip(font, Component.translatable("curios.identifier." + cs.getIdentifier()), mx, my);
        }
    }

    // ---------------------------------------------------------------- drawing helpers

    /** Vanilla-style grey panel. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
    }

    /** Vanilla-style inset 18x18 slot background. */
    private static void slotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    private void nineSlice(GuiGraphics g, int x, int y, int w, int h, int u, int v, int tw, int th, int b) {
        int cw = w - 2 * b, ch = h - 2 * b, sw = tw - 2 * b, sh = th - 2 * b;
        g.blit(BP_TEX, x, y, b, b, u, v, b, b, 256, 256);
        g.blit(BP_TEX, x + w - b, y, b, b, u + tw - b, v, b, b, 256, 256);
        g.blit(BP_TEX, x, y + h - b, b, b, u, v + th - b, b, b, 256, 256);
        g.blit(BP_TEX, x + w - b, y + h - b, b, b, u + tw - b, v + th - b, b, b, 256, 256);
        g.blit(BP_TEX, x + b, y, cw, b, u + b, v, sw, b, 256, 256);
        g.blit(BP_TEX, x + b, y + h - b, cw, b, u + b, v + th - b, sw, b, 256, 256);
        g.blit(BP_TEX, x, y + b, b, ch, u, v + b, b, sh, 256, 256);
        g.blit(BP_TEX, x + w - b, y + b, b, ch, u + tw - b, v + b, b, sh, 256, 256);
        g.blit(BP_TEX, x + b, y + b, cw, ch, u + b, v + b, sw, sh, 256, 256);
    }
}
