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
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
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
    public final int externalRows;
    public int extStart, extEnd;
    private Container external;

    public final int armorStart = 0, mainStart = 4, hotbarStart = 31, hotbarEnd = 40;
    public final int backpackStart = 40;
    public int backpackEnd, curioStart, curioEnd;
    /** crafting: resultIndex is the result slot, followed by 4 input slots. */
    public int resultIndex, craftEnd;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player player;

    /** Everything the client needs to build an identical menu. */
    private record Data(boolean hasBackpack, int cols, int rows, UnlockableSlots unlocked,
                        Component title, List<Pair<String, Integer>> curios, int containerRows) {
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
            buf.writeVarInt(containerRows);
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
            int containerRows = buf.readVarInt();
            return new Data(has, cols, rows, unlocked, title, curios, containerRows);
        }
    }

    // ------------------------------------------------------------------ server

    /** Open the grid inventory with an external container (chest, barrel...) in the middle. */
    public static void openWithContainer(ServerPlayer player, Container container, int rows) {
        Data base = collect(player);
        Data data = new Data(base.hasBackpack, base.cols, base.rows, base.unlocked, base.title, base.curios, rows);
        // keep the chest 'open' while we swap menus so its lid does not close and reopen
        container.startOpen(player);
        openMenu(player, data, container);
    }

    public static void open(ServerPlayer player) {
        openMenu(player, collect(player), null);
    }

    private static void openMenu(ServerPlayer player, Data data, Container external) {
        Container backpack = null;
        if (data.hasBackpack) {
            int idx = BackpackHelper.getSelectedBackpackIndex(player);
            backpack = ((BackpackedInventoryAccess) player).backpacked$GetBackpackInventory(idx);
        }
        final Container bp = backpack;
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new EquipmentMenu(id, inv, data, bp, external), Component.empty()),
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
        return new Data(has, cols, rows, unlocked, title, curios, 0);
    }

    // ------------------------------------------------------------------ client

    public EquipmentMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, Data.read(buf), null, null);
    }

    // ------------------------------------------------------------------ shared

    private EquipmentMenu(int id, Inventory inv, Data data, Container backpackContainer, Container externalContainer) {
        super(GridInventoryMod.EQUIPMENT_MENU.get(), id);
        this.player = inv.player;
        this.hasBackpack = data.hasBackpack;
        this.backpackTitle = data.title;

        int curioTotal = data.curios.stream().mapToInt(Pair::getSecond).sum();
        this.layout = new GuiLayout(data.hasBackpack, data.cols, data.rows, curioTotal, data.containerRows);
        this.externalRows = data.containerRows;
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

        // crafting (2x2) below the player
        resultIndex = slots.size();
        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, L.resultX(), L.resultY()));
        for (int r = 0; r < 2; r++)
            for (int c = 0; c < 2; c++)
                addSlot(new Slot(craftSlots, c + r * 2, L.craftInX(c), L.craftInY(r)));
        craftEnd = slots.size();

        // opened container (chest/barrel) in the middle column
        extStart = slots.size();
        if (externalRows > 0) {
            external = externalContainer != null ? externalContainer : new SimpleContainer(externalRows * 9);
            for (int i = 0; i < externalRows * 9; i++)
                addSlot(new Slot(external, i, L.extX(i % 9), L.extY(i / 9)));
        }
        extEnd = slots.size();
    }

    // ---------------------------------------------------------------- crafting

    @Override
    public void slotsChanged(Container container) {
        if (container == craftSlots) updateResult();
    }

    private void updateResult() {
        Level level = player.level();
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return;
        ItemStack result = ItemStack.EMPTY;
        var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, level);
        if (recipe.isPresent()) {
            CraftingRecipe r = recipe.get();
            if (resultSlots.setRecipeUsed(level, sp, r)) {
                ItemStack assembled = r.assemble(craftSlots, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled;
            }
        }
        resultSlots.setItem(0, result);
        sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), resultIndex, result));
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        resultSlots.clearContent();
        if (!p.level().isClientSide) clearContainer(p, craftSlots);
        if (external != null) external.stopOpen(p);
    }

    /**
     * Item-on-item click behaviour (a battery on a flashlight, items on bundles, ...). This is exactly what
     * vanilla's doClick does first, done here so it always runs for our menu - on the client and the server -
     * even when another mod's click hooks would otherwise swallow the click.
     */
    @Override
    public void clicked(int slotId, int button, ClickType type, Player p) {
        if (type == ClickType.PICKUP && (button == 0 || button == 1) && slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            ItemStack carried = getCarried();
            ItemStack slotStack = slot.getItem();
            if (!carried.isEmpty() && !slotStack.isEmpty()) {
                ClickAction action = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
                SlotAccess carriedAccess = new SlotAccess() {
                    @Override public ItemStack get() { return getCarried(); }
                    @Override public boolean set(ItemStack s) { setCarried(s); return true; }
                };
                var features = p.level().enabledFeatures();
                boolean handled = (carried.isItemEnabled(features) && carried.overrideStackedOnOther(slot, action, p))
                        || (slotStack.isItemEnabled(features)
                            && slotStack.overrideOtherStackedOnMe(carried, slot, action, p, carriedAccess));
                if (handled) {
                    slot.setChanged();
                    return;
                }
            }
        }
        super.clicked(slotId, button, type, p);
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
            if (extEnd > extStart) moved = moveItemStackTo(stack, extStart, extEnd, false);
            EquipmentSlot eq = Mob.getEquipmentSlotForItem(stack);
            if (!moved && eq.getType() == EquipmentSlot.Type.ARMOR) {
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
        if (index == resultIndex) slot.onQuickCraft(stack, copy);
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override public boolean stillValid(Player player) { return external == null || external.stillValid(player); }
}
