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
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import java.util.Locale;

/**
 * E-ventory screen, styled after The Last of Us Part II menus: charcoal wash with a vignette,
 * thin warm-white outlines, corner brackets, one amber accent.
 *
 * [ curios ] [ big player    ] [ armor ] [ backpack  ]
 *            [ crafting 2x2  ]           [ inventory ]
 * With a chest open the container takes the player's place.
 */
public class EquipmentScreen extends AbstractContainerScreen<EquipmentMenu> {
    // ---- TLOU2-ish palette: warm bone-white on soot
    private static final int C_INK        = 0xFF0C0C0A;
    private static final int C_PANEL_TOP  = 0xD2161510;
    private static final int C_PANEL_BOT  = 0xD00A0A08;
    private static final int C_LINE       = 0x58E6E2D3;
    private static final int C_LINE_HI    = 0xCCE6E2D3;
    private static final int C_TEXT       = 0xFFE6E2D3;
    private static final int C_DIM        = 0xFF8E8A7C;
    private static final int C_AMBER      = 0xFFD6A24A;
    private static final int C_AMBER_DIM  = 0xFF7E6230;
    private static final int C_SLOT       = 0xB0000000;
    private static final int C_SLOT_LINE  = 0x30E6E2D3;

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

    @Override
    protected void init() {
        super.init();
        PetiteCompat.enableFor(this);
    }

    // ------------------------------------------------------------ frame order

    /**
     * AbstractContainerScreen.render() draws the slots and calls renderBg(), but NOT the dimmed background
     * and NOT the tooltip - concrete screens do both themselves. Both were missing before, which is why
     * item names/descriptions never showed.
     */
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        double[] m = mapMouse(mx, my);
        int rx = (int) m[0], ry = (int) m[1];
        renderBackground(g);
        super.render(g, rx, ry, pt);
        renderTooltip(g, rx, ry);
    }

    /** Charcoal wash + vignette. The world stays visible. Spatial GUI draws its own backdrop. */
    @Override
    public void renderBackground(GuiGraphics g) {
        if (SpatialCompat.activeFor(this)) {
            super.renderBackground(g);   // lets Spatial GUI's own background handling run
            return;
        }
        g.fillGradient(0, 0, width, height, 0xA80A0A08, 0xC6050504);
        int steps = 34, w = 3;
        for (int i = 0; i < steps; i++) {
            float t = 1f - i / (float) steps;
            int c = ((int) (0x78 * t * t) << 24);
            g.fill(i * w, 0, i * w + w, height, c);
            g.fill(width - i * w - w, 0, width - i * w, height, c);
            g.fill(0, i * w, width, i * w + w, c);
            g.fill(0, height - i * w - w, width, height - i * w, c);
        }
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

    private boolean swallowRelease;

    @Override public boolean mouseClicked(double mx, double my, int b) {
        double[] m = mapMouse(mx, my);
        if ((b == 0 || b == 1) && tryDirectItemClick(m[0], m[1], b)) {
            swallowRelease = true;
            return true;
        }
        return super.mouseClicked(m[0], m[1], b);
    }
    @Override public boolean mouseReleased(double mx, double my, int b) {
        if (swallowRelease) { swallowRelease = false; return true; }
        double[] m = mapMouse(mx, my); return super.mouseReleased(m[0], m[1], b);
    }

    /**
     * Battery <-> flashlight: send the click straight to the menu as a plain PICKUP click, before any other
     * mod's screen hooks (Petite Inventory's placement rules) can swallow it.
     */
    private boolean tryDirectItemClick(double mx, double my, int button) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return false;
        Slot target = null;
        for (Slot s : menu.slots) {
            if (s.isActive() && mx >= leftPos + s.x - 1 && mx < leftPos + s.x + 17
                    && my >= topPos + s.y - 1 && my < topPos + s.y + 17) { target = s; break; }
        }
        if (target == null || !target.hasItem()) return false;
        String cn = EquipmentMenu.itemName(carried), sn = EquipmentMenu.itemName(target.getItem());
        boolean pair = (cn.contains("battery") && sn.contains("flashlight"))
                    || (cn.contains("flashlight") && sn.contains("battery"));
        if (!pair) return false;
        slotClicked(target, target.index, button, ClickType.PICKUP);
        return true;
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

        // inventory
        panel(g, x + L.rightX, y + L.invY, L.rightW, L.invH);
        for (int i = menu.mainStart; i < menu.hotbarEnd; i++) {
            Slot s = menu.slots.get(i);
            slotBg(g, x + s.x - 1, y + s.y - 1);
        }
        int sepY = y + L.hotbarY() - 3;                      // thin divider above the hotbar row
        g.fill(x + L.rightX + 8, sepY, x + L.rightX + L.rightW - 8, sepY + 1, C_LINE);

        // selected-slot outline (TLOU-style white frame)
        if (hoveredSlot != null && hoveredSlot.isActive() && menu.slots.indexOf(hoveredSlot) >= menu.mainStart) {
            outline(g, x + hoveredSlot.x - 1, y + hoveredSlot.y - 1, 18, 18, C_LINE_HI);
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
        // faint ground line under the feet
        g.fill(cx - 26, feetY + 1, cx + 26, feetY + 2, C_LINE);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, cx, feetY, scale, (float) (cx - mx), (float) (feetY - (int) (scale * 1.62f) - my), minecraft.player);
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
        g.fill(rx, ry, rx + 18, ry + 1, C_AMBER);                  // amber accent on the result

        // thin chevron arrow
        int ax = x + L.arrowX(), ay = y + L.arrowY();
        g.fill(ax, ay + 5, ax + 9, ay + 6, C_DIM);
        g.fill(ax + 7, ay + 2, ax + 8, ay + 3, C_DIM);
        g.fill(ax + 8, ay + 3, ax + 9, ay + 4, C_DIM);
        g.fill(ax + 9, ay + 4, ax + 10, ay + 5, C_DIM);
        g.fill(ax + 9, ay + 6, ax + 10, ay + 7, C_DIM);
        g.fill(ax + 8, ay + 7, ax + 9, ay + 8, C_DIM);
        g.fill(ax + 7, ay + 8, ax + 8, ay + 9, C_DIM);
    }

    /** Four equipment cards: thin frame, corner brackets, amber tick when worn. */
    private void drawArmorHolder(GuiGraphics g, int x, int y) {
        int cell = GuiLayout.ARMOR_CELL;
        for (int i = 0; i < 4; i++) {
            int cx = x + L.armorX + 1, cy = y + L.armorY + i * cell + 1;
            int w = cell - 2, h = cell - 2;
            boolean filled = menu.slots.get(menu.armorStart + i).hasItem();

            g.fillGradient(cx, cy, cx + w, cy + h, C_PANEL_TOP, C_PANEL_BOT);
            outline(g, cx, cy, w, h, filled ? C_LINE_HI : C_LINE);
            brackets(g, cx, cy, w, h, 5, filled ? C_LINE_HI : C_LINE);
            if (filled) g.fill(cx + 4, cy + 4, cx + 11, cy + 5, C_AMBER);   // worn tick

            // opaque well so the small 16px item can be hidden behind the 32px one
            int wx = cx + (w - 24) / 2, wy = cy + (h - 24) / 2;
            g.fill(wx, wy, wx + 24, wy + 24, C_INK);
            outline(g, wx, wy, 24, 24, C_SLOT_LINE);

            if (!filled) {
                g.blit(ARMOR_ICONS[i], cx + (w - 32) / 2, cy + (h - 32) / 2, 0, 0, 32, 32, 32, 32);
            }
        }
    }

    private void drawBackpack(GuiGraphics g, int x, int y) {
        int fx = x + L.rightX, fy = y + L.frameY;
        panel(g, fx, fy, L.rightW, L.frameH);
        Component title = menu.hasBackpack ? menu.backpackTitle : Component.translatable("gridinventory.backpack");
        header(g, fx, fy, L.rightW, title);

        if (!menu.hasBackpack) {
            Component msg = Component.translatable("gridinventory.no_backpack");
            g.drawCenteredString(font, msg, fx + L.rightW / 2, fy + L.frameH / 2 - 4, C_DIM);
            return;
        }
        for (int i = menu.backpackStart; i < menu.backpackEnd; i++) {
            Slot s = menu.slots.get(i);
            int sx = x + s.x - 1, sy = y + s.y - 1;
            slotBg(g, sx, sy);
            if (s instanceof UnlockableSlot us && !us.isUnlocked()) {
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xAA000000);          // locked slots sink into shadow
                g.blit(LOCK, sx + 3, sy + 2, 0, 0, 12, 14, 12, 14);
            }
        }
    }

    /** The opened chest/barrel, drawn where the player model usually is. */
    private void drawContainer(GuiGraphics g, int x, int y) {
        int fx = x + L.contX, fy = y + L.contY;
        panel(g, fx, fy, L.contW, L.contH);
        header(g, fx, fy, L.contW, containerTitle);
        for (int i = menu.extStart; i < menu.extEnd; i++) {
            Slot s = menu.slots.get(i);
            slotBg(g, x + s.x - 1, y + s.y - 1);
        }
    }

    // ------------------------------------------------------------ foreground: big armor items + tooltips

    /**
     * Armor items are drawn at 2x (32px), the same size as the silhouette icons. This runs after the normal
     * slot pass and before the tooltip, so tooltips stay on top.
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
            g.fill(sx, sy, sx + 16, sy + 16, C_INK);                    // hide the small 16px item
            if (hoveredSlot == s) g.fill(sx - 4, sy - 4, sx + 20, sy + 20, 0x26FFFFFF);
            g.pose().popPose();

            g.pose().pushPose();
            g.pose().translate(sx - 8, sy - 8, 130);
            g.pose().scale(2f, 2f, 1f);
            g.renderItem(st, 0, 0);
            g.renderItemDecorations(font, st, 0, 0);
            g.pose().popPose();
        }

        super.renderTooltip(g, mx, my);   // item name + description
        if (hoveredSlot instanceof CurioSlot cs && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            g.renderTooltip(font, Component.translatable("curios.identifier." + cs.getIdentifier()), mx, my);
        }
    }

    // ------------------------------------------------------------ drawing helpers

    /** Dark glass panel: soft vertical gradient, hairline border, corner brackets. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, C_PANEL_TOP, C_PANEL_BOT);
        outline(g, x, y, w, h, C_LINE);
        brackets(g, x, y, w, h, 5, C_LINE_HI);
    }

    /** Section header: small caps-style label with a hairline running out to the right edge. */
    private void header(GuiGraphics g, int px, int py, int pw, Component title) {
        String text = title.getString().toUpperCase(Locale.ROOT);
        int ty = py - GuiLayout.TAB_H + 3;
        g.drawString(font, text, px + 1, ty, C_TEXT, false);
        int lineX = px + 1 + font.width(text) + 5;
        g.fill(lineX, py - 5, px + pw, py - 4, C_LINE);
        g.fill(px, py - 5, px + 3, py - 4, C_AMBER);                    // amber lead-in
    }

    /** Recessed 18x18 slot. */
    private static void slotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, C_SLOT);
        outline(g, x, y, 18, 18, C_SLOT_LINE);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** L-shaped corner marks. */
    private static void brackets(GuiGraphics g, int x, int y, int w, int h, int len, int color) {
        g.fill(x, y, x + len, y + 1, color);                 g.fill(x, y, x + 1, y + len, color);
        g.fill(x + w - len, y, x + w, y + 1, color);         g.fill(x + w - 1, y, x + w, y + len, color);
        g.fill(x, y + h - 1, x + len, y + h, color);         g.fill(x, y + h - len, x + 1, y + h, color);
        g.fill(x + w - len, y + h - 1, x + w, y + h, color); g.fill(x + w - 1, y + h - len, x + w, y + h, color);
    }
}
