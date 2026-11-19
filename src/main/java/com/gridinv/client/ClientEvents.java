package com.gridinv.client;

import com.gridinv.GridInventoryMod;
import com.gridinv.OpenRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import com.gridinv.HudConfig;
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
        /** Creative inventory (and any other effect-rendering inventory screen) must not list effects. */
        @SubscribeEvent
        public static void onInventoryEffects(ScreenEvent.RenderInventoryMobEffects event) {
            if (HudConfig.CLIENT.hideEffects.get()) event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            // chest / barrel / ender chest: the server swaps it for our screen; remember the title
            if (event.getNewScreen() instanceof ContainerScreen cs && cs.getMenu().getClass() == ChestMenu.class) {
                EquipmentScreen.pendingContainerTitle = cs.getTitle();
                event.setCanceled(true);
                return;
            }
            if (event.getNewScreen() instanceof InventoryScreen) {
                Minecraft mc = Minecraft.getInstance();
                // creative keeps the vanilla creative inventory (vanilla opens it from InventoryScreen);
                // spectators have no server-side inventory to open
                if (mc.player == null || mc.player.isSpectator()
                        || (mc.gameMode != null && mc.gameMode.hasInfiniteItems())) return;
                event.setCanceled(true);
                GridInventoryMod.CHANNEL.sendToServer(new OpenRequest());
            }
        }
    }
}
