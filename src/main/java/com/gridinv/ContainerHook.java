package com.gridinv;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When a plain chest-style menu (chest, barrel, ender chest...) opens, swap it for the grid
 * inventory with that container shown where the player model normally is.
 */
@Mod.EventBusSubscriber(modid = GridInventoryMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ContainerHook {
    /**
     * LOWEST priority on purpose: other mods that react to the container opening (Rummage, Lootr...)
     * must finish with the original menu first. Closing it then fires their Close handlers, and
     * opening ours fires their Open handlers again for the menu the player actually uses.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getContainer().getClass() != ChestMenu.class) return;   // plain vanilla chest menus only
        ChestMenu chest = (ChestMenu) event.getContainer();
        EquipmentMenu.openWithContainer(player, chest.getContainer(), chest.getRowCount());
    }
}
