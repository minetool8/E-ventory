package com.gridinv.client;

import com.gridinv.GridInventoryMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class HudEvents {

    @Mod.EventBusSubscriber(modid = GridInventoryMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            UnifiedHud.registerOverlays(event);
        }
    }

    @Mod.EventBusSubscriber(modid = GridInventoryMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBus {
        // HIGHEST: remove the hotbar before any other mod (Petite Inventory) can draw its own copy.
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void preFirst(RenderGuiOverlayEvent.Pre e) { UnifiedHud.onOverlayPreFirst(e); }

        // LOWEST + receiveCanceled so we see the final canceled state set by LSO and others.
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void pre(RenderGuiOverlayEvent.Pre e) { UnifiedHud.onOverlayPre(e); }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void post(RenderGuiOverlayEvent.Post e) { UnifiedHud.onOverlayPost(e); }
    }
}
