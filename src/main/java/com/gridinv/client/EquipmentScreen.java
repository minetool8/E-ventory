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
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioSlot;

/**
 * Dark-themed layout:
 * [ curios ] [ big player    ] [ armor ] [ Backpacked backpack ]
 *            [ crafting 2x2  ]           [ inventory + hotbar  ]
 */
public class EquipmentScreen extends AbstractContainerScreen<EquipmentMenu> {
    // ---- dark palette
    private static final int C_EDGE      = 0xFF0A0B0E;  // outer outline
    private static final int C_RIM       = 0xFF2F343D;  // 1px light rim
    private static final int C_TOP       = 0xFF21252C;  // panel gradient top
    private static final int C_BOT       = 0xFF171A1F;  // panel gradient bottom
    private static final int C_WELL      = 0xFF101216;  // slot well
    private static final int C_WELL_DARK = 0xFF08090B;
    private static final int C_WELL_LITE = 0xFF2A2E36;
    private static final int C_ACCENT    = 0xFF4C8DFF;
    private static final int C_ACCENT_DIM= 0xFF2F5FB8;
    private static final int C_TEXT      = 0xFFD7DBE2;
    private static final int C_TEXT_DIM  = 0xFF8A919D;

    private static final ResourceLocation LOCK =
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/lock.png");
    private static final ResourceLocation[] ARMOR_ICONS = {
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_head.png"),
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_chest.png"),
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_legs.png"),
            new ResourceLocation(GridInventoryMod.MODID, "textures/gui/armor_feet.png")};

    /** Title of the container that was just opened (captured before the vanilla screen is cancelled). */
    public static Component pendingContainerTitle = Component.empty();

    private final GuiLayout L;
    private final Component containerTitle;

    public EquipmentScreen(EquipmentMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.empty());
        this.L = menu.layout;
        this.containerTitle = pendingContainerTitle;
        this.imageWidth = L.width;
        this.imageHeight = L.height;
    }

    // ------------------------------------------------------------ background filter

    /** Dark translucent filter over the world (not black, the game stays visible). */
    @Override
    public void renderBackground(GuiGraphics g) {
        g.fillGradient(0, 0, width, height, 0x9910131A, 0xB00A0C11);
    }

    @Override
    protected void init() {
        super.init();
        PetiteCompat.enableFor(this);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) { /* no vanilla labels */ }

    // ------------------------------------------------------------ big hit area for armor cells

    /** Clicking/hovering anywhere in an armor cell targets its slot. */
    private double[] mapMouse(double mx, double my) {
        for (int i = 0; i < 4; i++) {
            int cx = leftPos + L.armorX, cy = topPos + L.armorY + i * GuiLayout.ARMOR_CELL;
            if (mx >= cx && mx < cx + GuiLayout.ARMOR_CELL && my >= cy && my < cy + GuiLayout.ARMOR_CELL) {
                Slot s = menu.slots.get(menu.armorStart + i);
                return new double[]{leftPos + s.x + 8, topPos + s.y + 8};
            }
        }
        return new double[]{mx, my};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        double[] m = mapMouse(mx, my);
        super.render(g, (int) m[0], (int) m[1], pt);
    }

    @Override public boolean mouseClicked(double mx, double my, int b) {
        double[] m = mapMouse(mx, my); return super.mouseClicked(m[0], m[1], b);
    }
    @Override public boolean mouseReleased(double mx, double my, int b) {
        double[] m = mapMouse(mx, my); return super.mouseReleased(m[0], m[1], b);
    }
    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        double[] m = mapMouse(mx, my); return super.mouseDragged(m[0], m[1], b, dx, dy);
    }

    // ------------------------------------------------------------ background

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;

        drawCurios(g, x, y);
        if (menu.externalRows > 0) drawContainer(g, x, y); else drawPlayer(g, x, y, mx, my);
        drawCrafting(g, x, y);
        drawArmorHolder(g, x, y);
        drawBackpack(g, x, y);

        panel(g, x + L.rightX, y + L.invY, L.rightW, L.invH);
        for (int i = menu.mainStart; i < menu.hotbarEnd; i++) {
            Slot s = menu.slots.get(i);
            slotBg(g, x + s.x - 1, y + s.y - 1);
        }
    }

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
        int cx = x + L.playerX + L.centerW / 2;
        int feetY = y + L.craftY - 8;
        int scale = Math.min(72, (int) ((L.craftY - 22) / 1.85f));
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, cx, feetY, scale, (float) (cx - mx), (float) (feetY - (int) (scale * 1.62f) - my), minecraft.player);
    }

    /** The opened chest/barrel, drawn where the player model usually is. */
    private void drawContainer(GuiGraphics g, int x, int y) {
        int fx = x + L.contX, fy = y + L.contY;
        panel(g, fx, fy, L.contW, L.contH);
        g.fill(fx + 3, fy + 2, fx + L.contW - 3, fy + 3, C_ACCENT_DIM);

        int tabW = Math.min(L.contW - 20, Math.max(80, font.width(containerTitle) + 24));
        int tabX = fx + (L.contW - tabW) / 2, tabY = fy - GuiLayout.TAB_H;
        g.fill(tabX, tabY, tabX + tabW, fy + 1, C_EDGE);
        g.fill(tabX + 1, tabY + 1, tabX + tabW - 1, fy + 1, C_RIM);
        g.fillGradient(tabX + 2, tabY + 2, tabX + tabW - 2, fy + 1, C_TOP, C_BOT);
        g.drawCenteredString(font, containerTitle, tabX + tabW / 2, tabY + 4, C_TEXT);

        for (int i = menu.extStart; i < menu.extEnd; i++) {
            Slot s = menu.slots.get(i);
            slotBg(g, x + s.x - 1, y + s.y - 1);
        }
    }

    private void drawCrafting(GuiGraphics g, int x, int y) {
        panel(g, x + L.craftX, y + L.craftY, GuiLayout.CRAFT_W, GuiLayout.CRAFT_H);
        for (int i = menu.resultIndex + 1; i < menu.craftEnd; i++) {
            Slot s = menu.slots.get(i);
            slotBg(g, x + s.x - 1, y + s.y - 1);
        }
        Slot res = menu.slots.get(menu.resultIndex);
        int rx = x + res.x - 1, ry = y + res.y - 1;
        slotBg(g, rx, ry);
        g.fill(rx, ry, rx + 18, ry + 1, C_ACCENT_DIM);           // accent top on the result slot

        // arrow
        int ax = x + L.arrowX(), ay = y + L.arrowY();
        g.fill(ax, ay + 5, ax + 9, ay + 7, C_TEXT_DIM);
        g.fill(ax + 9, ay + 2, ax + 10, ay + 10, C_TEXT_DIM);
        g.fill(ax + 10, ay + 3, ax + 11, ay + 9, C_TEXT_DIM);
        g.fill(ax + 11, ay + 4, ax + 12, ay + 8, C_TEXT_DIM);
        g.fill(ax + 12, ay + 5, ax + 13, ay + 7, C_TEXT_DIM);
    }

    /** Armor holder: one framed housing with four stepped cells. */
    private void drawArmorHolder(GuiGraphics g, int x, int y) {
        int cell = GuiLayout.ARMOR_CELL;
        int hx = x + L.armorX - 3, hy = y + L.armorY - 3;
        int hw = cell + 6, hh = cell * 4 + 6;

        // outer housing
        g.fill(hx, hy, hx + hw, hy + hh, C_EDGE);
        g.fill(hx + 1, hy + 1, hx + hw - 1, hy + hh - 1, C_RIM);
        g.fill(hx + 2, hy + 2, hx + hw - 2, hy + hh - 2, C_EDGE);

        for (int i = 0; i < 4; i++) {
            int cx = x + L.armorX, cy = y + L.armorY + i * cell;
            boolean filled = menu.slots.get(menu.armorStart + i).hasItem();

            // cell body: subtle vertical gradient + top sheen + bottom shadow
            g.fillGradient(cx, cy, cx + cell, cy + cell, 0xFF272B33, 0xFF16181D);
            g.fill(cx, cy, cx + cell, cy + 1, 0x22FFFFFF);
            g.fill(cx, cy + cell - 1, cx + cell, cy + cell, 0x66000000);
            if (i > 0) g.fill(cx, cy, cx + cell, cy + 1, C_EDGE);   // divider between cells

            // accent bar on the left: bright when equipped
            g.fill(cx, cy + 3, cx + 2, cy + cell - 3, filled ? C_ACCENT : C_ACCENT_DIM);

            // corner ticks
            g.fill(cx + cell - 5, cy + 3, cx + cell - 3, cy + 4, C_RIM);
            g.fill(cx + cell - 4, cy + 3, cx + cell - 3, cy + 6, C_RIM);

            // recessed well around the item
            int wx = cx + (cell - 24) / 2 + 1, wy = cy + (cell - 24) / 2;
            g.fill(wx - 1, wy - 1, wx + 25, wy + 25, C_WELL_DARK);
            g.fill(wx, wy, wx + 24, wy + 24, C_WELL);
            g.fill(wx, wy + 23, wx + 24, wy + 24, C_WELL_LITE);

            if (!filled) {
                g.blit(ARMOR_ICONS[i], cx + (cell - 32) / 2 + 1, cy + (cell - 32) / 2, 0, 0, 32, 32, 32, 32);
            }
        }
    }

    private void drawBackpack(GuiGraphics g, int x, int y) {
        int fx = x + L.rightX, fy = y + L.frameY;

        panel(g, fx, fy, L.rightW, L.frameH);
        g.fill(fx + 3, fy + 2, fx + L.rightW - 3, fy + 3, C_ACCENT_DIM);   // accent line under the tab

        // title tab
        int tabW = 110, tabX = fx + (L.rightW - tabW) / 2, tabY = fy - GuiLayout.TAB_H;
        g.fill(tabX, tabY, tabX + tabW, fy + 1, C_EDGE);
        g.fill(tabX + 1, tabY + 1, tabX + tabW - 1, fy + 1, C_RIM);
        g.fillGradient(tabX + 2, tabY + 2, tabX + tabW - 2, fy + 1, C_TOP, C_BOT);
        Component title = menu.hasBackpack
                ? menu.backpackTitle : Component.translatable("gridinventory.backpack");
        g.drawCenteredString(font, title, tabX + tabW / 2, tabY + 4, C_TEXT);

        if (!menu.hasBackpack) {
            Component msg = Component.translatable("gridinventory.no_backpack");
            g.drawCenteredString(font, msg, fx + L.rightW / 2, fy + L.frameH / 2 - 4, C_TEXT_DIM);
            return;
        }

        for (int i = menu.backpackStart; i < menu.backpackEnd; i++) {
            Slot s = menu.slots.get(i);
            int sx = x + s.x - 1, sy = y + s.y - 1;
            slotBg(g, sx, sy);
            if (s instanceof UnlockableSlot us && !us.isUnlocked()) {
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x88000000);           // dim locked slots
                g.blit(LOCK, sx + 3, sy + 2, 0, 0, 12, 14, 12, 14);
            }
        }
    }

    // ------------------------------------------------------------ foreground: big armor items

    /**
     * Armor items are drawn at 2x (32px), the same size as the silhouette icons.
     * Runs after the normal slot pass and before the tooltip, so tooltips stay on top.
     */
    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < 4; i++) {
            Slot s = menu.slots.get(menu.armorStart + i);
            ItemStack st = s.getItem();
            if (st.isEmpty()) continue;
            int sx = leftPos + s.x, sy = topPos + s.y;

            g.pose().pushPose();
            g.pose().translate(0, 0, 280);
            g.fill(sx, sy, sx + 16, sy + 16, C_WELL);                  // hide the small 16px item
            if (hoveredSlot == s) g.fill(sx - 4, sy - 4, sx + 20, sy + 20, 0x30FFFFFF);
            g.pose().popPose();

            g.pose().pushPose();
            g.pose().translate(sx - 8, sy - 8, 130);
            g.pose().scale(2f, 2f, 1f);
            g.renderItem(st, 0, 0);
            g.renderItemDecorations(font, st, 0, 0);
            g.pose().popPose();
        }

        super.renderTooltip(g, mx, my);
        if (hoveredSlot instanceof CurioSlot cs && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            g.renderTooltip(font, Component.translatable("curios.identifier." + cs.getIdentifier()), mx, my);
        }
    }

    // ------------------------------------------------------------ drawing helpers

    /** Dark panel: outline, light rim, gradient fill. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, C_EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, C_RIM);
        g.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, C_TOP, C_BOT);
    }

    /** Recessed 18x18 slot background. */
    private static void slotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, C_WELL_DARK);
        g.fill(x + 1, y + 1, x + 18, y + 18, C_WELL_LITE);
        g.fill(x + 1, y + 1, x + 17, y + 17, C_WELL);
    }
}
