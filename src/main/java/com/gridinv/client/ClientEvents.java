package com.gridinv.client;

import com.gridinv.GridInventoryMod;
import com.gridinv.OpenRequest;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents {

    /** Mod-bus: register the screen for our menu type. */
    @Mod.EventBusSubscriber(modid = GridInventoryMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(GridInventoryMod.EQUIPMENT_MENU.get(), EquipmentScreen::new));
        }
    }

    /** Forge-bus: replace the vanilla E inventory with ours. */
    @Mod.EventBusSubscriber(modid = GridInventoryMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (event.getNewScreen() instanceof InventoryScreen) {
                event.setCanceled(true);
                GridInventoryMod.CHANNEL.sendToServer(new OpenRequest());
            }
        }
    }
}
