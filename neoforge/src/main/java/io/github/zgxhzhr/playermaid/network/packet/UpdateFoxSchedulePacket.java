package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.Constants;
import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：切换目标玩家的人是狐日程（主界面日程按钮）。
 *
 * @param targetId 目标玩家实体 id；-1 或发送者自身 id 表示自己
 * @param schedule 要切换到的日程模式
 */
public record UpdateFoxSchedulePacket(int targetId, ScheduleMode schedule) implements CustomPacketPayload {

    /** 他人修改时的最大距离（与界面打开的可达距离一致）。 */
    private static final double INTERACT_DISTANCE_SQR = 8.0 * 8.0;

    public static final CustomPacketPayload.Type<UpdateFoxSchedulePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update_fox_schedule"));

    public static final StreamCodec<FriendlyByteBuf, UpdateFoxSchedulePacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> packet.write(buf), UpdateFoxSchedulePacket::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(targetId);
        buf.writeEnum(schedule);
    }

    private static UpdateFoxSchedulePacket read(FriendlyByteBuf buf) {
        return new UpdateFoxSchedulePacket(buf.readVarInt(), buf.readEnum(ScheduleMode.class));
    }

    public static void handle(UpdateFoxSchedulePacket packet, IPayloadContext context) {
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
            // 修改他人日程需要同维度且在可达距离内，与打开界面的校验一致
            if (visitor != target) {
                if (visitor.level().dimension() != target.level().dimension()) {
                    return;
                }
                if (visitor.distanceToSqr(target) > INTERACT_DISTANCE_SQR) {
                    return;
                }
            }
            data.setSchedule(packet.schedule);
            FoxMaidManager.persistAndSync(target);
        });
    }
}
