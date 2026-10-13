package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.menu.MenuAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：请求打开人是狐界面。
 *
 * @param targetId 目标玩家实体 id；-1 表示打开者自己（按背包键）
 * @param bauble   true=饰品界面，false=主界面
 */
public record OpenFoxMaidMenuPacket(int targetId, boolean bauble) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenFoxMaidMenuPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "open_fox_maid_menu"));

    public static final StreamCodec<FriendlyByteBuf, OpenFoxMaidMenuPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> packet.write(buf), OpenFoxMaidMenuPacket::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(targetId);
        buf.writeBoolean(bauble);
    }

    private static OpenFoxMaidMenuPacket read(FriendlyByteBuf buf) {
        return new OpenFoxMaidMenuPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(OpenFoxMaidMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer visitor)) {
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
    }
}
