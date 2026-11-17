package com.gridinv;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Armor slot with the same rules as the vanilla inventory's armor slots. */
public class ArmorSlot extends Slot {
    public final EquipmentSlot type;
    private final Player player;

    public ArmorSlot(Player player, Container inv, int index, int x, int y, EquipmentSlot type) {
        super(inv, index, x, y);
        this.player = player;
        this.type = type;
    }

    @Override public int getMaxStackSize() { return 1; }

    @Override public boolean mayPlace(ItemStack stack) { return stack.canEquip(type, player); }

    @Override public boolean mayPickup(Player p) {
        ItemStack s = getItem();
        if (!s.isEmpty() && !p.isCreative() && EnchantmentHelper.hasBindingCurse(s)) return false;
        return super.mayPickup(p);
    }
}
