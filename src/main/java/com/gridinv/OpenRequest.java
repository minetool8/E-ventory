package com.gridinv;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: "the player pressed E, please open the grid inventory". */
public class OpenRequest {
    public static void encode(OpenRequest msg, FriendlyByteBuf buf) {}

    public static OpenRequest decode(FriendlyByteBuf buf) { return new OpenRequest(); }

    public static void handle(OpenRequest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && !player.isSpectator()) {
                EquipmentMenu.open(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
