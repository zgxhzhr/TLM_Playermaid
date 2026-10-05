package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.menu.MenuAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S：请求打开人是狐界面。
 *
 * @param targetId 目标玩家实体 id；-1 表示打开者自己（按 E）
 * @param bauble   true=饰品界面，false=主界面
 */
public record OpenFoxMaidMenuPacket(int targetId, boolean bauble) {

    public static void encode(OpenFoxMaidMenuPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.targetId);
        buf.writeBoolean(packet.bauble);
    }

    public static OpenFoxMaidMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenFoxMaidMenuPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(OpenFoxMaidMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer visitor = context.getSender();
            if (visitor == null) {
                return;
            }
            ServerPlayer target;
            if (packet.targetId == -1 || packet.targetId == visitor.getId()) {
                target = visitor;
            } else {
                Entity entity = visitor.level().getEntity(packet.targetId);
                if (!(entity instanceof ServerPlayer serverPlayer)) {
                    return;
                }
                target = serverPlayer;
            }
            MenuAccess.open(visitor, target, packet.bauble);
        });
        context.setPacketHandled(true);
    }
}
