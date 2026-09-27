package net.styna.stynastmc.classicbar.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.StynaSTMC;

public record ExhaustionSyncPayload(float exhaustion) implements CustomPacketPayload {
    public static final Type<ExhaustionSyncPayload> TYPE = new Type<>(StynaSTMC.id("classicbar_exhaustion"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExhaustionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ExhaustionSyncPayload::exhaustion,
            ExhaustionSyncPayload::new);

    @Override
    public Type<ExhaustionSyncPayload> type() {
        return TYPE;
    }

    public void handle(Player player) {
        player.getFoodData().exhaustionLevel = exhaustion;
        ClassicBarNetwork.presentOnServer = true;
    }
}
