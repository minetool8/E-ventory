package com.gridinv;

import com.mojang.datafixers.util.Pair;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.backpack.BackpackState;
import com.mrcrayfish.backpacked.common.backpack.UnlockableSlots;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import com.mrcrayfish.backpacked.inventory.BackpackedInventoryAccess;
import com.mrcrayfish.backpacked.inventory.container.slot.BackpackSlot;
import com.mrcrayfish.backpacked.item.BackpackItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * One menu containing everything shown on the E screen. Slot order:
 * armor(4) | main(27) | hotbar(9) | backpack(n) | curios(m)
 */
public class EquipmentMenu extends AbstractContainerMenu {
    public static final EquipmentSlot[] ARMOR_ORDER =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public final GuiLayout layout;
    public final boolean hasBackpack;
    public final Component backpackTitle;

    public final int armorStart = 0, mainStart = 4, hotbarStart = 31, hotbarEnd = 40;
    public final int backpackStart = 40;
    public int backpackEnd, curioStart, curioEnd;

    /** Everything the client needs to build an identical menu. */
    private record Data(boolean hasBackpack, int cols, int rows, UnlockableSlots unlocked,
                        Component title, List<Pair<String, Integer>> curios) {
        void write(FriendlyByteBuf buf) {
            buf.writeBoolean(hasBackpack);
            buf.writeVarInt(cols);
            buf.writeVarInt(rows);
            if (hasBackpack) {
                unlocked.encode(buf);
                buf.writeComponent(title);
            }
            buf.writeVarInt(curios.size());
            for (Pair<String, Integer> c : curios) {
                buf.writeUtf(c.getFirst());
                buf.writeVarInt(c.getSecond());
            }
        }

        static Data read(FriendlyByteBuf buf) {
            boolean has = buf.readBoolean();
            int cols = buf.readVarInt();
            int rows = buf.readVarInt();
            UnlockableSlots unlocked = null;
            Component title = Component.empty();
            if (has) {
                unlocked = UnlockableSlots.decode(buf);
                title = buf.readComponent();
            }
            int n = buf.readVarInt();
            List<Pair<String, Integer>> curios = new ArrayList<>();
            for (int i = 0; i < n; i++) curios.add(Pair.of(buf.readUtf(), buf.readVarInt()));
            return new Data(has, cols, rows, unlocked, title, curios);
        }
    }

    // ------------------------------------------------------------------ server

    public static void open(ServerPlayer player) {
        Data data = collect(player);
        Container backpack = null;
        if (data.hasBackpack) {
            int idx = BackpackHelper.getSelectedBackpackIndex(player);
            backpack = ((BackpackedInventoryAccess) player).backpacked$GetBackpackInventory(idx);
        }
        final Container bp = backpack;
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new EquipmentMenu(id, inv, data, bp), Component.empty()),
                data::write);
    }

    private static Data collect(ServerPlayer player) {
        boolean has = false;
        int cols = 0, rows = 0;
        UnlockableSlots unlocked = null;
        Component title = Component.empty();

        int idx = BackpackHelper.getSelectedBackpackIndex(player);
        BackpackedInventoryAccess access = (BackpackedInventoryAccess) player;
        if (idx >= 0 && idx < access.backpacked$GetBackpackInventoryCount()) {
            BackpackInventory inv = access.backpacked$GetBackpackInventory(idx);
            ItemStack stack = BackpackHelper.getBackpackStack(player, idx);
            if (inv != null && !stack.isEmpty() && stack.getItem() instanceof BackpackItem item) {
                BackpackState state = inv.getState();
                if (!state.isInvalid()) {
                    has = true;
                    cols = state.cols();
                    rows = state.rows();
                    unlocked = item.getUnlockableSlots(stack);
                    title = stack.getHoverName();
                }
            }
        }

        List<Pair<String, Integer>> curios = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                new TreeMap<>(handler.getCurios()).forEach((id, sh) -> {
                    if (sh.isVisible() && sh.getSlots() > 0) curios.add(Pair.of(id, sh.getSlots()));
                }));
        return new Data(has, cols, rows, unlocked, title, curios);
    }

    // ------------------------------------------------------------------ client

    public EquipmentMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, Data.read(buf), null);
    }

    // ------------------------------------------------------------------ shared

    private EquipmentMenu(int id, Inventory inv, Data data, Container backpackContainer) {
        super(GridInventoryMod.EQUIPMENT_MENU.get(), id);
        Player player = inv.player;
        this.hasBackpack = data.hasBackpack;
        this.backpackTitle = data.title;

        int curioTotal = data.curios.stream().mapToInt(Pair::getSecond).sum();
        this.layout = new GuiLayout(data.hasBackpack, data.cols, data.rows, curioTotal);
        GuiLayout L = this.layout;

        // armor (container indexes 39..36 = head..feet)
        for (int i = 0; i < 4; i++) {
            addSlot(new ArmorSlot(player, inv, 39 - i, L.armorSlotX(), L.armorSlotY(i), ARMOR_ORDER[i]));
        }
        // main inventory
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, 9 + r * 9 + c, L.mainX(c), L.mainY(r)));
        // hotbar
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(inv, c, L.mainX(c), L.hotbarY()));

        // backpack
        if (hasBackpack) {
            FixedController controller = new FixedController(data.unlocked);
            Container container = backpackContainer != null
                    ? backpackContainer : new SimpleContainer(data.cols * data.rows);
            for (int r = 0; r < data.rows; r++)
                for (int c = 0; c < data.cols; c++)
                    addSlot(new BackpackSlot(controller, container, r * data.cols + c, L.backpackX(c), L.backpackY(r)));
        }
        backpackEnd = slots.size();

        // curios
        curioStart = slots.size();
        int n = 0;
        for (Pair<String, Integer> entry : data.curios) {
            String identifier = entry.getFirst();
            Optional<ICurioStacksHandler> sh = CuriosApi.getCuriosInventory(player).resolve()
                    .flatMap(h -> h.getStacksHandler(identifier));
            for (int i = 0; i < entry.getSecond(); i++, n++) {
                int x = L.curioSlotX(n), y = L.curioSlotY(n);
                if (sh.isPresent() && i < sh.get().getStacks().getSlots()) {
                    addSlot(new CurioSlot(player, sh.get().getStacks(), i, identifier, x, y,
                            sh.get().getRenders(), sh.get().canToggleRendering()));
                } else {
                    // Keep slot indexes identical on both sides even if curios data is late.
                    addSlot(new Slot(new SimpleContainer(1), 0, x, y) {
                        @Override public boolean mayPlace(ItemStack s) { return false; }
                    });
                }
            }
        }
        curioEnd = slots.size();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved = false;

        if (index < mainStart || index >= hotbarEnd) {
            // armor / backpack / curios -> player inventory
            moved = moveItemStackTo(stack, mainStart, hotbarEnd, true);
        } else {
            // inventory -> armor, curios, backpack, then main <-> hotbar
            EquipmentSlot eq = Mob.getEquipmentSlotForItem(stack);
            if (eq.getType() == EquipmentSlot.Type.ARMOR) {
                int ai = 3 - eq.getIndex();
                if (!slots.get(ai).hasItem()) moved = moveItemStackTo(stack, ai, ai + 1, false);
            }
            if (!moved && curioEnd > curioStart) moved = moveItemStackTo(stack, curioStart, curioEnd, false);
            if (!moved && hasBackpack) moved = moveItemStackTo(stack, backpackStart, backpackEnd, false);
            if (!moved) {
                moved = index < hotbarStart
                        ? moveItemStackTo(stack, hotbarStart, hotbarEnd, false)
                        : moveItemStackTo(stack, mainStart, hotbarStart, false);
            }
        }

        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override public boolean stillValid(Player player) { return true; }
}
