package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：切换目标玩家的人是狐工作模式（任务）。
 *
 * @param targetId 目标玩家实体 id；-1 或发送者自身 id 表示自己
 * @param taskUid  任务 ResourceLocation 字符串（如 {@code touhou_little_maid:idle}）
 */
public record UpdateFoxTaskPacket(int targetId, String taskUid) implements CustomPacketPayload {

    /** 他人修改时的最大距离（与界面打开的可达距离一致）。 */
    private static final double INTERACT_DISTANCE_SQR = 8.0 * 8.0;

    public static final CustomPacketPayload.Type<UpdateFoxTaskPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update_fox_task"));

    public static final StreamCodec<FriendlyByteBuf, UpdateFoxTaskPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> packet.write(buf), UpdateFoxTaskPacket::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(targetId);
        buf.writeUtf(taskUid);
    }

    private static UpdateFoxTaskPacket read(FriendlyByteBuf buf) {
        return new UpdateFoxTaskPacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(UpdateFoxTaskPacket packet, IPayloadContext context) {
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
            FoxMaidData data = FoxMaidManager.peek(target).orElse(null);
            if (data == null || !data.isActive()) {
                return;
            }
            // 修改他人任务需要同维度且在可达距离内，与打开界面的校验一致
            if (visitor != target) {
                if (visitor.level().dimension() != target.level().dimension()) {
                    return;
                }
                if (visitor.distanceToSqr(target) > INTERACT_DISTANCE_SQR) {
                    return;
                }
            }
            data.setTaskUid(packet.taskUid);
            FoxMaidManager.persistAndSync(target);
        });
    }
}
