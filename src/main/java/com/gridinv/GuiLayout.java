package com.gridinv;

/**
 * Single source of truth for where everything sits, shared by the menu
 * (slot positions) and the screen (drawing). All values are GUI pixels
 * relative to the top-left of the whole inventory window.
 *
 * [ curios ] [ player ] [ armor ] [ backpack  ]
 *                                 [ inventory ]
 */
public final class GuiLayout {
    public static final int SLOT = 18;
    public static final int PLAYER_W = 100;
    public static final int ARMOR_CELL = 44;
    public static final int TAB_H = 14;

    public final int bpCols, bpRows;
    public final int curioCount, curioCols, curioRows;
    public final int curioX, curioY, curioW, curioH;
    public final int playerX, armorX, armorY, rightX, rightW;
    public final int frameY, frameH, invY, invH;
    public final int width, height;

    public GuiLayout(boolean hasBackpack, int cols, int rows, int curioCount) {
        this.bpCols = hasBackpack ? cols : 9;
        this.bpRows = hasBackpack ? rows : 5;
        this.curioCount = curioCount;

        this.curioCols = curioCount <= 0 ? 0 : Math.max(2, (curioCount + 8) / 9);
        this.curioRows = curioCount <= 0 ? 0 : (curioCount + curioCols - 1) / curioCols;
        this.curioW = curioCount > 0 ? curioCols * SLOT + 8 : 0;
        this.curioH = curioCount > 0 ? curioRows * SLOT + 8 : 0;
        this.curioX = 0;

        this.playerX = curioW + (curioW > 0 ? 6 : 0);
        this.armorX = playerX + PLAYER_W + 6;
        this.rightX = armorX + ARMOR_CELL + 10;
        this.rightW = Math.max(bpCols, 9) * SLOT + 16;

        this.frameY = TAB_H;
        this.frameH = bpRows * SLOT + 16;
        this.invY = frameY + frameH + 6;
        this.invH = 3 * SLOT + 4 + SLOT + 16;

        this.width = rightX + rightW;
        this.height = invY + invH;
        this.armorY = (height - ARMOR_CELL * 4) / 2;
        this.curioY = Math.max(0, (height - curioH) / 2);
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
}
