package com.gridinv;

/**
 * Single source of truth for where everything sits, shared by the menu
 * (slot positions) and the screen (drawing). All values are GUI pixels
 * relative to the top-left of the whole inventory window.
 *
 * [ curios ] [ player      ] [ armor ] [ backpack  ]
 *            [ crafting 2x2]          [ inventory ]
 */
public final class GuiLayout {
    public static final int SLOT = 18;
    public static final int PLAYER_W = 100;
    public static final int ARMOR_CELL = 44;
    public static final int TAB_H = 14;
    public static final int CRAFT_W = 90;
    public static final int CRAFT_H = 44;

    public final int containerRows, centerW;
    public final int contX, contY, contW, contH;
    public final int bpCols, bpRows;
    public final int curioCount, curioCols, curioRows;
    public final int curioX, curioY, curioW, curioH;
    public final int playerX, armorX, armorY, rightX, rightW;
    public final int craftX, craftY;
    public final int frameY, frameH, invY, invH;
    public final int width, height;

    public GuiLayout(boolean hasBackpack, int cols, int rows, int curioCount, int containerRows) {
        this.containerRows = containerRows;
        this.centerW = containerRows > 0 ? 9 * SLOT + 16 : PLAYER_W;
        this.bpCols = hasBackpack ? cols : 9;
        this.bpRows = hasBackpack ? rows : 5;
        this.curioCount = curioCount;

        this.curioCols = curioCount <= 0 ? 0 : Math.max(2, (curioCount + 8) / 9);
        this.curioRows = curioCount <= 0 ? 0 : (curioCount + curioCols - 1) / curioCols;
        this.curioW = curioCount > 0 ? curioCols * SLOT + 8 : 0;
        this.curioH = curioCount > 0 ? curioRows * SLOT + 8 : 0;
        this.curioX = 0;

        this.playerX = curioW + (curioW > 0 ? 6 : 0);
        this.armorX = playerX + centerW + 8;
        this.rightX = armorX + ARMOR_CELL + 12;
        this.rightW = Math.max(bpCols, 9) * SLOT + 16;

        this.frameY = TAB_H;
        this.frameH = bpRows * SLOT + 16;
        this.invY = frameY + frameH + 6;
        this.invH = 3 * SLOT + 4 + SLOT + 16;

        this.contX = playerX;
        this.contY = frameY;
        this.contW = centerW;
        this.contH = containerRows * SLOT + 16;

        int baseHeight = invY + invH;
        int contNeed = containerRows > 0 ? TAB_H + contH + 6 + CRAFT_H : 0;
        this.width = rightX + rightW;
        this.height = Math.max(baseHeight, contNeed);
        this.armorY = (height - ARMOR_CELL * 4) / 2;
        this.curioY = Math.max(0, (height - curioH) / 2);

        this.craftX = playerX + (centerW - CRAFT_W) / 2;
        this.craftY = height - CRAFT_H;
    }

    // Slot item origin is 1px inside the 18x18 slot background.
    public int armorSlotX() { return armorX + (ARMOR_CELL - 16) / 2; }
    public int armorSlotY(int i) { return armorY + i * ARMOR_CELL + (ARMOR_CELL - 16) / 2; }

    public int mainX(int c) { return rightX + 8 + c * SLOT + 1; }
    public int mainY(int r) { return invY + 8 + r * SLOT + 1; }
    public int hotbarY() { return invY + 8 + 3 * SLOT + 4 + 1; }

    public int backpackX(int c) { return rightX + (rightW - bpCols * SLOT) / 2 + c * SLOT + 1; }
    public int backpackY(int r) { return frameY + 8 + r * SLOT + 1; }

    public int curioSlotX(int n) { return curioX + 4 + (n % curioCols) * SLOT + 1; }
    public int curioSlotY(int n) { return curioY + 4 + (n / curioCols) * SLOT + 1; }

    // opened container (chest etc.) shown where the player model normally is
    public int extX(int c) { return contX + (contW - 9 * SLOT) / 2 + c * SLOT + 1; }
    public int extY(int r) { return contY + 8 + r * SLOT + 1; }

    // crafting: 2x2 grid, arrow, result
    public int craftInX(int c) { return craftX + 4 + c * SLOT + 1; }
    public int craftInY(int r) { return craftY + 4 + r * SLOT + 1; }
    public int arrowX() { return craftX + 4 + 2 * SLOT + 6; }
    public int arrowY() { return craftY + CRAFT_H / 2 - 6; }
    public int resultX() { return craftX + 4 + 2 * SLOT + 6 + 16 + 6 + 1; }
    public int resultY() { return craftY + 4 + 9 + 1; }
}
