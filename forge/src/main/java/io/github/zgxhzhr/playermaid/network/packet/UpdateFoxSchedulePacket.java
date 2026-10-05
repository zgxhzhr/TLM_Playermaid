package io.github.zgxhzhr.playermaid.network.packet;

import io.github.zgxhzhr.playermaid.data.FoxMaidData;
import io.github.zgxhzhr.playermaid.data.FoxMaidManager;
import io.github.zgxhzhr.playermaid.data.ScheduleMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S：切换目标玩家的人是狐日程（主界面日程按钮）。
 *
 * @param targetId 目标玩家实体 id；-1 或发送者自身 id 表示自己
 * @param schedule 要切换到的日程模式
 */
public record UpdateFoxSchedulePacket(int targetId, ScheduleMode schedule) {

    /** 他人修改时的最大距离（与界面打开的可达距离一致）。 */
    private static final double INTERACT_DISTANCE_SQR = 8.0 * 8.0;

    public static void encode(UpdateFoxSchedulePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.targetId);
        buf.writeEnum(packet.schedule);
    }

    public static UpdateFoxSchedulePacket decode(FriendlyByteBuf buf) {
        return new UpdateFoxSchedulePacket(buf.readInt(), buf.readEnum(ScheduleMode.class));
    }

    public static void handle(UpdateFoxSchedulePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
        context.setPacketHandled(true);
    }
}
